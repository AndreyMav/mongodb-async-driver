/*
 * #%L
 * SymbolElementTest.java - mongodb-async-driver - Allanbank Consulting, Inc.
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

package com.allanbank.mongodb.bson.element;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.allanbank.mongodb.bson.Element;
import com.allanbank.mongodb.bson.ElementType;
import com.allanbank.mongodb.bson.Visitor;

/**
 * SymbolElementTest provides tests for the {@link SymbolElement} class.
 *
 * @copyright 2012-2013, Allanbank Consulting, Inc., All Rights Reserved
 */
public class SymbolElementTest {

    /**
     * Test method for
     * {@link SymbolElement#accept(com.allanbank.mongodb.bson.Visitor)} .
     */
    @Test
    public void testAccept() {
        final SymbolElement element = new SymbolElement("foo", "string");

        final Visitor mockVisitor = createMock(Visitor.class);

        mockVisitor.visitSymbol(eq("foo"), eq("string"));
        expectLastCall();

        replay(mockVisitor);

        element.accept(mockVisitor);

        verify(mockVisitor);
    }

    /**
     * Test method for {@link SymbolElement#compareTo(Element)}.
     */
    @Test
    public void testCompareTo() {
        final SymbolElement a1 = new SymbolElement("a", "1");
        final SymbolElement a11 = new SymbolElement("a", "11");
        final SymbolElement b1 = new SymbolElement("b", "1");

        final StringElement i = new StringElement("a", "2");

        final Element other = new MaxKeyElement("a");

        assertEquals(0, a1.compareTo(a1));

        assertTrue(a1.compareTo(a11) < 0);
        assertTrue(a11.compareTo(a1) > 0);

        assertTrue(a1.compareTo(b1) < 0);
        assertTrue(b1.compareTo(a1) > 0);

        assertTrue(a1.compareTo(i) < 0);
        assertTrue(i.compareTo(a1) > 0);

        assertTrue(a1.compareTo(other) < 0);
        assertTrue(other.compareTo(a1) > 0);
    }

    /**
     * Test method for {@link SymbolElement#equals(java.lang.Object)} .
     */
    @Test
    public void testEqualsObject() {
        final Random random = new Random(System.currentTimeMillis());

        final List<Element> objs1 = new ArrayList<Element>();
        final List<Element> objs2 = new ArrayList<Element>();

        for (final String name : Arrays.asList("1", "foo", "bar", "baz", "2")) {
            for (int i = 0; i < 10; ++i) {
                final String value = "" + random.nextLong();
                objs1.add(new SymbolElement(name, value));
                objs2.add(new SymbolElement(name, value));
            }
        }

        // Sanity check.
        assertEquals(objs1.size(), objs2.size());

        for (int i = 0; i < objs1.size(); ++i) {
            final Element obj1 = objs1.get(i);
            Element obj2 = objs2.get(i);

            assertTrue(obj1.equals(obj1));
            assertNotSame(obj1, obj2);
            assertEquals(obj1, obj2);

            assertEquals(obj1.hashCode(), obj2.hashCode());

            for (int j = i + 1; j < objs1.size(); ++j) {
                obj2 = objs2.get(j);

                assertFalse(obj1.equals(obj2));
                assertFalse(obj1.hashCode() == obj2.hashCode());
            }

            assertFalse(obj1.equals("foo"));
            assertFalse(obj1.equals(null));
            assertFalse(obj1.equals(new MaxKeyElement(obj1.getName())));
        }
    }

    /**
     * Test method for {@link SymbolElement#getSymbol()}.
     */
    @Test
    public void testGetValue() {
        final SymbolElement element = new SymbolElement("foo", "string");

        assertEquals("string", element.getSymbol());
    }

    /**
     * Test method for
     * {@link SymbolElement#SymbolElement(java.lang.String, String)} .
     */
    @Test
    public void testSymbolElement() {
        final SymbolElement element = new SymbolElement("foo", "string");

        assertEquals("foo", element.getName());
        assertEquals("string", element.getSymbol());
        assertEquals(ElementType.SYMBOL, element.getType());
    }

    /**
     * Test method for {@link SymbolElement#SymbolElement}.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnNullName() {

        new SymbolElement(null, "s");
    }

    /**
     * Test method for {@link SymbolElement#SymbolElement}.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testThrowsOnNullValue() {

        new SymbolElement("s", null);
    }

    /**
     * Test method for {@link SymbolElement#toString()}.
     */
    @Test
    public void testToString() {
        final SymbolElement element = new SymbolElement("foo", "string");

        assertEquals("foo : string", element.toString());
    }

    /**
     * Test method for {@link SymbolElement#getValueAsObject()}.
     */
    @Test
    public void testValueAsObject() {
        final SymbolElement element = new SymbolElement("foo", "string");

        assertEquals("string", element.getValueAsObject());
    }

    /**
     * Test method for {@link SymbolElement#getValueAsString()}.
     */
    @Test
    public void testValueAsString() {
        final SymbolElement element = new SymbolElement("foo", "string");

        assertEquals("string", element.getValueAsString());
    }

    /**
     * Test method for {@link SymbolElement#withName(String)}.
     */
    @Test
    public void testWithName() {
        SymbolElement element = new SymbolElement("foo", "string");

        element = element.withName("bar");
        assertEquals("bar", element.getName());
        assertEquals("string", element.getSymbol());
        assertEquals(ElementType.SYMBOL, element.getType());
    }

    /**
     * Test method for {@link SymbolElement#withName(String)}.
     */
    @Test
    public void testWithNameWhenSameName() {
        final SymbolElement element = new SymbolElement("foo", "string");

        assertSame(element, element.withName("foo"));
    }
}
