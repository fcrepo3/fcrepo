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
        org.fcrepo.test.api.TestHTTPStatusCodesConfigB.class,
        org.fcrepo.test.api.TestXACMLPolicies.class,
        org.fcrepo.test.api.TestRelationships.class,
        org.fcrepo.test.api.TestRISearch.class,
        org.fcrepo.test.api.TestManagementNotifications.class,
        org.fcrepo.test.api.TestRESTAPI.class})
public class AllSystemTestsConfigB {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigB.class
                        .getName());

        suite.addTest(org.fcrepo.test.AllCommonSystemTests.suite());
        suite.addTest(org.fcrepo.test.api.TestAuthentication.suite());
        suite.addTest(org.fcrepo.test.api.TestHTTPStatusCodesConfigB.suite());
        suite.addTest(org.fcrepo.test.api.TestXACMLPolicies.suite());
        suite.addTest(org.fcrepo.test.api.TestRelationships.suite());
        suite.addTest(org.fcrepo.test.api.TestRISearch.suite());
        suite.addTest(org.fcrepo.test.api.TestManagementNotifications.suite());
        suite.addTest(org.fcrepo.test.api.TestRESTAPI.suite());

        return suite;
    }
}
