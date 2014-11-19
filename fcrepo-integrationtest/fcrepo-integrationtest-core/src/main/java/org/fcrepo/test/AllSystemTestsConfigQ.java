/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

// Does not include AllCommonSystemTests, because authz is disabled
@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.test.api.TestAPIA.class,
        org.fcrepo.test.api.TestAPIAConfigA.class,
        org.fcrepo.test.api.TestAPIALite.class,
        org.fcrepo.test.api.TestAPIALiteConfigA.class,
        org.fcrepo.test.api.TestAPIM.class,
        org.fcrepo.test.api.TestAPIMLite.class,
        org.fcrepo.test.api.TestHTTPStatusCodesConfigQ.class,
        org.fcrepo.test.api.TestManagedDatastreams.class,
        org.fcrepo.test.api.TestManyDisseminations.class,
        org.fcrepo.test.api.TestRESTAPIConfigQ.class,
        org.fcrepo.test.integration.TestCommandLineFormats.class,
        org.fcrepo.test.integration.TestCommandLineUtilities.class,
        org.fcrepo.test.integration.TestOAIService.class,
        org.fcrepo.test.integration.TestObjectLastModDate.class,
        org.fcrepo.test.integration.cma.ConflictingDeploymentTests.class,
        org.fcrepo.test.integration.cma.SharedDeploymentTests.class})
public class AllSystemTestsConfigQ {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigQ.class
                        .getName());

        suite.addTest(org.fcrepo.test.api.TestAPIA.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIAConfigA.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIALite.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIALiteConfigA.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIM.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIMLite.suite());
        suite.addTest(org.fcrepo.test.api.TestHTTPStatusCodesConfigQ.suite());
        suite.addTest(org.fcrepo.test.api.TestManagedDatastreams.suite());
        suite.addTest(org.fcrepo.test.api.TestManyDisseminations.suite());
        suite.addTest(org.fcrepo.test.api.TestRESTAPIConfigQ.suite());
        suite.addTest(org.fcrepo.test.integration.TestCommandLineFormats.suite());
        suite.addTest(org.fcrepo.test.integration.TestCommandLineUtilities.suite());
        suite.addTest(org.fcrepo.test.integration.TestOAIService.suite());
        suite.addTest(org.fcrepo.test.integration.TestObjectLastModDate.suite());
        suite.addTest(org.fcrepo.test.integration.cma.ConflictingDeploymentTests.suite());
        suite.addTest(org.fcrepo.test.integration.cma.SharedDeploymentTests.suite());

        return suite;
    }
}
