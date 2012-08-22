/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.journal;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.server.journal.helpers.AllUnitTests.class,
        org.fcrepo.server.journal.readerwriter.AllUnitTests.class,
        org.fcrepo.server.journal.xmlhelpers.AllUnitTests.class,
        TestJournalRoundTrip.class})
public class AllUnitTests {

}
