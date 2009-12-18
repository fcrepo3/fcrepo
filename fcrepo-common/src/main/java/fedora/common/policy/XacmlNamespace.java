/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

import java.util.Vector;

public abstract class XacmlNamespace {

    public String uri;

    private final Vector<XacmlNamespace> memberNamespaces =
            new Vector<XacmlNamespace>();

    private final Vector<XacmlName> memberNames = new Vector<XacmlName>();

    protected XacmlNamespace(XacmlNamespace parent, String localName) {
        uri = (parent == null ? "" : parent.uri + ":") + localName;
    }

    XacmlNamespace addNamespace(XacmlNamespace namespace) {
        XacmlNamespace result = null;
        if (memberNamespaces.add(namespace)) {
            result = namespace;
        }
        return result;
    }

    XacmlName addName(XacmlName name) {
        XacmlName result = null;
        if (memberNames.add(name)) {
            result = name;
        }
        return result;
    }

    public void flatRep(Vector<XacmlName> flatRep) {
        flatRep.addAll(memberNames);
        for (int i = 0; i < memberNamespaces.size(); i++) {
            memberNamespaces.get(i).flatRep(flatRep);
        }
    }

}
