/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.test.AllCommonSystemTests.class,
        org.fcrepo.test.api.TestAuthentication.class,
        org.fcrepo.test.api.TestHTTPStatusCodesConfigC.class,
        org.fcrepo.test.fesl.policy.TestPolicies.class,
        org.fcrepo.test.fesl.policyindex.TestPolicyIndex.class,
        org.fcrepo.test.fesl.restapi.TestREST.class,
        org.fcrepo.test.api.TestRelationships.class,
        org.fcrepo.test.api.TestRISearch.class,
        org.fcrepo.server.messaging.AtomAPIMMessageTest.class,
        org.fcrepo.test.api.TestRESTAPI.class,
        org.fcrepo.test.api.TestAdminAPI.class})
public class AllSystemTestsConfigC {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigC.class
                        .getName());

        suite.addTest(org.fcrepo.test.AllCommonSystemTests.suite());
        suite.addTest(org.fcrepo.test.api.TestAuthentication.suite());
        suite.addTest(org.fcrepo.test.api.TestHTTPStatusCodesConfigC.suite());
        suite.addTest(org.fcrepo.test.fesl.policy.TestPolicies.suite());
        suite.addTest(org.fcrepo.test.fesl.policyindex.TestPolicyIndex.suite());
        suite.addTest(org.fcrepo.test.fesl.restapi.TestREST.suite());
        suite.addTest(org.fcrepo.test.api.TestRelationships.suite());
        suite.addTest(org.fcrepo.test.api.TestRISearch.suite());
        suite.addTest(org.fcrepo.test.api.TestRESTAPI.suite());
        suite.addTest(org.fcrepo.server.messaging.AtomAPIMMessageTest.suite());
        suite.addTest(org.fcrepo.test.api.TestAdminAPI.suite());

        return suite;
    }
}
