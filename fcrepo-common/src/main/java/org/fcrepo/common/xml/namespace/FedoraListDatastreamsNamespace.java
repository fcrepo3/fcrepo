/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.common.xml.namespace;

/**
 * The Fedora ListDatastreams XML namespace.
 *
 * <pre>
 * Namespace URI    : info:fedora/fedora-system:def/listDatastreams#
 * Preferred Prefix : listDatastreams
 * </pre>
 *
 * @author Edwin Shin
 * @version $Id$
 * @since 3.4.0
 */
public class FedoraListDatastreamsNamespace
        extends XMLNamespace {

    //---
    // Singleton instantiation
    //---

    /** The only instance of this class. */
    private static final FedoraListDatastreamsNamespace ONLY_INSTANCE =
            new FedoraListDatastreamsNamespace();

    /**
     * Constructs the instance.
     */
    private FedoraListDatastreamsNamespace() {
        super("info:fedora/fedora-system:def/listDatastreams#", "listDatastreams");
    }

    /**
     * Gets the only instance of this class.
     *
     * @return the instance.
     */
    public static FedoraListDatastreamsNamespace getInstance() {
        return ONLY_INSTANCE;
    }

}
