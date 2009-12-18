/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

public class FedoraAsProjectNamespace
        extends XacmlNamespace {

    private FedoraAsProjectNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static FedoraAsProjectNamespace onlyInstance =
            new FedoraAsProjectNamespace(NamesNamespace.getInstance(), "fedora");
    static {
        onlyInstance.addNamespace(Release2_1Namespace.getInstance());
    }

    public static final FedoraAsProjectNamespace getInstance() {
        return onlyInstance;
    }

}
