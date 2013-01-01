/*
 * Copyright 2012, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.client;

import com.allanbank.mongodb.Callback;
import com.allanbank.mongodb.MongoDbException;
import com.allanbank.mongodb.ReadPreference;
import com.allanbank.mongodb.bson.io.SizeOfVisitor;
import com.allanbank.mongodb.connection.Connection;
import com.allanbank.mongodb.connection.Message;
import com.allanbank.mongodb.connection.message.Reply;
import com.allanbank.mongodb.error.DocumentToLargeException;

/**
 * AbstractClient provides a base class for {@link Client} implementations.
 * 
 * @api.no This class is <b>NOT</b> part of the drivers API. This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 * @copyright 2012, Allanbank Consulting, Inc., All Rights Reserved
 */
public abstract class AbstractClient implements Client {

    /**
     * Creates a new AbstractClient.
     */
    public AbstractClient() {
        super();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to locate the a connection to send the message and then
     * forward the message to that connection.
     * </p>
     */
    @Override
    public String send(final Message message,
            final Callback<Reply> replyCallback) throws MongoDbException {

        validateMessageSize(message, null);

        return findConnection(message, null).send(message, replyCallback);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to locate the a connection to send the messages and then
     * forward the messages to that connection.
     * </p>
     */
    @Override
    public String send(final Message message1, final Message message2,
            final Callback<Reply> replyCallback) throws MongoDbException {

        validateMessageSize(message1, message2);

        return findConnection(message1, message2).send(message1, message2,
                replyCallback);
    }

    /**
     * Locates a {@link Connection} to send a message on.
     * 
     * @param message1
     *            The first message that will be sent. The connection return
     *            should be compatible with all of the messages
     *            {@link ReadPreference}.
     * @param message2
     *            The second message that will be sent. The connection return
     *            should be compatible with all of the messages
     *            {@link ReadPreference}. May be <code>null</code>.
     * 
     * @return The {@link Connection} to send a message on.
     * @throws MongoDbException
     *             In the case of an error finding a {@link Connection}.
     */
    protected abstract Connection findConnection(Message message1,
            Message message2) throws MongoDbException;

    /**
     * Ensures that the documents in the message do not exceed the maximum size
     * allowed by MongoDB.
     * 
     * @param message1
     *            The message to be sent to the server.
     * @param message2
     *            The second message to be sent to the server.
     * @throws DocumentToLargeException
     *             On a message being too large.
     */
    private void validateMessageSize(final Message message1,
            final Message message2) throws DocumentToLargeException {
        final SizeOfVisitor visitor = new SizeOfVisitor();
        message1.validateSize(visitor, MAX_DOCUMENT_SIZE);

        if (message2 != null) {
            visitor.reset();
            message2.validateSize(visitor, MAX_DOCUMENT_SIZE);
        }
    }

}