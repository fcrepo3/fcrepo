/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.common.xml.namespace;

/**
 * A namespace-qualified name in XML.
 * 
 * @author Chris Wilper
 * @version $Id$
 */
public class QName extends javax.xml.namespace.QName {

    private static final long serialVersionUID = 6368425528110304020L;

    /** The namespace to which this name belongs. */
    public final XMLNamespace namespace;

    /** The local part of the qualified name. */
    public final String localName;

    /**
     * A string of the form: <code>prefix:localName</code>, acceptable for
     * use in an instance document. The prefix used will be the preferred prefix
     * of the namespace.
     */
    public final String qName;

    /**
     * Constructs an instance.
     * 
     * @param namespace
     *        the namespace to which this name belongs.
     * @param localName
     *        the local part of the qualified name.
     * @throws IllegalArgumentException
     *         if either parameter is null.
     */
    public QName(XMLNamespace namespace, String localName) {
        super(namespace.uri, localName, namespace.prefix);
        
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (localName == null) {
            throw new IllegalArgumentException("localName cannot be null");
        }
        this.namespace = namespace;
        this.localName = localName;
        qName = namespace.prefix + ":" + localName;
    }
}
