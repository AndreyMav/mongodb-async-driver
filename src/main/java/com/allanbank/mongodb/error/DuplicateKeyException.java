/*
 * #%L
 * DuplicateKeyException.java - mongodb-async-driver - Allanbank Consulting, Inc.
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
package com.allanbank.mongodb.error;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.allanbank.mongodb.client.Message;
import com.allanbank.mongodb.client.message.Reply;

/**
 * Exception raised when an insert encounters a duplicate key error.
 *
 * @api.yes This class is part of the driver's API. Public and protected members
 *          will be deprecated for at least 1 non-bugfix release (version
 *          numbers are &lt;major&gt;.&lt;minor&gt;.&lt;bugfix&gt;) before being
 *          removed or modified.
 * @copyright 2011-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
@Immutable
@ThreadSafe
public class DuplicateKeyException
        extends ReplyException {

    /** The serialization version for the class. */
    private static final long serialVersionUID = -3588171889388956082L;

    /**
     * Create a new DuplicateKeyException.
     *
     * @param okValue
     *            The value of the "ok" field in the reply document.
     * @param errorNumber
     *            The value of the "errNo" field in the reply document.
     * @param errorMessage
     *            The value of the 'errmsg" field in the reply document.
     * @param message
     *            The message that triggered the message.
     * @param reply
     *            The reply with the error.
     */
    public DuplicateKeyException(final int okValue, final int errorNumber,
            final String errorMessage, final Message message, final Reply reply) {
        super(okValue, errorNumber, errorMessage, message, reply);
    }

    /**
     * Create a new DuplicateKeyException.
     *
     * @param okValue
     *            The value of the "ok" field in the reply document.
     * @param errorNumber
     *            The value of the "errNo" field in the reply document.
     * @param errorMessage
     *            The value of the 'errmsg" field in the reply document.
     * @param reply
     *            The reply with the error.
     */
    public DuplicateKeyException(final int okValue, final int errorNumber,
            final String errorMessage, final Reply reply) {
        this(okValue, errorNumber, errorMessage, null, reply);
    }

}
