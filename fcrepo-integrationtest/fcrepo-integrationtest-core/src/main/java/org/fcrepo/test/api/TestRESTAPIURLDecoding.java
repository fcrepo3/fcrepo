/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static org.apache.http.HttpStatus.SC_CREATED;

import java.net.URI;

import junit.framework.JUnit4TestAdapter;

import org.apache.http.entity.StringEntity;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import static org.apache.http.HttpStatus.SC_OK;

public class TestRESTAPIURLDecoding
        extends FedoraServerTestCase {

    private static final String pid = "test:SomeObject";
    private static final String pidWithEscapedColon1 = "test%3ASomeObject";
    private static final String pidWithEscapedColon2 = "test%3aSomeObject";

    private static TestRESTAPI rest;

    private static String getFOXML(String pid) {
        // Test minimal FOXML object
        StringBuilder sb = new StringBuilder();
        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" PID=\"" + pid + "\" ");
        sb.append("  xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" ");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        sb.append("  xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# ");
        sb.append("  http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("</foxml:digitalObject>");
        return sb.toString();
    }
    
    @Before
    public void setUp() throws Exception {
        URI url = TestRESTAPI.getURI(String.format("/objects/%s", pid));
        StringEntity entity = TestRESTAPI.getStringEntity(getFOXML(pid), "text/xml");
        TestRESTAPI.initClient();
        rest = new TestRESTAPI();
        rest.verifyPOSTStatusOnly(url, SC_CREATED, entity, true);
    }
    
    @After
    public void tearDown() throws Exception {
        URI url = TestRESTAPI.getURI(String.format("/objects/%s", pid));
        rest.verifyDELETEStatusOnly(url, SC_OK, true);
        TestRESTAPI.stopClient();
    }

    @Test
    public void testGetObjectProfile() throws Exception {
        URI url = TestRESTAPI.getURI("/objects/" + pid);
        rest.verifyGETStatusOnly(url, SC_OK, true);
        url = TestRESTAPI.getURI("/objects/" + pidWithEscapedColon1);
        rest.verifyGETStatusOnly(url, SC_OK, true);
        url = TestRESTAPI.getURI("/objects/" + pidWithEscapedColon2);
        rest.verifyGETStatusOnly(url, SC_OK, true);
    }
    
    // Supports legacy test runners

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestRESTAPIURLDecoding.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestRESTAPIURLDecoding.class);
    }
}
