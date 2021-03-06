/*
 * #%L
 * MongoDatabase.java - mongodb-async-driver - Allanbank Consulting, Inc.
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
package com.allanbank.mongodb;

import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.DocumentAssignable;
import com.allanbank.mongodb.builder.ListCollections;

/**
 * Interface for interacting with a MongoDB database. Primarily used to
 * {@link #getCollection(String) get} a {@link MongoCollection} .
 *
 * @api.yes This interface is part of the driver's API. Public and protected
 *          members will be deprecated for at least 1 non-bugfix release
 *          (version numbers are &lt;major&gt;.&lt;minor&gt;.&lt;bugfix&gt;)
 *          before being removed or modified.
 * @copyright 2011-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
@ThreadSafe
public interface MongoDatabase {
    /** The name of the administration database. */
    public static final String ADMIN_NAME = MongoClientConfiguration.ADMIN_DB_NAME;

    /** The name of the configuration database for a sharded configuration. */
    public static final String CONFIG_NAME = "config";

    /** The name of the local database. */
    public static final String LOCAL_NAME = "local";

    /** The name of the test database. */
    public static final String TEST_NAME = "test";

    /**
     * Creates the capped collection with the specified name and size on the
     * server.
     *
     * @param name
     *            The name of the collection.
     * @param size
     *            The size of the collection in bytes.
     * @return True if the collection was created, false otherwise (including it
     *         already exists).
     * @throws MongoDbException
     *             On a failure to create the collection.
     */
    public boolean createCappedCollection(String name, long size)
            throws MongoDbException;

    /**
     * Creates the collection with the specified name on the server.
     *
     * @param name
     *            The name of the collection.
     * @param options
     *            The options for the collection being created.
     * @return True if the collection was created, false otherwise (including it
     *         already exists).
     * @throws MongoDbException
     *             On a failure to create the collection.
     */
    public boolean createCollection(String name, DocumentAssignable options)
            throws MongoDbException;

    /**
     * Drops the database.
     *
     * @return True if the database was successfully dropped, false otherwise.
     * @throws MongoDbException
     *             On an error issuing the drop command or in running the
     *             command
     */
    public boolean drop() throws MongoDbException;

    /**
     * Returns true if this database already exists on the server.
     * <p>
     * This method is simply a helper name to check if this database's name
     * appears in the parent {@link MongoClient client's} list of databases.
     * </p>
     *
     * @return True if the parent client's returns this database's name in its
     *         list of database's names.
     * @throws MongoDbException
     *             On an error retrieving the list of database's.
     */
    public boolean exists() throws MongoDbException;

    /**
     * Returns the MongoCollection with the specified name. This method does not
     * validate that the collection already exists in the MongoDB database.
     *
     * @param name
     *            The name of the collection.
     * @return The {@link MongoCollection}.
     */
    public MongoCollection getCollection(String name);

    /**
     * Returns the durability for write operations sent to the server from this
     * {@link MongoDatabase} instance.
     * <p>
     * Defaults to the {@link Durability} from the {@link MongoClient}'s
     * configuration.
     * </p>
     *
     * @return The durability for write operations on the server.
     *
     * @see MongoClientConfiguration#getDefaultDurability()
     */
    public Durability getDurability();

    /**
     * Returns the name of the database.
     *
     * @return The name of the database.
     */
    public String getName();

    /**
     * Retrieves the profiling level for the database.
     *
     * @return The current profiling level.
     * @throws MongoDbException
     *             On a failure to create the collection.
     * @see <a
     *      href="http://docs.mongodb.org/manual/reference/command/profile/">profile
     *      Command Reference</a>
     */
    public ProfilingStatus getProfilingStatus() throws MongoDbException;

    /**
     * Returns the read preference for queries from this {@link MongoDatabase}
     * instance.
     * <p>
     * Defaults to {@link ReadPreference} from the {@link MongoClient}'s
     * configuration.
     * </p>
     *
     * @return The default read preference for a query.
     *
     * @see MongoClientConfiguration#getDefaultReadPreference()
     */
    public ReadPreference getReadPreference();

    /**
     * Returns the list of the collection names contained within the database.
     *
     * @return The list of the collection names contained within the database.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public List<String> listCollectionNames() throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @return The iterator over of the collections contained within the
     *         database.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public MongoIterator<Document> listCollections(
            ListCollections listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @return The iterator over of the collections contained within the
     *         database.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public MongoIterator<Document> listCollections(
            ListCollections.Builder listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param results
     *            The callback to notify of the results.
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public void listCollectionsAsync(Callback<MongoIterator<Document>> results,
            ListCollections listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param results
     *            The callback to notify of the results.
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public void listCollectionsAsync(Callback<MongoIterator<Document>> results,
            ListCollections.Builder listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param results
     *            The callback to notify of the results.
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public void listCollectionsAsync(
            LambdaCallback<MongoIterator<Document>> results,
            ListCollections listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param results
     *            The callback to notify of the results.
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public void listCollectionsAsync(
            LambdaCallback<MongoIterator<Document>> results,
            ListCollections.Builder listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @return The future for the iterator over of the collections contained
     *         within the database.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public ListenableFuture<MongoIterator<Document>> listCollectionsAsync(
            ListCollections listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     *
     * @param listCollections
     *            The specification for the collection documents to be returned.
     * @return The future for the iterator over of the collections contained
     *         within the database.
     * @throws MongoDbException
     *             On an error listing the collections.
     */
    public ListenableFuture<MongoIterator<Document>> listCollectionsAsync(
            ListCollections.Builder listCollections) throws MongoDbException;

    /**
     * Runs an administrative command against the 'admin' database.
     *
     * @param command
     *            The name of the command to run.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runAdminCommand(String command) throws MongoDbException;

    /**
     * Runs an administrative command against the 'admin' database.
     *
     * @param command
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runAdminCommand(String command, DocumentAssignable options)
            throws MongoDbException;

    /**
     * Runs an administrative command against the 'admin' database.
     *
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runAdminCommand(String commandName, String commandValue,
            DocumentAssignable options) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param command
     *            The command document to run.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runCommand(DocumentAssignable command)
            throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param command
     *            The name of the command to run.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runCommand(String command) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param command
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runCommand(String command, DocumentAssignable options)
            throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runCommand(String commandName, int commandValue,
            DocumentAssignable options) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public Document runCommand(String commandName, String commandValue,
            DocumentAssignable options) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link Callback} that will be notified of the command results.
     * @param command
     *            The command document to run.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(Callback<Document> reply,
            DocumentAssignable command) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link Callback} that will be notified of the command results.
     * @param command
     *            The command document to run.
     * @param requiredServerVersion
     *            The minimum required server version to support the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(Callback<Document> reply,
            DocumentAssignable command, Version requiredServerVersion)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link Callback} that will be notified of the command results.
     * @param command
     *            The name of the command to run.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(Callback<Document> reply, String command)
            throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link Callback} that will be notified of the command results.
     * @param command
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(Callback<Document> reply, String command,
            DocumentAssignable options) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link Callback} that will be notified of the command results.
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(Callback<Document> reply, String commandName,
            int commandValue, DocumentAssignable options)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link Callback} that will be notified of the command results.
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(Callback<Document> reply, String commandName,
            String commandValue, DocumentAssignable options)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param command
     *            The command document to run.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public ListenableFuture<Document> runCommandAsync(DocumentAssignable command)
            throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link LambdaCallback} that will be notified of the command
     *            results.
     * @param command
     *            The command document to run.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(LambdaCallback<Document> reply,
            DocumentAssignable command) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link LambdaCallback} that will be notified of the command
     *            results.
     * @param command
     *            The command document to run.
     * @param requiredServerVersion
     *            The minimum required server version to support the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(LambdaCallback<Document> reply,
            DocumentAssignable command, Version requiredServerVersion)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link LambdaCallback} that will be notified of the command
     *            results.
     * @param command
     *            The name of the command to run.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(LambdaCallback<Document> reply, String command)
            throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link LambdaCallback} that will be notified of the command
     *            results.
     * @param command
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(LambdaCallback<Document> reply, String command,
            DocumentAssignable options) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link LambdaCallback} that will be notified of the command
     *            results.
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(LambdaCallback<Document> reply,
            String commandName, int commandValue, DocumentAssignable options)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param reply
     *            {@link LambdaCallback} that will be notified of the command
     *            results.
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public void runCommandAsync(LambdaCallback<Document> reply,
            String commandName, String commandValue, DocumentAssignable options)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param command
     *            The name of the command to run.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public ListenableFuture<Document> runCommandAsync(String command)
            throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param command
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public ListenableFuture<Document> runCommandAsync(String command,
            DocumentAssignable options) throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public ListenableFuture<Document> runCommandAsync(String commandName,
            int commandValue, DocumentAssignable options)
                    throws MongoDbException;

    /**
     * Runs a command against the database.
     *
     * @param commandName
     *            The name of the command to run.
     * @param commandValue
     *            The name of the command to run.
     * @param options
     *            Optional (may be null) options for the command.
     * @return The result of the command.
     * @throws MongoDbException
     *             On an error issuing the command or in running the command
     */
    public ListenableFuture<Document> runCommandAsync(String commandName,
            String commandValue, DocumentAssignable options)
                    throws MongoDbException;

    /**
     * Sets the durability for write operations from this {@link MongoDatabase}
     * instance.
     * <p>
     * Defaults to the {@link Durability} from the {@link MongoClient}'s
     * configuration if set to <code>null</code>.
     * </p>
     *
     * @param durability
     *            The durability for write operations on the server.
     *
     * @see MongoClientConfiguration#getDefaultDurability()
     */
    public void setDurability(final Durability durability);

    /**
     * Sets the profiling level for the database.
     *
     * @param profileLevel
     *            The desired profiling level
     * @return True if the profiling level was changed. Note if the level
     *         provided matches the profiling level already set then this method
     *         returns <code>false</code>.
     * @throws MongoDbException
     *             On a failure to create the collection.
     * @see <a
     *      href="http://docs.mongodb.org/manual/reference/command/profile/">profile
     *      Command Reference</a>
     */
    public boolean setProfilingStatus(ProfilingStatus profileLevel)
            throws MongoDbException;

    /**
     * Sets the value of the read preference for a queries from this
     * {@link MongoDatabase} instance.
     * <p>
     * Defaults to the {@link ReadPreference} from the {@link MongoClient}'s
     * configuration if set to <code>null</code>.
     * </p>
     *
     * @param readPreference
     *            The read preference for a query.
     *
     * @see MongoClientConfiguration#getDefaultReadPreference()
     */
    public void setReadPreference(final ReadPreference readPreference);

    /**
     * Returns the statistics for the database.
     *
     * @return The results document with the database statistics.
     * @throws MongoDbException
     *             On an error collecting the database statistics.
     * @see <a
     *      href="http://docs.mongodb.org/manual/reference/command/dbStats/">dbStats
     *      Command Reference</a>
     */
    public Document stats() throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     * <p>
     * The sequence of callbacks will be terminated by either calling the
     * {@link StreamCallback#done() results.done()} method or by calling the
     * {@link StreamCallback#exception(Throwable) results.exception(...)} method
     * (in the event of an error).
     * </p>
     * <p>
     * Applications can terminate the stream by throwing a
     * {@link RuntimeException} from the {@link StreamCallback#callback} method
     * (which will then call the {@link StreamCallback#exception} method or by
     * closing the {@link MongoCursorControl} returned from this method.
     * </p>
     * <p>
     * Only a single thread will invoke the callback at a time but that thread
     * may change over time.
     * </p>
     * <p>
     * If the callback processing requires any significant time (including I/O)
     * it is recommended that an
     * {@link MongoClientConfiguration#setExecutor(java.util.concurrent.Executor)
     * Executor} be configured within the {@link MongoClientConfiguration} to
     * off-load the processing from the receive thread.
     * </p>
     *
     * @param results
     *            Callback that will be notified of the results of the query.
     * @param listCollections
     *            The query details.
     * @return A {@link MongoCursorControl} to control the cursor streaming
     *         documents to the caller. This includes the ability to stop the
     *         cursor and persist its state.
     * @throws MongoDbException
     *             On an error finding the documents.
     */
    public MongoCursorControl stream(LambdaCallback<Document> results,
            ListCollections.Builder listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     * <p>
     * The sequence of callbacks will be terminated by either calling the
     * {@link StreamCallback#done() results.done()} method or by calling the
     * {@link StreamCallback#exception(Throwable) results.exception(...)} method
     * (in the event of an error).
     * </p>
     * <p>
     * Applications can terminate the stream by throwing a
     * {@link RuntimeException} from the {@link StreamCallback#callback} method
     * (which will then call the {@link StreamCallback#exception} method or by
     * closing the {@link MongoCursorControl} returned from this method.
     * </p>
     * <p>
     * Only a single thread will invoke the callback at a time but that thread
     * may change over time.
     * </p>
     * <p>
     * If the callback processing requires any significant time (including I/O)
     * it is recommended that an
     * {@link MongoClientConfiguration#setExecutor(java.util.concurrent.Executor)
     * Executor} be configured within the {@link MongoClientConfiguration} to
     * off-load the processing from the receive thread.
     * </p>
     *
     * @param results
     *            Callback that will be notified of the results of the query.
     * @param listCollections
     *            The query details.
     * @return A {@link MongoCursorControl} to control the cursor streaming
     *         documents to the caller. This includes the ability to stop the
     *         cursor and persist its state.
     * @throws MongoDbException
     *             On an error finding the documents.
     */
    public MongoCursorControl stream(LambdaCallback<Document> results,
            ListCollections listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     * <p>
     * The sequence of callbacks will be terminated by either calling the
     * {@link StreamCallback#done() results.done()} method or by calling the
     * {@link StreamCallback#exception(Throwable) results.exception(...)} method
     * (in the event of an error).
     * </p>
     * <p>
     * Applications can terminate the stream by throwing a
     * {@link RuntimeException} from the {@link StreamCallback#callback} method
     * (which will then call the {@link StreamCallback#exception} method or by
     * closing the {@link MongoCursorControl} returned from this method.
     * </p>
     * <p>
     * Only a single thread will invoke the callback at a time but that thread
     * may change over time.
     * </p>
     * <p>
     * If the callback processing requires any significant time (including I/O)
     * it is recommended that an
     * {@link MongoClientConfiguration#setExecutor(java.util.concurrent.Executor)
     * Executor} be configured within the {@link MongoClientConfiguration} to
     * off-load the processing from the receive thread.
     * </p>
     *
     * @param results
     *            Callback that will be notified of the results of the query.
     * @param listCollections
     *            The query details.
     * @return A {@link MongoCursorControl} to control the cursor streaming
     *         documents to the caller. This includes the ability to stop the
     *         cursor and persist its state.
     * @throws MongoDbException
     *             On an error finding the documents.
     */
    public MongoCursorControl stream(StreamCallback<Document> results,
            ListCollections listCollections) throws MongoDbException;

    /**
     * Returns the list of the collections contained within the database.
     * <p>
     * The sequence of callbacks will be terminated by either calling the
     * {@link StreamCallback#done() results.done()} method or by calling the
     * {@link StreamCallback#exception(Throwable) results.exception(...)} method
     * (in the event of an error).
     * </p>
     * <p>
     * Applications can terminate the stream by throwing a
     * {@link RuntimeException} from the {@link StreamCallback#callback} method
     * (which will then call the {@link StreamCallback#exception} method or by
     * closing the {@link MongoCursorControl} returned from this method.
     * </p>
     * <p>
     * Only a single thread will invoke the callback at a time but that thread
     * may change over time.
     * </p>
     * <p>
     * If the callback processing requires any significant time (including I/O)
     * it is recommended that an
     * {@link MongoClientConfiguration#setExecutor(java.util.concurrent.Executor)
     * Executor} be configured within the {@link MongoClientConfiguration} to
     * off-load the processing from the receive thread.
     * </p>
     *
     * @param results
     *            Callback that will be notified of the results of the query.
     * @param listCollections
     *            The query details.
     * @return A {@link MongoCursorControl} to control the cursor streaming
     *         documents to the caller. This includes the ability to stop the
     *         cursor and persist its state.
     * @throws MongoDbException
     *             On an error finding the documents.
     */
    public MongoCursorControl stream(StreamCallback<Document> results,
            ListCollections.Builder listCollections) throws MongoDbException;
}
