/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import fedora.common.PID;

import fedora.server.management.FedoraAPIM;

import fedora.test.FedoraServerTestCase;

import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;

/**
 * Tests of the REST API based on the config Q settings.
 * Tests assume a running instance of Fedora.
 * //TODO: actually validate the ResponseBody
 * instead of just HTTP status codes
 *
 * @author Bill Branan
 * @since 3.0
 * @version $Id$
 */
public class TestRESTAPIConfigQ
        extends FedoraServerTestCase {

    private FedoraAPIM apim;

    private static String DEMO_FOXML;

    private static final PID pid = PID.getInstance("demo:RESTQ");

    private static TestRESTAPI rest = new TestRESTAPI();

    static {
        // Test minimal FOXML object
        StringBuilder sb = new StringBuilder();
        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" PID=\""+pid.toString()+"\" ");
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

    @Override
    public void setUp() throws Exception {
        apim = getFedoraClient().getAPIM();
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testFindObjects() throws Exception {
        rest.url = String.format("/objects?pid=true&terms=&query=&resultFormat=xml");
        assertEquals(SC_OK, rest.get(false).getStatusCode());
    }

    public void testIngest() throws Exception {
        String label = "Label";
        rest.url = String.format("/objects/%s?label=%s", pid, label);
        assertEquals(SC_UNAUTHORIZED, rest.post(DEMO_FOXML, false).getStatusCode());

        // Make sure the object was not ingested
        rest.url = String.format("/objects/%s", pid);
        assertEquals(SC_NOT_FOUND, rest.get(false).getStatusCode());

        rest.url = String.format("/objects/%s?label=%s", pid, label);
        assertEquals(SC_CREATED, rest.post(DEMO_FOXML, true).getStatusCode());

        // Make sure the object was ingested
        rest.url = String.format("/objects/%s", pid);
        assertEquals(SC_OK, rest.get(false).getStatusCode());

        apim.purgeObject(pid.toString(), "", false);
    }

}
