/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

/**
 * @author Sandy Payette
 * @version $Id$
 */
public class DeploymentDSBindSpec {

    public String serviceDeploymentPID;

    public String bindSpecLabel;

    public String state;

    public DeploymentDSBindRule[] dsBindRules;

    public DeploymentDSBindSpec() {
    }
}
