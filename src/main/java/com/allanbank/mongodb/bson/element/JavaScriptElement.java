/*
 * Copyright 2011-2012, Allanbank Consulting, Inc. 
 *           All Rights Reserved
 */
package com.allanbank.mongodb.bson.element;

import static com.allanbank.mongodb.util.Assertions.assertNotNull;

import com.allanbank.mongodb.bson.Document;
import com.allanbank.mongodb.bson.Element;
import com.allanbank.mongodb.bson.ElementType;
import com.allanbank.mongodb.bson.Visitor;

/**
 * A wrapper for a BSON JavaScript.
 * 
 * @api.yes This class is part of the driver's API. Public and protected members
 *          will be deprecated for at least 1 non-bugfix release (version
 *          numbers are &lt;major&gt;.&lt;minor&gt;.&lt;bugfix&gt;) before being
 *          removed or modified.
 * @copyright 2011-2012, Allanbank Consulting, Inc., All Rights Reserved
 */
public class JavaScriptElement extends AbstractElement {

    /** The BSON type for a string. */
    public static final ElementType TYPE = ElementType.JAVA_SCRIPT;

    /** Serialization version for the class. */
    private static final long serialVersionUID = -180121123367519947L;

    /** The BSON string value. */
    private final String myJavaScript;

    /**
     * Constructs a new {@link JavaScriptElement}.
     * 
     * @param name
     *            The name for the BSON string.
     * @param javaScript
     *            The BSON JavaScript value.
     * @throws IllegalArgumentException
     *             If the {@code name} or {@code javaScript} is
     *             <code>null</code>.
     */
    public JavaScriptElement(final String name, final String javaScript) {
        super(name);

        assertNotNull(javaScript,
                "JavaScript element's code block cannot be null.");

        myJavaScript = javaScript;

    }

    /**
     * Accepts the visitor and calls the
     * {@link Visitor#visitJavaScript(String,String)} method.
     * 
     * @see Element#accept(Visitor)
     */
    @Override
    public void accept(final Visitor visitor) {
        visitor.visitJavaScript(getName(), getJavaScript());
    }

    /**
     * Determines if the passed object is of this same type as this object and
     * if so that its fields are equal.
     * 
     * @param object
     *            The object to compare to.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = false;
        if (this == object) {
            result = true;
        }
        else if ((object != null) && (getClass() == object.getClass())) {
            final JavaScriptElement other = (JavaScriptElement) object;

            result = super.equals(object)
                    && nullSafeEquals(myJavaScript, other.myJavaScript);
        }
        return result;
    }

    /**
     * Returns the BSON JavaScript value.
     * 
     * @return The BSON JavaScript value.
     */
    public String getJavaScript() {
        return myJavaScript;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ElementType getType() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the result of {@link #getJavaScript()}.
     * </p>
     * <p>
     * <em>Implementation Note:</em> The return type cannot be a String here as
     * {@link JavaScriptWithScopeElement} returns a {@link Document}.
     * </p>
     */
    @Override
    public Object getValueAsObject() {
        return getJavaScript();
    }

    /**
     * Computes a reasonable hash code.
     * 
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + super.hashCode();
        result = (31 * result)
                + ((myJavaScript != null) ? myJavaScript.hashCode() : 3);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a new {@link JavaScriptElement}.
     * </p>
     */
    @Override
    public JavaScriptElement withName(final String name) {
        return new JavaScriptElement(name, myJavaScript);
    }
}
