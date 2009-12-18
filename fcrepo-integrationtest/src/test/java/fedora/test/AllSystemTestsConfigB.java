/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.test.AllCommonSystemTests.class,
        fedora.test.api.TestAuthentication.class,
        fedora.test.api.TestHTTPStatusCodesConfigB.class,
        fedora.test.api.TestXACMLPolicies.class,
        fedora.test.api.TestRelationships.class,
        fedora.test.api.TestRISearch.class,
        fedora.test.api.TestManagementNotifications.class,
        fedora.test.api.TestRESTAPI.class})
public class AllSystemTestsConfigB {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigB.class
                        .getName());

        suite.addTest(fedora.test.AllCommonSystemTests.suite());
        suite.addTest(fedora.test.api.TestAuthentication.suite());
        suite.addTest(fedora.test.api.TestHTTPStatusCodesConfigB.suite());
        suite.addTest(fedora.test.api.TestXACMLPolicies.suite());
        suite.addTest(fedora.test.api.TestRelationships.suite());
        suite.addTest(fedora.test.api.TestRISearch.suite());
        suite.addTest(fedora.test.api.TestManagementNotifications.suite());
        suite.addTest(fedora.test.api.TestRESTAPI.suite());

        return suite;
    }
}
