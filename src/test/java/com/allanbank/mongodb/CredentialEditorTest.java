/*
 * Copyright 2014, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */

package com.allanbank.mongodb;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * CredentialEditorTest provides tests for the {@link CredentialEditor} class.
 * 
 * @copyright 2014, Allanbank Consulting, Inc., All Rights Reserved
 */
public class CredentialEditorTest {

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetAsTextNoDb() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/");

        assertThat(
                editor.getValue(),
                is((Object) Credential.builder().userName("user")
                        .setPassword("password".toCharArray()).build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetAsTextString() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("db").build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithAuthMethodGssApi() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db?authmechanism=GssApi");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("db").kerberos().build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithAuthMethodMongodbCr() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db?authmechanism=MONGODB-CR");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("db").build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithAuthMethodPlainSasl() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db?authmechanism=plain");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("db").plainSasl().build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithAuthMethodUnknown() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db?authmechanism=WTF");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("db").build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithAuthMethodX509() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db?authmechanism=MONGODB-X509");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("db").x509().build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithAuthMethodX509NoUser() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setValue("Hummm");
        editor.setAsText("mongodb://host:port/db?authmechanism=MONGODB-X509");

        // Should not set null.
        assertThat(editor.getValue(), is((Object) "Hummm"));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithauthsource() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/db?authsource=other");

        assertThat(editor.getValue(), is((Object) Credential.builder()
                .userName("user").setPassword("password".toCharArray())
                .database("other").build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithauthsourceButNoUser() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://host:port/db?authsource=other");

        assertThat(editor.getValue(), nullValue());
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithgssapiservicename() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://user:password@host:port/?gssapiservicename=mongo");

        assertThat(
                editor.getValue(),
                is((Object) Credential.builder().userName("user")
                        .setPassword("password".toCharArray()).kerberos()
                        .addOption("kerberos.service.name", "mongo").build()));
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithgssapiservicenameButNoUser() {
        final CredentialEditor editor = new CredentialEditor();

        editor.setAsText("mongodb://host:port/?gssapiservicename=mongo");

        assertThat(editor.getValue(), nullValue());
    }

    /**
     * Test method for {@link CredentialEditor#setAsText(String)}.
     */
    @Test
    public void testSetWithNonUri() {
        final CredentialEditor editor = new CredentialEditor();

        try {
            editor.setAsText("f");
            fail("Should have thrown an exception.");
        }
        catch (final IllegalArgumentException good) {
            // OK.
        }
    }

}
