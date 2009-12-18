/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.format;

import fedora.common.xml.namespace.XMLNamespace;

/**
 * An XML format.
 * 
 * @author Chris Wilper
 */
public class XMLFormat {

    /** The URI of this format. */
    public final String uri;

    /** The primary XML namespace of this format. */
    public final XMLNamespace namespace;

    /** The primary public location of the XSD schema for this format. */
    public final String xsdLocation;

    /**
     * Constructs an instance.
     * 
     * @param uri
     *        the URI of the format.
     * @param xmlNamespace
     *        the primary XML namespace.
     * @param xsdSchemaLocation
     *        the public location of the XSD schema.
     * @throws IllegalArgumentException
     *         if any parameter is null.
     */
    public XMLFormat(String uri, XMLNamespace namespace, String xsdLocation) {
        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (xsdLocation == null) {
            throw new IllegalArgumentException("xsdLocation cannot be null");
        }
        this.uri = uri;
        this.namespace = namespace;
        this.xsdLocation = xsdLocation;
    }

    //---
    // Object overrides
    //---

    /**
     * Returns the URI of the format. {@inheritDoc}
     */
    @Override
    public String toString() {
        return uri;
    }

    /**
     * Returns true iff the given object is an instance of this class and has
     * the same URI. {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof XMLFormat) {
            XMLFormat f = (XMLFormat) o;
            return uri.equals(f.uri);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return uri.hashCode();
    }

}
