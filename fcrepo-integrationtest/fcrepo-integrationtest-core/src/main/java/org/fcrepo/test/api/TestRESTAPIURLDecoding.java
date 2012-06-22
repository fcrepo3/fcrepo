/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import org.fcrepo.test.FedoraServerTestCase;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;

public class TestRESTAPIURLDecoding
        extends FedoraServerTestCase {

    private static final String pid = "test:SomeObject";
    private static final String pidWithEscapedColon1 = "test%3ASomeObject";
    private static final String pidWithEscapedColon2 = "test%3aSomeObject";

    private static TestRESTAPI rest = new TestRESTAPI();

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
    
    @Override
    public void setUp() throws Exception {
        rest.url = String.format("/objects/%s", pid);
        rest.post(getFOXML(pid), true);
    }
    
    @Override
    public void tearDown() throws Exception {
        rest.url = String.format("/objects/%s", pid);
        rest.delete(true);
    }

    public void testGetObjectProfile() throws Exception {
        getAndExpect("/objects/" + pid, SC_OK);
        getAndExpect("/objects/" + pidWithEscapedColon1, SC_OK);
        getAndExpect("/objects/" + pidWithEscapedColon2, SC_OK);
    }
    
    private void getAndExpect(String url, int statusCode) throws Exception {
        rest.url = url;
        assertEquals(SC_OK, rest.get(true).getStatusCode());
    }
}
