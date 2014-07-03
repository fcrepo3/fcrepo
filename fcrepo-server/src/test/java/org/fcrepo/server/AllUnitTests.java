/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses( {
        BasicServerTest.class,
        org.fcrepo.server.access.AllUnitTests.class,
        org.fcrepo.server.config.AllUnitTests.class,
        org.fcrepo.server.journal.AllUnitTests.class,
        org.fcrepo.server.messaging.AllUnitTests.class,
        org.fcrepo.server.proxy.AllUnitTests.class,
        org.fcrepo.server.rest.AllUnitTests.class,
        org.fcrepo.server.search.AllUnitTests.class,
        org.fcrepo.server.security.AllUnitTests.class,
        org.fcrepo.server.storage.AllUnitTests.class,
        org.fcrepo.server.utilities.AllUnitTests.class,
        org.fcrepo.server.utilities.rebuild.AllUnitTests.class,
        org.fcrepo.server.validation.AllUnitTests.class})
public class AllUnitTests {

}
