/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import fedora.common.Constants;

@RunWith(Suite.class)
@Suite.SuiteClasses( {fedora.client.AllUnitTests.class,
                      fedora.common.AllUnitTests.class,
                      fedora.server.AllUnitTests.class})
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

        suite.addTest(fedora.client.AllUnitTests.suite());
        suite.addTest(fedora.common.AllUnitTests.suite());
        suite.addTest(fedora.server.AllUnitTests.suite());

        return suite;
    }

    private static void defineIfNotSet(String key, String val) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, val);
        }
    }
}
