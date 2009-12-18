/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.test.AllCommonSystemTests.class,
        fedora.test.api.TestAPIAConfigA.class,
        fedora.test.api.TestAPIALiteConfigA.class,
        fedora.test.api.TestHTTPStatusCodesConfigA.class,
        fedora.test.api.TestManyDisseminations.class})
public class AllSystemTestsConfigA {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigA.class
                        .getName());

        suite.addTest(fedora.test.AllCommonSystemTests.suite());
        suite.addTest(fedora.test.api.TestAPIAConfigA.suite());
        suite.addTest(fedora.test.api.TestAPIALiteConfigA.suite());
        suite.addTest(fedora.test.api.TestHTTPStatusCodesConfigA.suite());
        suite.addTest(fedora.test.api.TestManyDisseminations.suite());

        return suite;
    }
}
