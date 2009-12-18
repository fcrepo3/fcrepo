/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.test.api.TestAPIA.class,
        fedora.test.api.TestAPIALite.class, fedora.test.api.TestAPIM.class,
        fedora.test.api.TestAPIMLite.class,
        fedora.test.integration.cma.ConflictingDeploymentTests.class,
        fedora.test.integration.cma.ContentModelDSInputTest.class,
        fedora.test.integration.cma.SharedDeploymentTests.class,
        fedora.test.integration.cma.SimpleDeploymentTests.class,
        fedora.test.integration.TestOAIService.class,
        fedora.test.integration.TestCommandLineUtilities.class,
        fedora.test.integration.TestCommandLineFormats.class})
public class AllCommonSystemTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllCommonSystemTests.class
                        .getName());

        suite.addTest(fedora.test.api.TestAPIA.suite());
        suite.addTest(fedora.test.api.TestAPIALite.suite());
        suite.addTest(fedora.test.api.TestAPIM.suite());
        suite.addTest(fedora.test.api.TestAPIMLite.suite());
        suite.addTest(fedora.test.api.TestManagedDatastreams.suite());
        suite.addTest(fedora.test.integration.cma.ConflictingDeploymentTests
                .suite());
        suite.addTest(fedora.test.integration.cma.ContentModelDSInputTest
                .suite());
        suite
                .addTest(fedora.test.integration.cma.SharedDeploymentTests
                        .suite());
        suite
                .addTest(fedora.test.integration.cma.SimpleDeploymentTests
                        .suite());
        suite.addTest(fedora.test.integration.TestOAIService.suite());
        suite.addTest(fedora.test.integration.TestCommandLineUtilities.suite());
        suite.addTest(fedora.test.integration.TestCommandLineFormats.suite());

        return suite;
    }
}
