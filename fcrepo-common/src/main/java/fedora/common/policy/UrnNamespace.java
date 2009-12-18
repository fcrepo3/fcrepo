/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

public class UrnNamespace
        extends XacmlNamespace {

    private UrnNamespace(XacmlNamespace parent, String localName) {
        super(parent, localName);
    }

    public static UrnNamespace onlyInstance = new UrnNamespace(null, "urn");
    static {
        onlyInstance.addNamespace(FedoraAsOrganizationNamespace.getInstance());
    }

    public static final UrnNamespace getInstance() {
        return onlyInstance;
    }

}
