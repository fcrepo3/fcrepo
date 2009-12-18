/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.policy;

public class FedoraAsOrganizationNamespace
        extends XacmlNamespace {

    private FedoraAsOrganizationNamespace(XacmlNamespace parent,
                                          String localName) {
        super(parent, localName);
    }

    public static FedoraAsOrganizationNamespace onlyInstance =
            new FedoraAsOrganizationNamespace(UrnNamespace.getInstance(),
                                              "fedora");
    static {
        onlyInstance.addNamespace(NamesNamespace.getInstance());
    }

    public static final FedoraAsOrganizationNamespace getInstance() {
        return onlyInstance;
    }

}
