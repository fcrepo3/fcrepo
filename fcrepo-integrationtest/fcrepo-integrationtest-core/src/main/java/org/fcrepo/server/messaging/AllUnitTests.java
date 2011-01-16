/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.messaging;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.fcrepo.server.messaging.AllUnitTests;

import junit.framework.JUnit4TestAdapter;

@RunWith(Suite.class)
@Suite.SuiteClasses( {org.fcrepo.server.messaging.AtomAPIMMessageTest.class,
        org.fcrepo.server.messaging.NotificationInvocationHandlerTest.class})
public class AllUnitTests {

    // Supports legacy tests runners
    public static junit.framework.Test suite() throws Exception {
        return new JUnit4TestAdapter(AllUnitTests.class);
    }
}
