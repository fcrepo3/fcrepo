/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;

import static fedora.test.api.TestHTTPStatusCodes.DESCRIBE_REPOSITORY_PATH;
import static fedora.test.api.TestHTTPStatusCodes.FIND_OBJECTS_PATH;
import static fedora.test.api.TestHTTPStatusCodes.GET_CUSTOM_DISSEM_PATH;
import static fedora.test.api.TestHTTPStatusCodes.GET_DEFAULT_DISSEM_PATH;
import static fedora.test.api.TestHTTPStatusCodes.GET_DS_DISSEM_PATH;
import static fedora.test.api.TestHTTPStatusCodes.GET_OBJ_HISTORY_PATH;
import static fedora.test.api.TestHTTPStatusCodes.GET_OBJ_PROFILE_PATH;
import static fedora.test.api.TestHTTPStatusCodes.LIST_DATASTREAMS_PATH;
import static fedora.test.api.TestHTTPStatusCodes.LIST_METHODS_PATH;
import static fedora.test.api.TestHTTPStatusCodes.RI_SEARCH_PATH;
import static fedora.test.api.TestHTTPStatusCodes.checkBadAuthN;
import static fedora.test.api.TestHTTPStatusCodes.checkBadAuthZ;
import static fedora.test.api.TestHTTPStatusCodes.checkOK;

/**
 * HTTP status code tests to be run when API-A authentication is on.
 *
 * @author Chris Wilper
 */
public class TestHTTPStatusCodesConfigB
        extends FedoraServerTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("TestHTTPStatusCodesConfigB TestSuite");
        suite.addTestSuite(TestHTTPStatusCodesConfigB.class);
        suite.addTest(fedora.test.api.TestHTTPStatusCodes.suite());
        return new DemoObjectTestSetup(suite);
    }

    //---
    // API-A Lite: describeRepository
    //---

    public void testDescribeRepository_BadAuthN() throws Exception {
        checkBadAuthN(DESCRIBE_REPOSITORY_PATH);
    }

    public void testDescribeRepository_BadAuthZ() throws Exception {
        checkBadAuthZ(DESCRIBE_REPOSITORY_PATH);
    }

    //---
    // API-A Lite: getDatastreamDissemination
    //---

    public void testGetDatastreamDissemination_BadAuthN() throws Exception {
        checkBadAuthN(GET_DS_DISSEM_PATH);
    }

    public void testGetDatastreamDissemination_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_DS_DISSEM_PATH);
    }

    //---
    // API-A Lite: getDissemination (default)
    //---

    public void testGetDissemination_Default_BadAuthN() throws Exception {
        checkBadAuthN(GET_DEFAULT_DISSEM_PATH);
    }

    public void testGetDissemination_Default_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_DEFAULT_DISSEM_PATH);
    }

    //---
    // API-A Lite: getDissemination (custom)
    //---

    public void testGetDissemination_Custom_BadAuthN() throws Exception {
        checkBadAuthN(GET_CUSTOM_DISSEM_PATH);
    }

    public void testGetDissemination_Custom_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_CUSTOM_DISSEM_PATH);
    }

    //---
    // API-A Lite: getObjectHistory
    //---

    public void testGetObjectHistory_BadAuthN() throws Exception {
        checkBadAuthN(GET_OBJ_HISTORY_PATH);
    }

    public void testGetObjectHistory_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_OBJ_HISTORY_PATH);
    }

    //---
    // API-A Lite: getObjectProfile
    //---

    public void testGetObjectProfile_BadAuthN() throws Exception {
        checkBadAuthN(GET_OBJ_PROFILE_PATH);
    }

    public void testGetObjectProfile_BadAuthZ() throws Exception {
        checkBadAuthZ(GET_OBJ_PROFILE_PATH);
    }

    //---
    // API-A Lite: listDatastreams
    //---

    public void testListDatastreams_BadAuthN() throws Exception {
        checkBadAuthN(LIST_DATASTREAMS_PATH);
    }

    public void testListDatastreams_BadAuthZ() throws Exception {
        checkBadAuthZ(LIST_DATASTREAMS_PATH);
    }

    //---
    // API-A Lite: listMethods
    //---

    public void testListMethods_BadAuthN() throws Exception {
        checkBadAuthN(LIST_METHODS_PATH);
    }

    public void testListMethods_BadAuthZ() throws Exception {
        checkBadAuthZ(LIST_METHODS_PATH);
    }

    //---
    // API-A Lite: findObjects
    //---

    public void testFindObjects_BadAuthN() throws Exception {
        checkBadAuthN(FIND_OBJECTS_PATH);
    }

    public void testFindObjects_BadAuthZ() throws Exception {
        checkBadAuthZ(FIND_OBJECTS_PATH);
    }

    //---
    // API-A Lite: riSearch
    //---

    public void testRISearch_OK() throws Exception {
        checkOK(RI_SEARCH_PATH);
    }

    public void testRISearch_BadAuthN() throws Exception {
        checkBadAuthN(RI_SEARCH_PATH);
    }

    public void testRISearch_BadAuthZ() throws Exception {
        checkBadAuthZ(RI_SEARCH_PATH);
    }

}
