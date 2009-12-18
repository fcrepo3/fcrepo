/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import fedora.common.xml.namespace.XMLNamespace;


/**
 * An implementation of {@link NamespaceContext NamespaceContext} that provides
 * an addNamespace method.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class NamespaceContextImpl
        implements NamespaceContext {

    private final Map<String, String> prefix2ns = new ConcurrentHashMap<String, String>();

    public NamespaceContextImpl() {
        prefix2ns.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        prefix2ns.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
    }

    public NamespaceContextImpl(String prefix, String namespaceURI) {
        this();
        this.addNamespace(prefix, namespaceURI);
    }

    /**
     * Constructor that takes a Map of prefix to namespaces.
     *
     * @param prefix2ns a mapping of prefixes to namespaces.
     * @throws IllegalArgumentException if prefix2ns contains
     * {@value XMLConstants.XML_NS_URI} or {@value XMLConstants.XMLNS_ATTRIBUTE_NS_URI}
     */
    public NamespaceContextImpl(Map<String, String> prefix2ns) {
        this();
        for (String prefix : prefix2ns.keySet()) {
            addNamespace(prefix, prefix2ns.get(prefix));
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("null prefix not allowed.");
        }
        if (prefix2ns.containsKey(prefix)) {
            return prefix2ns.get(prefix);
        }
        return XMLNamespace.NULL_NS_URI;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("null namespaceURI not allowed.");
        }
        for (String prefix : prefix2ns.keySet()) {
            if (prefix2ns.get(prefix).equals(namespaceURI)) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<String> getPrefixes(String namespaceURI) {
        List<String> prefixes = new ArrayList<String>();
        for (String prefix : prefix2ns.keySet()) {
            if (prefix2ns.containsKey(prefix) &&
                    prefix2ns.get(prefix).equals(namespaceURI)) {
                prefixes.add(prefix);
            }
        }
        return Collections.unmodifiableList(prefixes).iterator();
    }

    /**
     * Add a prefix to namespace mapping.
     *
     * @param prefix
     * @param namespaceURI
     * @throws IllegalArgumentException if namespaceURI is one of
     * {@value XMLConstants.XML_NS_URI} or {@value XMLConstants.XMLNS_ATTRIBUTE_NS_URI}
     */
    public void addNamespace(String prefix, String namespaceURI) {
        if (prefix == null || namespaceURI == null) {
            throw new IllegalArgumentException("null arguments not allowed.");
        }
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            throw new IllegalArgumentException("Adding a new namespace for " +
                XMLConstants.XML_NS_URI + "not allowed.");
        } else if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            throw new IllegalArgumentException("Adding a new namespace for " +
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI + "not allowed.");
        }
        prefix2ns.put(prefix, namespaceURI);
    }

}
