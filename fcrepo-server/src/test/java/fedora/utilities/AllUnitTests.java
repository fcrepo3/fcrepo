/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.utilities.install.container.AllUnitTests.class,
    TestBase64.class,
    TestFileUtils.class,
    TestZip.class,
    NamespaceContextImplTest.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(fedora.utilities.install.container.AllUnitTests.suite());
        suite.addTestSuite(TestBase64.class);
        suite.addTestSuite(TestFileUtils.class);
        suite.addTestSuite(TestZip.class);
        suite.addTest(NamespaceContextImplTest.suite());

        return suite;
    }
}
