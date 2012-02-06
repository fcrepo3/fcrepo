/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.test.AllUnitTests.class,
        org.fcrepo.test.AllIntegrationTests.class,
        org.fcrepo.server.AllUnitTests.class,
        org.fcrepo.utilities.AllUnitTests.class})
public class AllOfflineTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllOfflineTests.class.getName());

        suite.addTest(org.fcrepo.test.AllUnitTests.suite());
        suite.addTest(org.fcrepo.test.AllIntegrationTests.suite());
        suite.addTest(org.fcrepo.utilities.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.AllUnitTests.suite());

        return suite;
    }
}
