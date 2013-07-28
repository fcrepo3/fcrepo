/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static junit.framework.Assert.assertEquals;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import junit.framework.JUnit4TestAdapter;

import org.apache.http.entity.StringEntity;
import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.PID;

import org.fcrepo.server.management.FedoraAPIMMTOM;

import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * Tests of the REST API based on the config Q settings.
 * Tests assume a running instance of Fedora.
 * //TODO: actually validate the ResponseBody
 * instead of just HTTP status codes
 *
 * @author Bill Branan
 * @version $Id$
 * @since 3.0
 */
public class TestRESTAPIConfigQ
        extends FedoraServerTestCase {
    
    private static FedoraAPIMMTOM apim;

    private static String DEMO_FOXML;

    private static final PID pid = PID.getInstance("demo:RESTQ");

    private static TestRESTAPI rest;

    static {
        // Test minimal FOXML object
        StringBuilder sb = new StringBuilder();
        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" PID=\"" + pid.toString() + "\" ");
        sb.append("  xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" ");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        sb.append("  xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# ");
        sb.append("  http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("</foxml:digitalObject>");

        DEMO_FOXML = sb.toString();
    }
    
    @BeforeClass
    public static void bootStrap() throws Exception {
        apim = TestRESTAPI.initClient().getAPIMMTOM();
        rest = new TestRESTAPI();
    }
    
    @AfterClass
    public static void cleanUp() {
        TestRESTAPI.stopClient();
    }

    @Test
    public void testFindObjects() throws Exception {
        URI url = TestRESTAPI.getURI(String.format("/objects?pid=true&terms=&query=&resultFormat=xml"));
        rest.verifyGETStatusOnly(url, SC_OK, false);
    }

    @Test
    public void testIngest() throws Exception {
        String label = "Label";
        URI url = TestRESTAPI.getURI(String.format("/objects/%s?label=%s", pid, label));
        StringEntity entity = TestRESTAPI.getStringEntity(DEMO_FOXML, "text/xml");
        rest.verifyPOSTStatusOnly(url, SC_UNAUTHORIZED, entity, false);

        // Make sure the object was not ingested
        url = TestRESTAPI.getURI(String.format("/objects/%s", pid));
        rest.verifyGETStatusOnly(url, SC_NOT_FOUND, false);

        url = TestRESTAPI.getURI(String.format("/objects/%s?label=%s", pid, label));
        rest.verifyPOSTStatusOnly(url, SC_CREATED, entity, true);

        // Make sure the object was ingested
        url = TestRESTAPI.getURI(String.format("/objects/%s", pid));
        rest.verifyGETStatusOnly(url, SC_OK, false, false);

        apim.purgeObject(pid.toString(), "", false);
    }
    
    // Supports legacy test runners

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestRESTAPIConfigQ.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestRESTAPIConfigQ.class);
    }

}
