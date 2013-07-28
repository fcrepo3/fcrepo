/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import org.fcrepo.test.FedoraServerTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

import static org.fcrepo.test.api.TestHTTPStatusCodes.DESCRIBE_REPOSITORY_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.FIND_OBJECTS_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.GET_CUSTOM_DISSEM_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.GET_DEFAULT_DISSEM_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.GET_DS_DISSEM_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.GET_OBJ_HISTORY_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.GET_OBJ_PROFILE_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.LIST_DATASTREAMS_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.LIST_METHODS_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.RI_SEARCH_PATH;
import static org.fcrepo.test.api.TestHTTPStatusCodes.checkBadAuthN;
import static org.fcrepo.test.api.TestHTTPStatusCodes.checkBadAuthZ;
import static org.fcrepo.test.api.TestHTTPStatusCodes.checkOK;


/**
 * HTTP status code tests to be run when API-A authentication is on.
 *
 * @author Chris Wilper
 */
public class TestHTTPStatusCodesConfigB
        extends FedoraServerTestCase {

    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite("TestHTTPStatusCodesConfigB TestSuite");
        suite.addTest(new JUnit4TestAdapter(TestHTTPStatusCodesConfigB.class));
        suite.addTest(org.fcrepo.test.api.TestHTTPStatusCodes.suite());
        return suite;
    }
    
    @BeforeClass
    public static void bootstrap() throws Exception {
        TestHTTPStatusCodes.bootstrap();
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        TestHTTPStatusCodes.cleanUp();
    }

    //---
    // API-A Lite: describeRepository
    //---

    @Test
    public void testDescribeRepository_BadAuthN() throws Exception {
        checkBadAuthN(DESCRIBE_REPOSITORY_PATH);
    }

    @Test
    public void testDescribeRepository_BadAuthZ() throws Exception {
        checkBadAuthZ(DESCRIBE_REPOSITORY_PATH);
    }

    //---
    // API-A Lite: getDatastreamDissemination
    //---

    @Test
    public void testGetDatastreamDissemination_BadAuthN() throws Exception {
        checkBadAuthN(GET_DS_DISSEM_PATH);
    }

    @Test
    public void testGetDatastreamDissemination_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_DS_DISSEM_PATH);
    }

    //---
    // API-A Lite: getDissemination (default)
    //---

    @Test
    public void testGetDissemination_Default_BadAuthN() throws Exception {
        checkBadAuthN(GET_DEFAULT_DISSEM_PATH);
    }

    @Test
    public void testGetDissemination_Default_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_DEFAULT_DISSEM_PATH);
    }

    //---
    // API-A Lite: getDissemination (custom)
    //---

    @Test
    public void testGetDissemination_Custom_BadAuthN() throws Exception {
        checkBadAuthN(GET_CUSTOM_DISSEM_PATH);
    }

    @Test
    public void testGetDissemination_Custom_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_CUSTOM_DISSEM_PATH);
    }

    //---
    // API-A Lite: getObjectHistory
    //---

    @Test
    public void testGetObjectHistory_BadAuthN() throws Exception {
        checkBadAuthN(GET_OBJ_HISTORY_PATH);
    }

    @Test
    public void testGetObjectHistory_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_OBJ_HISTORY_PATH);
    }

    //---
    // API-A Lite: getObjectProfile
    //---

    @Test
    public void testGetObjectProfile_BadAuthN() throws Exception {
        checkBadAuthN(GET_OBJ_PROFILE_PATH);
    }

    @Test
    public void testGetObjectProfile_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_OBJ_PROFILE_PATH);
    }

    //---
    // API-A Lite: listDatastreams
    //---

    @Test
    public void testListDatastreams_BadAuthN() throws Exception {
        checkBadAuthN(LIST_DATASTREAMS_PATH);
    }

    @Test
    public void testListDatastreams_BadAuthZ() throws Exception {
        checkBadAuthZ(LIST_DATASTREAMS_PATH);
    }

    //---
    // API-A Lite: listMethods
    //---

    @Test
    public void testListMethods_BadAuthN() throws Exception {
        checkBadAuthN(LIST_METHODS_PATH);
    }

    @Test
    public void testListMethods_BadAuthZ() throws Exception {
        checkBadAuthZ(LIST_METHODS_PATH);
    }

    //---
    // API-A Lite: findObjects
    //---

    @Test
    public void testFindObjects_BadAuthN() throws Exception {
        checkBadAuthN(FIND_OBJECTS_PATH);
    }

    @Test
    public void testFindObjects_BadAuthZ() throws Exception {
        checkBadAuthZ(FIND_OBJECTS_PATH);
    }

    //---
    // API-A Lite: riSearch
    //---

    @Test
    public void testRISearch_OK() throws Exception {
        checkOK(RI_SEARCH_PATH);
    }

    @Test
    public void testRISearch_BadAuthN() throws Exception {
        checkBadAuthN(RI_SEARCH_PATH);
    }

    @Test
    public void testRISearch_BadAuthZ() throws Exception {
        checkBadAuthZ(RI_SEARCH_PATH);
    }

}