/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.test.api.TestAPIA.class,
        org.fcrepo.test.api.TestAPIALite.class, org.fcrepo.test.api.TestAPIM.class,
        org.fcrepo.test.api.TestAPIMLite.class,
        org.fcrepo.test.integration.cma.ConflictingDeploymentTests.class,
        org.fcrepo.test.integration.cma.ContentModelDSInputTest.class,
        org.fcrepo.test.integration.cma.SharedDeploymentTests.class,
        org.fcrepo.test.integration.cma.SimpleDeploymentTests.class,
        org.fcrepo.test.integration.TestOAIService.class,
        org.fcrepo.test.integration.TestCommandLineUtilities.class,
        org.fcrepo.test.integration.TestCommandLineFormats.class})
public class AllCommonSystemTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllCommonSystemTests.class
                        .getName());

        suite.addTest(org.fcrepo.test.api.TestAPIA.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIALite.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIM.suite());
        suite.addTest(org.fcrepo.test.api.TestAPIMLite.suite());
        suite.addTest(org.fcrepo.test.api.TestManagedDatastreams.suite());
        suite.addTest(org.fcrepo.test.integration.cma.ConflictingDeploymentTests
                .suite());
        suite.addTest(org.fcrepo.test.integration.cma.ContentModelDSInputTest
                .suite());
        suite
                .addTest(org.fcrepo.test.integration.cma.SharedDeploymentTests
                        .suite());
        suite
                .addTest(org.fcrepo.test.integration.cma.SimpleDeploymentTests
                        .suite());
        suite.addTest(org.fcrepo.test.integration.TestOAIService.suite());
        suite.addTest(org.fcrepo.test.integration.TestCommandLineUtilities.suite());
        suite.addTest(org.fcrepo.test.integration.TestCommandLineFormats.suite());

        return suite;
    }
}
