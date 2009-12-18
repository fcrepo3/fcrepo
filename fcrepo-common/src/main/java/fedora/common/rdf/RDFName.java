/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import java.net.URI;
import java.net.URISyntaxException;

import org.jrdf.graph.TypedNodeVisitor;
import org.jrdf.graph.URIReference;

/**
 * A URIReference from a known namespace.
 */
public class RDFName
        implements URIReference {

    private static final long serialVersionUID = 1L;

    public final RDFNamespace namespace;

    public final String localName;

    public final String uri;

    public final String qName;

    private URI m_uri;

    public RDFName(RDFNamespace namespace, String localName) {
        try {
            this.namespace = namespace;
            this.localName = localName;
            uri = namespace.uri + localName;
            qName = namespace.prefix + ":" + this.localName;
            m_uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bad URI Syntax", e);
        }
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
        if (in.indexOf(localName) != -1) {
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

    public void accept(TypedNodeVisitor visitor) {
        visitor.visitURIReference(this);
    }

    public URI getURI() {
        return m_uri;
    }

    @Override
    public String toString() {
        return uri;
    }

    public String stringValue() {
        return toString();
    }

    public String getLocalName() {
        return localName;
    }

    public String getNamespace() {
        return namespace.toString();
    }

	public boolean isBlankNode() {
		return false;
	}

	public boolean isLiteral() {
		return false;
	}

	public boolean isURIReference() {
		return true;
	}

}
