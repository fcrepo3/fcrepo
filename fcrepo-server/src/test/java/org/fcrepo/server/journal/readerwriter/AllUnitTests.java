/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal.readerwriter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
        org.fcrepo.server.journal.readerwriter.multifile.AllUnitTests.class,
        org.fcrepo.server.journal.readerwriter.multicast.AllUnitTests.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(org.fcrepo.server.journal.readerwriter.multifile.AllUnitTests
                .suite());
        suite.addTest(org.fcrepo.server.journal.readerwriter.multicast.AllUnitTests
                .suite());

        return suite;
    }
}
