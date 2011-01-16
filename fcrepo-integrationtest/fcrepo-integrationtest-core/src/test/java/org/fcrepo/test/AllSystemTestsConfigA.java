/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.test.AllCommonSystemTests.class,
        org.fcrepo.test.api.TestAPIAConfigA.class,
        org.fcrepo.test.api.TestAPIALiteConfigA.class,
        org.fcrepo.test.api.TestHTTPStatusCodesConfigA.class,
        org.fcrepo.test.api.TestManyDisseminations.class,
        org.fcrepo.test.api.TestRESTAPI.class,
        // Disabled until FCREPO-798 is public:
        // org.fcrepo.test.api.TestAPIM2.class,
        org.fcrepo.test.api.TestAdminAPI.class})
public class AllSystemTestsConfigA {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigA.class
                        .getName());

        suite.addTest(org.fcrepo.test.AllCommonSystemTests.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIAConfigA.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIALiteConfigA.suite());
        suite.addTest(org.fcrepo.test.api.TestHTTPStatusCodesConfigA.suite());
        suite.addTest(org.fcrepo.test.api.TestManyDisseminations.suite());
        suite.addTest(org.fcrepo.test.api.TestRESTAPI.suite());
        suite.addTest(org.fcrepo.test.api.TestAdminAPI.suite());
        // Disabled until FCREPO-798 is public:
        // suite.addTest(org.fcrepo.test.api.TestAPIM2.suite());

        return suite;
    }
}
