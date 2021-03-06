/*
 * #%L
 * SocketConnectionFactory.java - mongodb-async-driver - Allanbank Consulting, Inc.
 * %%
 * Copyright (C) 2011 - 2014 Allanbank Consulting, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.allanbank.mongodb.client.connection.socket;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.allanbank.mongodb.MongoClientConfiguration;
import com.allanbank.mongodb.Version;
import com.allanbank.mongodb.bson.io.StringDecoderCache;
import com.allanbank.mongodb.bson.io.StringEncoderCache;
import com.allanbank.mongodb.client.ClusterStats;
import com.allanbank.mongodb.client.ClusterType;
import com.allanbank.mongodb.client.connection.Connection;
import com.allanbank.mongodb.client.connection.ConnectionFactory;
import com.allanbank.mongodb.client.connection.ReconnectStrategy;
import com.allanbank.mongodb.client.connection.proxy.ProxiedConnectionFactory;
import com.allanbank.mongodb.client.message.IsMaster;
import com.allanbank.mongodb.client.metrics.MongoClientMetrics;
import com.allanbank.mongodb.client.metrics.noop.NoOpMongoClientMetrics;
import com.allanbank.mongodb.client.state.Cluster;
import com.allanbank.mongodb.client.state.LatencyServerSelector;
import com.allanbank.mongodb.client.state.Server;
import com.allanbank.mongodb.client.state.ServerSelector;
import com.allanbank.mongodb.client.state.ServerUpdateCallback;
import com.allanbank.mongodb.client.state.SimpleReconnectStrategy;
import com.allanbank.mongodb.client.transport.Transport;
import com.allanbank.mongodb.client.transport.TransportOutputBuffer;
import com.allanbank.mongodb.util.log.Log;
import com.allanbank.mongodb.util.log.LogFactory;

/**
 * {@link ConnectionFactory} to create direct socket connections to the servers.
 *
 * @api.no This class is <b>NOT</b> part of the drivers API. This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 * @copyright 2011-2014, Allanbank Consulting, Inc., All Rights Reserved
 */
public class SocketConnectionFactory
        implements ProxiedConnectionFactory {

    /** The logger for the factory. */
    private static final Log LOG = LogFactory
            .getLog(SocketConnectionFactory.class);

    /** The MongoDB client configuration. */
    protected final MongoClientConfiguration myConfig;

    /** Cache used for decoding strings. */
    protected final StringDecoderCache myDecoderCache;

    /** Cache used for encoding strings. */
    protected final StringEncoderCache myEncoderCache;

    /** The state of the cluster. */
    private final Cluster myCluster;

    /** The MongoDB client configuration. */
    private final ConfigurationListener myConfigListener;

    /** The metrics for the client. */
    private MongoClientMetrics myMetrics;

    /** The server selector. */
    private final ServerSelector myServerSelector;

    /**
     * Creates a new {@link SocketConnectionFactory}.
     *
     * @param config
     *            The MongoDB client configuration.
     */
    public SocketConnectionFactory(final MongoClientConfiguration config) {
        super();
        myConfig = config;
        setMetrics(null); // Enforces a non-null value.

        myCluster = new Cluster(config, ClusterType.STAND_ALONE);
        myServerSelector = new LatencyServerSelector(myCluster, true);

        myConfigListener = new ConfigurationListener();
        myConfig.addPropertyChangeListener(myConfigListener);

        myDecoderCache = new StringDecoderCache();
        myDecoderCache.setMaxCacheEntries(config.getMaxCachedStringEntries());
        myDecoderCache.setMaxCacheLength(config.getMaxCachedStringLength());

        myEncoderCache = new StringEncoderCache();
        myEncoderCache.setMaxCacheEntries(config.getMaxCachedStringEntries());
        myEncoderCache.setMaxCacheLength(config.getMaxCachedStringLength());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to do nothing.
     * </p>
     */
    @Override
    public void close() {
        myConfig.removePropertyChangeListener(myConfigListener);

        // Release the cached entries too.
        myDecoderCache.setMaxCacheEntries(0);
        myDecoderCache.setMaxCacheLength(0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a new {@link TransportConnection}.
     * </p>
     *
     * @see ConnectionFactory#connect()
     */
    @Override
    public Connection connect() throws IOException {
        final List<InetSocketAddress> servers = new ArrayList<InetSocketAddress>(
                myConfig.getServerAddresses());

        // Shuffle the servers and try to connect to each until one works.
        IOException last = null;
        Collections.shuffle(servers);
        for (final InetSocketAddress address : servers) {
            try {
                final Server server = myCluster.add(address);
                final Connection conn = connect(server, myConfig);

                // Get the state of the server updated.
                final ServerUpdateCallback cb = new ServerUpdateCallback(server);
                conn.send(new IsMaster(), cb);

                if (Version.UNKNOWN.equals(server.getVersion())) {
                    // If we don't know the version then wait for that response.
                    try {
                        cb.get();
                    }
                    catch (final ExecutionException e) {
                        // Probably not in a good state...
                        LOG.debug(e, "Could not execute an 'ismaster' command.");
                    }
                    catch (final InterruptedException e) {
                        // Probably not in a good state...
                        LOG.debug(e, "Could not execute an 'ismaster' command.");
                    }
                }

                return conn;
            }
            catch (final IOException error) {
                last = error;
            }
        }

        if (last != null) {
            throw last;
        }
        throw new IOException("Could not connect to any server: " + servers);
    }

    /**
     * Creates a connection to the address provided.
     *
     * @param server
     *            The MongoDB server to connect to.
     * @param config
     *            The configuration for the Connection to the MongoDB server.
     * @return The Connection to MongoDB.
     * @throws IOException
     *             On a failure connecting to the server.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Connection connect(final Server server,
            final MongoClientConfiguration config) throws IOException {

        final TransportConnection connection = new TransportConnection(server,
                config, myMetrics.newConnection(server.getCanonicalName()));

        final Transport<TransportOutputBuffer> transport = (Transport<TransportOutputBuffer>) config
                .getTransportFactory().createTransport(server, config,
                        myEncoderCache, myDecoderCache, connection);

        // Associate the connection with the transport.
        connection.setTransport(transport);

        // Start the connection.
        connection.start();

        return connection;
    }

    /**
     * Returns the cluster state.
     *
     * @return The cluster state.
     */
    public Cluster getCluster() {
        return myCluster;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return the {@link Cluster}.
     * </p>
     */
    @Override
    public ClusterStats getClusterStats() {
        return myCluster;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return {@link ClusterType#STAND_ALONE} cluster type.
     * </p>
     */
    @Override
    public ClusterType getClusterType() {
        return ClusterType.STAND_ALONE;
    }

    /**
     * Returns the factory' string decoder cache.
     *
     * @return The factory's string decoder cache.
     */
    public StringDecoderCache getDecoderCache() {
        return myDecoderCache;
    }

    /**
     * Returns the factory' string encoder cache.
     *
     * @return The factory's string encoder cache.
     */
    public StringEncoderCache getEncoderCache() {
        return myEncoderCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MongoClientMetrics getMetrics() {
        return myMetrics;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return a {@link SimpleReconnectStrategy}.
     * </p>
     */
    @Override
    public ReconnectStrategy getReconnectStrategy() {
        final SimpleReconnectStrategy strategy = new SimpleReconnectStrategy();
        strategy.setConfig(myConfig);
        strategy.setConnectionFactory(this);
        strategy.setSelector(myServerSelector);
        strategy.setState(myCluster);

        return strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMetrics(final MongoClientMetrics metrics) {
        if (metrics == null) {
            myMetrics = new NoOpMongoClientMetrics();
        }
        else {
            myMetrics = metrics;
        }
    }

    /**
     * Notification of a change to the configuration of the client.
     *
     * @param evt
     *            The details of the configuration change.
     */
    protected void configurationChanged(final PropertyChangeEvent evt) {
        final String name = evt.getPropertyName();
        final Object value = evt.getNewValue();
        if ("maxCachedStringEntries".equals(name) && (value instanceof Number)) {
            myDecoderCache.setMaxCacheEntries(((Number) value).intValue());
            myEncoderCache.setMaxCacheEntries(((Number) value).intValue());
        }
        else if ("maxCachedStringLength".equals(name)
                && (value instanceof Number)) {
            myDecoderCache.setMaxCacheLength(((Number) value).intValue());
            myEncoderCache.setMaxCacheLength(((Number) value).intValue());
        }
    }

    /**
     * ConfigurationListener provides a listener for changes in the client's
     * configuration.
     *
     * @api.no This class is <b>NOT</b> part of the drivers API. This class may
     *         be mutated in incompatible ways between any two releases of the
     *         driver.
     * @copyright 2014, Allanbank Consulting, Inc., All Rights Reserved
     */
    protected final class ConfigurationListener
            implements PropertyChangeListener {
        /**
         * {@inheritDoc}
         * <p>
         * Overridden forward to
         * {@link SocketConnectionFactory#configurationChanged}.
         * </p>
         *
         * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
         */
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            configurationChanged(evt);
        }
    }
}
