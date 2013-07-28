/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static org.fcrepo.test.api.TestHTTPStatusCodes.RI_SEARCH_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.checkError;

import org.fcrepo.test.FedoraServerTestCase;
import org.junit.AfterClass;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;


/**
 * HTTP status code tests to be run when API-A authentication is off and the
 * resource index is disabled.
 *
 * @author Chris Wilper
 */
public class TestHTTPStatusCodesConfigA
        extends FedoraServerTestCase {

    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite("TestHTTPStatusCodesConfigA TestSuite");
        suite.addTest(new JUnit4TestAdapter(TestHTTPStatusCodesConfigA.class));
        suite.addTest(org.fcrepo.test.api.TestHTTPStatusCodes.suite());
        return suite;
    }
        
    @AfterClass
    public static void cleanUp() throws Exception {
        TestHTTPStatusCodes.cleanUp();
    }

    //---
    // API-A Lite: riSearch
    //---

    @Test
    public void testRISearch_Disabled() throws Exception {
        checkError(RI_SEARCH_PATH);
    }

}
