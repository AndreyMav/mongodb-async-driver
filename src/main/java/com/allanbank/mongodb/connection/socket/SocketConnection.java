/*
 * Copyright 2011-2012, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */
package com.allanbank.mongodb.connection.socket;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.allanbank.mongodb.Callback;
import com.allanbank.mongodb.MongoClientConfiguration;
import com.allanbank.mongodb.MongoDbException;
import com.allanbank.mongodb.bson.io.BsonInputStream;
import com.allanbank.mongodb.bson.io.BsonOutputStream;
import com.allanbank.mongodb.connection.Connection;
import com.allanbank.mongodb.connection.Message;
import com.allanbank.mongodb.connection.Operation;
import com.allanbank.mongodb.connection.message.Delete;
import com.allanbank.mongodb.connection.message.GetMore;
import com.allanbank.mongodb.connection.message.Header;
import com.allanbank.mongodb.connection.message.Insert;
import com.allanbank.mongodb.connection.message.IsMaster;
import com.allanbank.mongodb.connection.message.KillCursors;
import com.allanbank.mongodb.connection.message.PendingMessage;
import com.allanbank.mongodb.connection.message.PendingMessageQueue;
import com.allanbank.mongodb.connection.message.Query;
import com.allanbank.mongodb.connection.message.Reply;
import com.allanbank.mongodb.connection.message.ReplyHandler;
import com.allanbank.mongodb.connection.message.Update;
import com.allanbank.mongodb.connection.state.ServerState;
import com.allanbank.mongodb.error.ConnectionLostException;
import com.allanbank.mongodb.util.IOUtils;

/**
 * Provides a blocking Socket based connection to a MongoDB server.
 * 
 * @api.no This class is <b>NOT</b> part of the drivers API. This class may be
 *         mutated in incompatible ways between any two releases of the driver.
 * @copyright 2011-2012, Allanbank Consulting, Inc., All Rights Reserved
 */
public class SocketConnection implements Connection {

    /** The length of the message header in bytes. */
    public static final int HEADER_LENGTH = 16;

    /** Exception that there was no reply for a message from MongoDB. */
    public static final MongoDbException NO_REPLY = new MongoDbException(
            "No reply received.");

    /** The logger for the {@link SocketConnection}. */
    protected static final Logger LOG = Logger.getLogger(SocketConnection.class
            .getCanonicalName());

    /** The executor for the responses. */
    protected final Executor myExecutor;

    /** Holds if the connection is open. */
    protected final AtomicBoolean myOpen;

    /** The queue of messages sent but waiting for a reply. */
    protected final PendingMessageQueue myPendingQueue;

    /** Set to true when the connection should be gracefully closed. */
    protected final AtomicBoolean myShutdown;

    /** The queue of messages to be sent. */
    protected final PendingMessageQueue myToSendQueue;

    /** The writer for BSON documents. Shares this objects {@link #myInput}. */
    private final BsonInputStream myBsonIn;

    /** The writer for BSON documents. Shares this objects {@link #myOutput}. */
    private final BsonOutputStream myBsonOut;

    /** Support for emitting property change events. */
    private final PropertyChangeSupport myEventSupport;

    /** The buffered input stream. */
    private final BufferedInputStream myInput;

    /** The buffered output stream. */
    private final BufferedOutputStream myOutput;

    /** The thread receiving replies. */
    private final Thread myReceiver;

    /** The thread sending messages. */
    private final Thread mySender;

    /** The open socket. */
    private final ServerState myServer;

    /** The open socket. */
    private final Socket mySocket;

    /**
     * Creates a new SocketConnection to a MongoDB server.
     * 
     * @param server
     *            The MongoDB server to connect to.
     * @param config
     *            The configuration for the Connection to the MongoDB server.
     * @throws SocketException
     *             On a failure connecting to the MongoDB server.
     * @throws IOException
     *             On a failure to read or write data to the MongoDB server.
     */
    public SocketConnection(final ServerState server,
            final MongoClientConfiguration config) throws SocketException,
            IOException {

        myExecutor = config.getExecutor();
        myServer = server;
        myEventSupport = new PropertyChangeSupport(this);
        myOpen = new AtomicBoolean(false);
        myShutdown = new AtomicBoolean(false);

        mySocket = config.getSocketFactory().createSocket();
        mySocket.connect(myServer.getServer(), config.getConnectTimeout());
        updateSocketWithOptions(config);

        myOpen.set(true);

        myInput = new BufferedInputStream(mySocket.getInputStream());
        myBsonIn = new BsonInputStream(myInput);

        myOutput = new BufferedOutputStream(mySocket.getOutputStream());
        myBsonOut = new BsonOutputStream(myOutput);

        myToSendQueue = new PendingMessageQueue(
                config.getMaxPendingOperationsPerConnection(),
                config.getLockType());
        myPendingQueue = new PendingMessageQueue(
                config.getMaxPendingOperationsPerConnection(),
                config.getLockType());

        myReceiver = config.getThreadFactory().newThread(new ReceiveRunnable());
        myReceiver.setName("MongoDB " + mySocket.getLocalPort() + "<--"
                + myServer.getServer().toString());

        mySender = config.getThreadFactory().newThread(new SendRunnable());
        mySender.setName("MongoDB " + mySocket.getLocalPort() + "-->"
                + myServer.getServer().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPending(final List<PendingMessage> pending)
            throws InterruptedException {
        while (!pending.isEmpty()) {
            myToSendQueue.put(pending.get(0));
            pending.remove(0);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to add the listener to this connection.
     * </p>
     */
    @Override
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        myEventSupport.addPropertyChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        final boolean wasOpen = myOpen.get();
        myOpen.set(false);

        mySender.interrupt();
        myReceiver.interrupt();

        try {
            if (Thread.currentThread() != mySender) {
                mySender.join();
            }
        }
        catch (final InterruptedException ie) {
            // Ignore.
        }
        finally {
            // Now that output is shutdown. Close up the socket. This
            // Triggers the receiver to close if the interrupt didn't work.
            myOutput.close();
            myInput.close();
            mySocket.close();
        }

        try {
            if (Thread.currentThread() != myReceiver) {
                myReceiver.join();
            }
        }
        catch (final InterruptedException ie) {
            // Ignore.
        }

        myEventSupport.firePropertyChange(OPEN_PROP_NAME, wasOpen, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void drainPending(final List<PendingMessage> pending) {
        myToSendQueue.drainTo(pending);
    }

    /**
     * /** {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        myOutput.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPendingCount() {
        return myPendingQueue.size() + myToSendQueue.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * True if the send and pending queues are empty.
     * </p>
     */
    @Override
    public boolean isIdle() {
        return myPendingQueue.isEmpty() && myToSendQueue.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * True if the send and receive threads are running.
     * </p>
     */
    @Override
    public boolean isOpen() {
        return myOpen.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Notifies the appropriate messages of the error.
     * </p>
     */
    @Override
    public void raiseErrors(final MongoDbException exception,
            final boolean notifyToBeSent) {
        final PendingMessage message = new PendingMessage();
        if (notifyToBeSent) {
            while (myToSendQueue.poll(message)) {
                raiseError(exception, message.getReplyCallback());
            }
        }

        // Now the pending.
        while (myPendingQueue.poll(message)) {
            raiseError(exception, message.getReplyCallback());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to remove the listener from this connection.
     * </p>
     */
    @Override
    public void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        myEventSupport.removePropertyChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String send(final Message message,
            final Callback<Reply> replyCallback) throws MongoDbException {
        try {
            myToSendQueue.put(message, replyCallback);
        }
        catch (final InterruptedException e) {
            throw new MongoDbException(e);
        }

        return myServer.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String send(final Message message1, final Message message2,
            final Callback<Reply> replyCallback) throws MongoDbException {
        try {
            myToSendQueue.put(message1, null, message2, replyCallback);
        }
        catch (final InterruptedException e) {
            throw new MongoDbException(e);
        }

        return myServer.getName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to mark the socket as shutting down and tickles the sender to
     * make sure that happens as soon as possible.
     * </p>
     */
    @Override
    public void shutdown() {
        // Mark
        myShutdown.set(true);

        // Force a message with a callback to wake the sender and receiver up.
        send(new IsMaster(), new NoopCallback());
    }

    /**
     * Starts the connections read and write threads.
     */
    public void start() {
        myReceiver.start();
        mySender.start();
    }

    /**
     * Stops the socket connection by calling {@link #shutdown()}.
     */
    public void stop() {
        shutdown();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to return the socket information.
     * </p>
     */
    @Override
    public String toString() {
        return "MongoDB(" + mySocket.getLocalPort() + "-->"
                + mySocket.getRemoteSocketAddress() + ")";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Waits for the connections pending queues to empty.
     * </p>
     */
    @Override
    public void waitForClosed(final int timeout, final TimeUnit timeoutUnits) {
        long now = System.currentTimeMillis();
        final long deadline = now + timeoutUnits.toMillis(timeout);

        while (isOpen() && (now < deadline)) {
            try {
                // A slow spin loop.
                TimeUnit.MILLISECONDS.sleep(10);
            }
            catch (final InterruptedException e) {
                // Ignore.
                e.hashCode();
            }
            now = System.currentTimeMillis();
        }
    }

    /**
     * Receives a single message from the connection.
     * 
     * @return The {@link Message} received.
     * @throws MongoDbException
     *             On an error receiving the message.
     */
    protected Message doReceive() throws MongoDbException {
        try {
            int length;
            try {
                length = readIntSuppressTimeoutOnNonFirstByte();
            }
            catch (final SocketTimeoutException ok) {
                // This is OK. We check if we are still running and come right
                // back.
                return null;
            }

            final int requestId = myBsonIn.readInt();
            final int responseId = myBsonIn.readInt();
            final int opCode = myBsonIn.readInt();

            final Operation op = Operation.fromCode(opCode);
            if (op == null) {
                // Huh? Dazed and confused
                throw new MongoDbException("Unexpected operation read '"
                        + opCode + "'.");
            }

            final Header header = new Header(length, requestId, responseId, op);
            Message message = null;
            switch (op) {
            case REPLY:
                message = new Reply(header, myBsonIn);
                break;
            case QUERY:
                message = new Query(header, myBsonIn);
                break;
            case UPDATE:
                message = new Update(myBsonIn);
                break;
            case INSERT:
                message = new Insert(header, myBsonIn);
                break;
            case GET_MORE:
                message = new GetMore(myBsonIn);
                break;
            case DELETE:
                message = new Delete(myBsonIn);
                break;
            case KILL_CURSORS:
                message = new KillCursors(myBsonIn);
                break;
            }

            return message;
        }
        catch (final IOException ioe) {
            final MongoDbException error = new ConnectionLostException(ioe);

            // Have to assume all of the requests have failed that are pending.
            final PendingMessage message = new PendingMessage();
            while (myPendingQueue.poll(message)) {
                raiseError(error, message.getReplyCallback());
            }

            closeQuietly();

            throw error;
        }
    }

    /**
     * Sends a single message to the connection.
     * 
     * @param messageId
     *            The id to use for the message.
     * @param message
     *            The message to send.
     * @throws IOException
     *             On a failure sending the message.
     */
    protected void doSend(final int messageId, final Message message)
            throws IOException {
        message.write(messageId, myBsonOut);
    }

    /**
     * Updates to raise an error on the callback, if any.
     * 
     * @param exception
     *            The thrown exception.
     * @param replyCallback
     *            The callback for the reply to the message.
     */
    protected void raiseError(final Throwable exception,
            final Callback<Reply> replyCallback) {
        ReplyHandler.raiseError(exception, replyCallback, myExecutor);
    }

    /**
     * Reads a little-endian 4 byte signed integer from the stream.
     * 
     * @return The integer value.
     * @throws EOFException
     *             On insufficient data for the integer.
     * @throws IOException
     *             On a failure reading the integer.
     */
    protected int readIntSuppressTimeoutOnNonFirstByte() throws EOFException,
            IOException {
        int read = 0;
        int eofCheck = 0;
        int result = 0;

        read = myBsonIn.read();
        eofCheck |= read;
        result += (read << 0);

        for (int i = Byte.SIZE; i < Integer.SIZE; i += Byte.SIZE) {
            try {
                read = myBsonIn.read();
            }
            catch (final SocketTimeoutException ste) {
                // Bad - Only the first byte should timeout.
                throw new IOException(ste);
            }
            eofCheck |= read;
            result += (read << i);
        }

        if (eofCheck < 0) {
            throw new EOFException();
        }
        return result;
    }

    /**
     * Updates to set the reply for the callback, if any.
     * 
     * @param reply
     *            The reply.
     * @param replyCallback
     *            The callback for the reply to the message.
     */
    protected void reply(final Reply reply, final Callback<Reply> replyCallback) {
        ReplyHandler.reply(reply, replyCallback, myExecutor);
    }

    /**
     * Updates the socket with the configuration's socket options.
     * 
     * @param config
     *            The configuration to apply.
     * @throws SocketException
     *             On a failure setting the socket options.
     */
    protected void updateSocketWithOptions(final MongoClientConfiguration config)
            throws SocketException {
        mySocket.setKeepAlive(config.isUsingSoKeepalive());
        mySocket.setSoTimeout(config.getReadTimeout());
        try {
            mySocket.setTcpNoDelay(true);
        }
        catch (final SocketException seIgnored) {
            // The junixsocket implementation does not support TCP_NO_DELAY,
            // which makes sense but it throws an exception instead of silently
            // ignoring - ignore it here.
            if (!"AFUNIXSocketException".equals(seIgnored.getClass()
                    .getSimpleName())) {
                throw seIgnored;
            }
        }
        mySocket.setPerformancePreferences(1, 5, 6);
    }

    /**
     * Waits for the requested number of messages to become pending.
     * 
     * @param count
     *            The number of pending messages expected.
     * @param millis
     *            The number of milliseconds to wait.
     */
    protected void waitForPending(final int count, final long millis) {
        long now = System.currentTimeMillis();
        final long deadline = now + millis;

        while ((count < myPendingQueue.size()) && (now < deadline)) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            catch (final InterruptedException e) {
                // Ignore.
                e.hashCode();
            }
            now = System.currentTimeMillis();
        }
        // Pause for the write to happen.
        try {
            TimeUnit.MILLISECONDS.sleep(5);
        }
        catch (final InterruptedException e) {
            // Ignore.
            e.hashCode();
        }
    }

    /**
     * Closes the connection to the server without allowing an exception to be
     * thrown.
     */
    private void closeQuietly() {
        try {
            close();
        }
        catch (final IOException e) {
            LOG.log(Level.WARNING,
                    "I/O exception trying to shutdown the connection.", e);
        }
    }

    /**
     * NoopCallback provides a callback that does not look at the reply.
     * 
     * @copyright 2012, Allanbank Consulting, Inc., All Rights Reserved
     */
    protected static final class NoopCallback implements Callback<Reply> {
        /**
         * {@inheritDoc}
         * <p>
         * Overridden to do nothing.
         * </p>
         */
        @Override
        public void callback(final Reply result) {
            // Noop.
        }

        /**
         * {@inheritDoc}
         * <p>
         * Overridden to do nothing.
         * </p>
         */
        @Override
        public void exception(final Throwable thrown) {
            // Noop.
        }
    }

    /**
     * Runnable to receive messages.
     * 
     * @copyright 2011, Allanbank Consulting, Inc., All Rights Reserved
     */
    protected class ReceiveRunnable implements Runnable {

        /** The {@link PendingMessage} used for the local cached copy. */
        private final PendingMessage myPendingMessage = new PendingMessage();

        /**
         * Processing thread for receiving responses from the server.
         */
        @Override
        public void run() {
            try {
                while (myOpen.get()) {
                    try {
                        receiveOne();

                        // Check if we are shutdown. Note the shutdown() method
                        // makes sure the last message gets a reply.
                        if (myShutdown.get() && myToSendQueue.isEmpty()
                                && myPendingQueue.isEmpty()) {
                            // All done.
                            return;
                        }
                    }
                    catch (final MongoDbException error) {
                        if (myOpen.get()) {
                            LOG.log(Level.WARNING, "Error reading a message: "
                                    + error.getMessage(), error);
                        }
                        // All done.
                        return;
                    }
                }
            }
            finally {
                // Make sure the connection is closed completely.
                IOUtils.close(SocketConnection.this);
            }
        }

        /**
         * Receives and process a single message.
         */
        protected final void receiveOne() {
            final Message received = doReceive();
            if (received instanceof Reply) {
                final Reply reply = (Reply) received;
                final int replyId = reply.getResponseToId();
                boolean took = false;

                // Keep polling the pending queue until we get to
                // message based on a matching replyId.
                try {
                    took = myPendingQueue.poll(myPendingMessage);
                    while (took && (myPendingMessage.getMessageId() != replyId)) {

                        // Note that this message will not get a reply.
                        raiseError(NO_REPLY,
                                myPendingMessage.getReplyCallback());

                        // Keep looking.
                        took = myPendingQueue.poll(myPendingMessage);
                    }

                    if (took) {
                        // Must be the pending message's reply.
                        reply(reply, myPendingMessage.getReplyCallback());
                    }
                    else {
                        LOG.warning("Could not find the callback for reply '"
                                + replyId + "'.");
                    }
                }
                finally {
                    myPendingMessage.clear();
                }
            }
            else if (received != null) {
                LOG.warning("Received a non-Reply message: " + received);
            }
        }
    }

    /**
     * Runnable to push data out over the MongoDB connection.
     * 
     * @copyright 2011, Allanbank Consulting, Inc., All Rights Reserved
     */
    protected class SendRunnable implements Runnable {

        /** Tracks if there are messages in the buffer that need to be flushed. */
        private boolean myNeedToFlush = false;

        /** The {@link PendingMessage} used for the local cached copy. */
        private final PendingMessage myPendingMessage = new PendingMessage();

        /**
         * {@inheritDoc}
         * <p>
         * Overridden to pull messages off the
         * {@link SocketConnection#myToSendQueue} and push them into the socket
         * connection. If <code>null</code> is ever received from a poll of the
         * queue then the socket connection is flushed and blocking call is made
         * to the queue.
         * </p>
         * 
         * @see Runnable#run()
         */
        @Override
        public void run() {
            boolean sawError = false;
            try {
                while (myOpen.get() && !sawError) {
                    try {
                        sendOne();
                    }
                    catch (final InterruptedException ie) {
                        // Handled by loop but if we have a message, need to
                        // tell him something bad happened (but we shouldn't).
                        raiseError(ie, myPendingMessage.getReplyCallback());
                    }
                    catch (final IOException ioe) {
                        LOG.log(Level.WARNING, "I/O Error sending a message.",
                                ioe);
                        raiseError(ioe, myPendingMessage.getReplyCallback());
                        sawError = true;
                    }
                    catch (final RuntimeException re) {
                        LOG.log(Level.WARNING,
                                "Runtime error sending a message.", re);
                        raiseError(re, myPendingMessage.getReplyCallback());
                        sawError = true;
                    }
                    catch (final Error error) {
                        LOG.log(Level.SEVERE, "Error sending a message.", error);
                        raiseError(error, myPendingMessage.getReplyCallback());
                        sawError = true;
                    }
                    finally {
                        myPendingMessage.clear();
                    }
                }
            }
            finally {
                // This may/will fail because we are dying.
                try {
                    if (myOpen.get()) {
                        doFlush();
                    }
                }
                catch (final IOException ioe) {
                    LOG.log(Level.WARNING,
                            "I/O Error on final flush of messages.", ioe);
                }
                finally {
                    // Make sure we get shutdown completely.
                    IOUtils.close(SocketConnection.this);
                }
            }
        }

        /**
         * Flushes the messages in the buffer and clears the need-to-flush flag.
         * 
         * @throws IOException
         *             On a failure flushing the messages.
         */
        protected final void doFlush() throws IOException {
            if (myNeedToFlush) {
                flush();
                myNeedToFlush = false;
            }
        }

        /**
         * Sends a single message.
         * 
         * @throws InterruptedException
         *             If the thread is interrupted waiting for a message to
         *             send.
         * @throws IOException
         *             On a failure sending the message.
         */
        protected final void sendOne() throws InterruptedException, IOException {
            boolean took = false;
            if (myNeedToFlush) {
                took = myToSendQueue.poll(myPendingMessage);
            }
            else {
                myToSendQueue.take(myPendingMessage);
                took = true;
            }

            if (took) {
                final int messageId = myPendingMessage.getMessageId();
                final Message message = myPendingMessage.getMessage();

                // Make sure the message is on the queue before the
                // message is sent to ensure the receive thread can
                // assume an empty pending queue means that there is
                // no message for the reply.
                if ((myPendingMessage.getReplyCallback() != null)
                        && !myPendingQueue.offer(myPendingMessage)) {
                    // Push what we have out before blocking.
                    doFlush();
                    myPendingQueue.put(myPendingMessage);
                }

                myNeedToFlush = true;
                doSend(messageId, message);

                // If shutting down then flush after each message.
                if (myShutdown.get()) {
                    doFlush();
                }

                // We have handed the message off. Not our problem any more.
                // We could legitimately do this before the send but in the case
                // of an I/O error the send's exception is more meaningful then
                // the receivers generic "Didn't get a reply".
                myPendingMessage.clear();
            }
            else {
                doFlush();
            }
        }
    }
}
