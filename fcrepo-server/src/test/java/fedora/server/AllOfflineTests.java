/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.server.AllUnitTests.class,
        fedora.server.AllIntegrationTests.class})
public class AllOfflineTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllOfflineTests.class.getName());

        suite.addTest(fedora.server.AllUnitTests.suite());
        suite.addTest(fedora.server.AllIntegrationTests.suite());

        return suite;
    }
}
