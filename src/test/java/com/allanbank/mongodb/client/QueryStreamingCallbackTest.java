/*
 * Copyright 2012, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb.client;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.allanbank.mongodb.Callback;
import com.allanbank.mongodb.ReadPreference;
import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.builder.BuilderFactory;
import com.allanbank.mongodb.bson.builder.DocumentBuilder;
import com.allanbank.mongodb.connection.message.GetMore;
import com.allanbank.mongodb.connection.message.KillCursors;
import com.allanbank.mongodb.connection.message.Query;
import com.allanbank.mongodb.connection.message.Reply;

/**
 * QueryStreamingCallbackTest provides test for the
 * {@link QueryStreamingCallback} class.
 * 
 * @copyright 2012, Allanbank Consulting, Inc., All Rights Reserved
 */
public class QueryStreamingCallbackTest {

    /** The address for the test. */
    private String myAddress = null;

    /** A set of documents for the test. */
    private List<Document> myDocs = null;

    /** A set of documents for the test. */
    private Query myQuery = null;

    /**
     * Creates a set of documents for the test.
     */
    @Before
    public void setUp() {
        final DocumentBuilder b = BuilderFactory.start();
        myDocs = new ArrayList<Document>();
        myDocs.add(b.add("a", 1).build());
        myDocs.add(b.reset().add("a", 2).build());
        myDocs.add(b.reset().add("a", 3).build());
        myDocs.add(b.reset().add("a", 4).build());
        myDocs.add(b.reset().add("a", 5).build());

        myQuery = new Query("db", "c", b.reset().build(), b.build(), 5, 0, 0,
                false, ReadPreference.PRIMARY, false, false, false, false);

        myAddress = "localhost:21017";
    }

    /**
     * Cleans up after the test.
     */
    @After
    public void tearDown() {
        myDocs = null;
        myQuery = null;
        myAddress = null;
    }

    /**
     * Test method for {@link QueryStreamingCallback} getting all of the
     * documents in one batch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAllDocsInFirstReply() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 0, 0, myDocs, false, false, false,
                false);

        for (final Document doc : myDocs) {
            mockCallback.callback(doc);
            expectLastCall();
        }
        mockCallback.callback(isNull(Document.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback} requesting the second
     * batch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAskForMore() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 10, 0, myDocs, false, false, false,
                false);
        final Reply reply2 = new Reply(0, 0, 0, myDocs, false, false, false,
                false);

        for (final Document doc : myDocs) {
            mockCallback.callback(doc);
            expectLastCall();
        }
        expect(mockClient.send(anyObject(GetMore.class), eq(qsCallback)))
                .andReturn(myAddress);
        for (final Document doc : myDocs) {
            mockCallback.callback(doc);
            expectLastCall();
        }
        mockCallback.callback(isNull(Document.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);
        qsCallback.callback(reply2);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback} requesting the second and
     * third batch both of which are empty.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAskForMoreGetNone() {
        final List<Document> empty = Collections.emptyList();

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 10, 0, myDocs, false, false, false,
                false);
        final Reply reply2 = new Reply(0, 10, 0, empty, false, false, false,
                false);
        final Reply reply3 = new Reply(0, 0, 0, empty, false, false, false,
                false);

        for (final Document doc : myDocs) {
            mockCallback.callback(doc);
            expectLastCall();
        }
        expect(mockClient.send(anyObject(GetMore.class), eq(qsCallback)))
                .andReturn(myAddress);
        expect(mockClient.send(anyObject(GetMore.class), eq(qsCallback)))
                .andReturn(myAddress);
        mockCallback.callback(isNull(Document.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);
        qsCallback.callback(reply2);
        qsCallback.callback(reply3);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback} getting all of the
     * documents in one batch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testErrorReply() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 0, 0, myDocs, false, false, true,
                false);

        mockCallback.exception(notNull(Throwable.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback} getting all of the
     * documents in one batch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testErrorReplyWithMessage() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Document replyDoc = BuilderFactory.start().add("ok", 0)
                .add("$err", "This is an error").build();
        final Reply reply = new Reply(0, 0, 0,
                Collections.singletonList(replyDoc), false, false, false, false);

        mockCallback.exception(notNull(Throwable.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback} getting all of the
     * documents in one batch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLateSetAddress() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 0, 0, myDocs, false, false, false,
                false);

        for (final Document doc : myDocs) {
            mockCallback.callback(doc);
            expectLastCall();
        }
        mockCallback.callback(isNull(Document.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.callback(reply);
        qsCallback.setAddress(myAddress);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback#nextBatchSize()}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextBatchSize() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        replay(mockClient, mockCallback);

        final int batchSize = 5;
        int limit = 100;
        myQuery = new Query("db", "c", myDocs.get(0), myDocs.get(0), batchSize,
                limit, 0, false, ReadPreference.PRIMARY, false, false, false,
                false);
        QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);
        assertEquals(batchSize, qsCallback.nextBatchSize());

        limit = 5;
        myQuery = new Query("db", "c", myDocs.get(0), myDocs.get(0), batchSize,
                limit, 0, false, ReadPreference.PRIMARY, false, false, false,
                false);
        qsCallback = new QueryStreamingCallback(mockClient, myQuery,
                mockCallback);
        assertEquals(-limit, qsCallback.nextBatchSize());

        limit = -1;
        myQuery = new Query("db", "c", myDocs.get(0), myDocs.get(0), batchSize,
                limit, 0, false, ReadPreference.PRIMARY, false, false, false,
                false);
        qsCallback = new QueryStreamingCallback(mockClient, myQuery,
                mockCallback);
        assertEquals(batchSize, qsCallback.nextBatchSize());

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for
     * {@link MongoIterator#MongoIterator(Query, Client, String, Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOverLimit() {
        final int batchSize = 5;
        final int limit = 4;
        myQuery = new Query("db", "c", myDocs.get(0), myDocs.get(0), batchSize,
                limit, 0, false, ReadPreference.PRIMARY, false, false, false,
                false);

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 10, 0, myDocs, false, false, false,
                false);

        mockCallback.callback(myDocs.get(0));
        expectLastCall();
        mockCallback.callback(myDocs.get(1));
        expectLastCall();
        mockCallback.callback(myDocs.get(2));
        expectLastCall();
        mockCallback.callback(myDocs.get(3));
        expectLastCall();
        mockCallback.callback(isNull(Document.class));
        expectLastCall();
        expect(
                mockClient.send(anyObject(KillCursors.class),
                        isNull(Callback.class))).andReturn(myAddress);

        replay(mockClient, mockCallback);

        assertNull(qsCallback.getAddress());
        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for
     * {@link MongoIterator#MongoIterator(Query, Client, String, Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testOverLimitCursorAlreadyDead() {
        final int batchSize = 5;
        final int limit = 4;
        myQuery = new Query("db", "c", myDocs.get(0), myDocs.get(0), batchSize,
                limit, 0, false, ReadPreference.PRIMARY, false, false, false,
                false);

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 0, 0, myDocs, false, false, false,
                false);

        mockCallback.callback(myDocs.get(0));
        expectLastCall();
        mockCallback.callback(myDocs.get(1));
        expectLastCall();
        mockCallback.callback(myDocs.get(2));
        expectLastCall();
        mockCallback.callback(myDocs.get(3));
        expectLastCall();
        mockCallback.callback(isNull(Document.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for {@link QueryStreamingCallback} getting all of the
     * documents in one batch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTerminateStreamOnRuntimeExceptionInCallback() {

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 0, 0, myDocs, false, false, false,
                false);
        final RuntimeException error = new RuntimeException("Injected!");

        mockCallback.callback(myDocs.get(0));
        expectLastCall();
        mockCallback.callback(myDocs.get(1));
        expectLastCall().andThrow(error);
        mockCallback.exception(error);
        expectLastCall();

        replay(mockClient, mockCallback);

        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);

        verify(mockClient, mockCallback);
    }

    /**
     * Test method for
     * {@link MongoIterator#MongoIterator(Query, Client, String, Reply)} .
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUnderLimit() {
        final int batchSize = 5;
        final int limit = 7;
        myQuery = new Query("db", "c", myDocs.get(0), myDocs.get(0), batchSize,
                limit, 0, false, ReadPreference.PRIMARY, false, false, false,
                false);

        final Client mockClient = createMock(Client.class);
        final Callback<Document> mockCallback = createMock(Callback.class);

        final QueryStreamingCallback qsCallback = new QueryStreamingCallback(
                mockClient, myQuery, mockCallback);

        final Reply reply = new Reply(0, 10, 0, myDocs, false, false, false,
                false);
        final Reply reply2 = new Reply(0, 0, 0, myDocs, false, false, false,
                false);

        for (final Document doc : myDocs) {
            mockCallback.callback(doc);
            expectLastCall();
        }
        expect(mockClient.send(anyObject(GetMore.class), eq(qsCallback)))
                .andReturn(myAddress);
        mockCallback.callback(myDocs.get(0));
        expectLastCall();
        mockCallback.callback(myDocs.get(1));
        expectLastCall();
        mockCallback.callback(isNull(Document.class));
        expectLastCall();

        replay(mockClient, mockCallback);

        assertNull(qsCallback.getAddress());
        qsCallback.setAddress(myAddress);
        qsCallback.callback(reply);
        qsCallback.callback(reply2);

        verify(mockClient, mockCallback);
    }
}
