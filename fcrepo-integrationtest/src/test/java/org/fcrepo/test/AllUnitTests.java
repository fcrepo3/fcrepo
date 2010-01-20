/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.fcrepo.common.Constants;


@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.client.AllUnitTests.class,
                      org.fcrepo.common.AllUnitTests.class,
                      org.fcrepo.server.AllUnitTests.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {

        /* Make sure these are set so we needn't check FEDORA_HOME */
        defineIfNotSet("fedora.hostname", "localhost");
        defineIfNotSet("fedora.port", "8080");
        defineIfNotSet("fedora.appServerContext", Constants.FEDORA_DEFAULT_APP_CONTEXT);
        defineIfNotSet("fedora.baseURL", "http://localhost:8080/"
                       + Constants.FEDORA_DEFAULT_APP_CONTEXT);

        junit.framework.TestSuite suite =
                new junit.framework.TestSuite(AllUnitTests.class.getName());

        suite.addTest(org.fcrepo.client.AllUnitTests.suite());
        suite.addTest(org.fcrepo.common.AllUnitTests.suite());
        suite.addTest(org.fcrepo.server.AllUnitTests.suite());

        return suite;
    }

    private static void defineIfNotSet(String key, String val) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, val);
        }
    }
}
