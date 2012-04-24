/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.common;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.common.TestPID.class} )
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {
        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());
        suite.addTest(org.fcrepo.common.TestPID.suite());
        suite.addTest(org.fcrepo.common.TestDateUtility.suite());
        return suite;
    }
}
