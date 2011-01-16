/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.fcrepo.server.AllUnitTests;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.server.config.AllUnitTests.class,
        org.fcrepo.server.messaging.AllUnitTests.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(org.fcrepo.server.config.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.messaging.AllUnitTests.suite());

        return suite;
    }
}
