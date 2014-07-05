package org.fcrepo.server.security;

import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.jboss.security.xacml.sunxacml.Indenter;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.DateTimeAttribute;


public interface Attribute {

    /**
     * Returns the id of this attribute
     *
     * @return the attribute id
     */
    public URI getId();

    /**
     * Returns the data type of this attribute
     *
     * @return the attribute's data type
     */
    public URI getType();

    /**
     * Returns the issuer of this attribute, or null if no issuer was named
     * 
     * @return the issuer or null
     */
    public String getIssuer();

    /**
     * Returns the moment at which the attribute was issued, or null if no
     * issue time was provided
     *
     * @return the time of issuance or null
     */
    public DateTimeAttribute getIssueInstant();

    /**
     * Return all the values
     * @return
     */
    public List<AttributeValue> getValues();

    /**
     * The value of this attribute, or null if no value was included
     *
     * @return the attribute's value or null
     */
    public AttributeValue getValue();

    /**
     * Encodes this attribute into its XML representation and writes
     * this encoding to the given <code>OutputStream</code> with no
     * indentation.
     *
     * @param output a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output);

    /**
     * Encodes this attribute into its XML representation and writes
     * this encoding to the given <code>OutputStream</code> with
     * indentation.
     *
     * @param output a stream into which the XML-encoded data is written
     * @param indenter an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter);

    public String encode();
}
