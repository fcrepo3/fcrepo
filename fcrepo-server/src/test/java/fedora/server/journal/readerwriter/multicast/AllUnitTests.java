/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal.readerwriter.multicast;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestMulticastJournalWriterInitializations.class,
        TestMulticastJournalWriterOperation.class,
        TestJournalEntrySizeEstimator.class, TestLocalDirectoryTransport.class,
        fedora.server.journal.readerwriter.multicast.rmi.AllUnitTests.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(TestMulticastJournalWriterInitializations.suite());
        suite.addTest(TestMulticastJournalWriterOperation.suite());
        suite.addTest(TestJournalEntrySizeEstimator.suite());
        suite.addTest(TestLocalDirectoryTransport.suite());
        suite
                .addTest(fedora.server.journal.readerwriter.multicast.rmi.AllUnitTests
                        .suite());

        return suite;
    }
}
