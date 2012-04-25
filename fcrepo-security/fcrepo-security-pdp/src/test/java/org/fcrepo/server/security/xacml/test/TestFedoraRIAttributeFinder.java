
package org.fcrepo.server.security.xacml.test;

import org.fcrepo.server.security.xacml.pdp.finder.attribute.FedoraRIAttributeFinder;
import org.fcrepo.server.security.xacml.util.RIRelationshipResolver;

public class TestFedoraRIAttributeFinder {

    public static void main(String[] args) throws Exception {
        new FedoraRIAttributeFinder(new RIRelationshipResolver());
    }
}
