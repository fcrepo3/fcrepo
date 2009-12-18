/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.journal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.server.journal.readerwriter.AllIntegrationTests.class})
public class AllIntegrationTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllIntegrationTests.class
                        .getName());

        suite.addTest(fedora.server.journal.readerwriter.AllIntegrationTests
                .suite());

        return suite;
    }
}
