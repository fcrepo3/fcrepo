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
        fedora.test.api.TestHTTPStatusCodesConfigQ.class,
        fedora.test.api.TestManyDisseminations.class,
        fedora.test.api.TestRESTAPIConfigQ.class})
public class AllSystemTestsConfigQ {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllSystemTestsConfigQ.class
                        .getName());

        suite.addTest(fedora.test.AllCommonSystemTests.suite());
        suite.addTest(fedora.test.api.TestAPIAConfigA.suite());
        suite.addTest(fedora.test.api.TestAPIALiteConfigA.suite());
        suite.addTest(fedora.test.api.TestHTTPStatusCodesConfigQ.suite());
        suite.addTest(fedora.test.api.TestManyDisseminations.suite());

        return suite;
    }
}
