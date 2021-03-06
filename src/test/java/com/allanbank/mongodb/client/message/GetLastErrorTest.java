/*
 * #%L
 * GetLastErrorTest.java - mongodb-async-driver - Allanbank Consulting, Inc.
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

package com.allanbank.mongodb.client.message;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.allanbank.mongodb.Durability;
import com.allanbank.mongodb.bson.builder.BuilderFactory;
import com.allanbank.mongodb.bson.builder.DocumentBuilder;

/**
 * GetLastErrorTest provides tests for the {@link GetLastError} command.
 *
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class GetLastErrorTest {

    /**
     * Test method for {@link GetLastError#GetLastError(String, Durability)}.
     */
    @Test
    public void testGetLastErrorStringDurabilityAck() {

        final GetLastError msg = new GetLastError("foo", Durability.ACK);
        assertEquals("foo", msg.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());

        final DocumentBuilder b = BuilderFactory.start();
        b.addInteger("getlasterror", 1);
        b.addInteger("w", 1);

        assertEquals(b.build(), msg.getQuery());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());
    }

    /**
     * Test method for {@link GetLastError#GetLastError(String, Durability)}.
     */
    @Test
    public void testGetLastErrorStringDurabilityFsync() {

        final GetLastError msg = new GetLastError("foo",
                Durability.fsyncDurable(1000));
        assertEquals("foo", msg.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());

        final DocumentBuilder b = BuilderFactory.start();
        b.addInteger("getlasterror", 1);
        b.addBoolean("fsync", true);
        b.addInteger("wtimeout", 1000);

        assertEquals(b.build(), msg.getQuery());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());
    }

    /**
     * Test method for {@link GetLastError#GetLastError(String, Durability)}.
     */
    @Test
    public void testGetLastErrorStringDurabilityJournal() {

        final GetLastError msg = new GetLastError("foo",
                Durability.journalDurable(1001));
        assertEquals("foo", msg.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());

        final DocumentBuilder b = BuilderFactory.start();
        b.addInteger("getlasterror", 1);
        b.addBoolean("j", true);
        b.addInteger("wtimeout", 1001);

        assertEquals(b.build(), msg.getQuery());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());
    }

    /**
     * Test method for {@link GetLastError#GetLastError(String, Durability)}.
     */
    @Test
    public void testGetLastErrorStringDurabilityReplica() {

        final GetLastError msg = new GetLastError("foo",
                Durability.replicaDurable(1002));
        assertEquals("foo", msg.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());

        final DocumentBuilder b = BuilderFactory.start();
        b.addInteger("getlasterror", 1);
        b.addInteger("wtimeout", 1002);
        b.addInteger("w", 2);

        assertEquals(b.build(), msg.getQuery());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());
    }

    /**
     * Test method for {@link GetLastError#GetLastError(String, Durability)}.
     */
    @Test
    public void testGetLastErrorStringDurabilityReplica5() {

        final GetLastError msg = new GetLastError("foo",
                Durability.replicaDurable(5, 1003));
        assertEquals("foo", msg.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());

        final DocumentBuilder b = BuilderFactory.start();
        b.addInteger("getlasterror", 1);
        b.addInteger("wtimeout", 1003);
        b.addInteger("w", 5);

        assertEquals(b.build(), msg.getQuery());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());
    }

    /**
     * Test method for {@link GetLastError#GetLastError(String, Durability)}.
     */
    @Test
    public void testGetLastErrorStringDurabilityReplicaMode() {

        final GetLastError msg = new GetLastError("foo",
                Durability.replicaDurable("mode", 1004));
        assertEquals("foo", msg.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());

        final DocumentBuilder b = BuilderFactory.start();
        b.addInteger("getlasterror", 1);
        b.addInteger("wtimeout", 1004);
        b.addString("w", "mode");

        assertEquals(b.build(), msg.getQuery());
        assertEquals(Command.COMMAND_COLLECTION, msg.getCollectionName());
    }

}
