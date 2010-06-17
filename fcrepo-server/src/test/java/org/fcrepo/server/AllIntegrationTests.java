/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.server.resourceIndex.AllIntegrationTests.class,
        org.fcrepo.server.search.AllIntegrationTests.class,
        org.fcrepo.server.utilities.AllIntegrationTests.class,
        org.fcrepo.server.journal.AllIntegrationTests.class})
public class AllIntegrationTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllIntegrationTests.class
                        .getName());

        suite.addTest(org.fcrepo.server.resourceIndex.AllIntegrationTests.suite());
        suite.addTest(org.fcrepo.server.search.AllIntegrationTests.suite());
        suite.addTest(org.fcrepo.server.utilities.AllIntegrationTests.suite());
        suite.addTest(org.fcrepo.server.journal.AllIntegrationTests.suite());

        return suite;
    }
}
