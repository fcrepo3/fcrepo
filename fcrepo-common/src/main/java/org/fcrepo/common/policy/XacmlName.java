/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.common.policy;

import java.net.URI;
import java.net.URISyntaxException;

import org.jrdf.graph.TypedNodeVisitor;
import org.jrdf.graph.URIReference;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.StringAttribute;

/**
 * A URIReference from a known namespace.
 */
public class XacmlName
        implements URIReference {

    private static final long serialVersionUID = 1L;

    public final XacmlNamespace parent;

    public final String localName;

    public final String datatype;

    public final String uri;

    private final URI m_uri;

    private final StringAttribute m_att;

    private final AnyURIAttribute m_uri_att;

    public XacmlName(XacmlNamespace parent, String localName, String datatype) {
        try {
            this.parent = parent;
            this.localName = localName;
            this.datatype = datatype;
            uri = parent.uri + ":" + localName;
            m_uri = new URI(uri);
            m_att = new StringAttribute(m_uri.toASCIIString());
            m_uri_att = new AnyURIAttribute(m_uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bad URI Syntax", e);
        }
    }

    public XacmlName(XacmlNamespace parent, String localName) {
        this(parent, localName, "");
    }

    /**
     * Does the given string loosely match this name? Either: 1) It matches
     * localName (case insensitive) 2) It matches uri (case sensitive) if
     * (firstLocalNameChar == true): 3) It is one character long, and that
     * character matches the first character of localName (case insensitive)
     */
    public boolean looselyMatches(String in, boolean tryFirstLocalNameChar) {
        if (in == null || in.length() == 0) {
            return false;
        }
        if (in.equalsIgnoreCase(localName)) {
            return true;
        }
        if (in.equals(uri)) {
            return true;
        }
        if (tryFirstLocalNameChar
                && in.length() == 1
                && in.toUpperCase().charAt(0) == localName.toUpperCase()
                        .charAt(0)) {
            return true;
        }
        return false;
    }

    //
    // Implementation of the URIReference interface
    //

    @Override
    public void accept(TypedNodeVisitor visitor) {
        visitor.visitURIReference(this);
    }

    @Override
    public URI getURI() {
        return m_uri;
    }

    public StringAttribute getStringAttribute() {
        return m_att;
    }

    public AnyURIAttribute getURIAttribute(){
        return m_uri_att;
    }

    @Override
    public String toString() {
        return uri + "\t" + datatype;
    }

    @Override
    public String stringValue() {
        return toString();
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String getNamespace() {
        return parent.toString();
    }

	@Override
    public boolean isBlankNode() {
		return false;
	}

	@Override
    public boolean isLiteral() {
		return false;
	}

	@Override
    public boolean isURIReference() {
		return true;
	}

}
