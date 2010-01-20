/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.fcrepo.server.AllUnitTests;


@RunWith(Suite.class)
@Suite.SuiteClasses( {
        org.fcrepo.server.journal.AllUnitTests.class,
        org.fcrepo.server.messaging.AllUnitTests.class,
        org.fcrepo.server.proxy.AllUnitTests.class,
        org.fcrepo.server.search.AllUnitTests.class,
        org.fcrepo.server.security.AllUnitTests.class,
        org.fcrepo.server.storage.AllUnitTests.class,
        org.fcrepo.server.utilities.AllUnitTests.class,
        org.fcrepo.server.validation.AllUnitTests.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(org.fcrepo.server.journal.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.messaging.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.proxy.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.search.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.security.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.storage.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.utilities.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.validation.AllUnitTests.suite());

        return suite;
    }
}
