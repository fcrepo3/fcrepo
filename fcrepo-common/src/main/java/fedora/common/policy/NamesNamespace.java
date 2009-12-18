/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

public class NamesNamespace
        extends XacmlNamespace {

    private NamesNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static NamesNamespace onlyInstance =
            new NamesNamespace(FedoraAsOrganizationNamespace.getInstance(),
                               "names");
    static {
        onlyInstance.addNamespace(FedoraAsProjectNamespace.getInstance());
    }

    public static final NamesNamespace getInstance() {
        return onlyInstance;
    }

}
