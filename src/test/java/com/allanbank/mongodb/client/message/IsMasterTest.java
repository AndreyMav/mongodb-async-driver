/*
 * #%L
 * IsMasterTest.java - mongodb-async-driver - Allanbank Consulting, Inc.
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

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.allanbank.mongodb.ReadPreference;

/**
 * IsMasterTest provides tests for the {@link IsMaster} message.
 *
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class IsMasterTest {

    /**
     * Test method for {@link IsMaster#IsMaster()}.
     */
    @Test
    public void testIsMaster() {
        final IsMaster isMaster = new IsMaster();

        assertEquals(AdminCommand.ADMIN_DATABASE, isMaster.getDatabaseName());
        assertEquals(Command.COMMAND_COLLECTION, isMaster.getCollectionName());
        assertEquals(IsMaster.IS_MASTER, isMaster.getCommand());
        assertSame(ReadPreference.PRIMARY, isMaster.getReadPreference());
        assertThat(isMaster.getRequiredVersionRange(), nullValue());
    }
}
