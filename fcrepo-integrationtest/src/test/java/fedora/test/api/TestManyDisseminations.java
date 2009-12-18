/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.test.api;

import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.common.PID;

import fedora.server.management.FedoraAPIM;

import fedora.test.FedoraServerTestCase;

/**
 * Tests a series of many dissemination requests. NOTE: This test requies API-A
 * to be open (non-authenticating)
 *
 * @author Chris Wilper
 */
public class TestManyDisseminations
        extends FedoraServerTestCase {

    private static final String DATA_OBJECT_PID = "test:ManyDiss";

    private static final String SDEF_OBJECT_PID = "test:ManyDiss-SDef";

    private static final String SDEP_OBJECT_PID = "test:ManyDiss-SDep";

    private static final String CMODEL_OBJECT_PID = "test:ManyDiss-CModel";

    private static final String X_DS = "DC";

    private static final String E_DS = "DC_REF_E";

    private static final String R_DS = "DC_REF_R";

    private static final FedoraClient CLIENT;

    private static final FedoraAPIM APIM;

    private static final String BASE_URL;

    static {
        try {
            BASE_URL = FedoraServerTestCase.getBaseURL();
            CLIENT = FedoraServerTestCase.getFedoraClient();
            APIM = CLIENT.getAPIM();
        } catch (Exception e) {
            throw new RuntimeException("Error getting Fedora Client", e);
        }
    }

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite("TestManyDisseminations TestSuite");
        suite.addTestSuite(TestManyDisseminations.class);
        return new ManyDisseminationsTestSetup(suite);
    }

    //---
    // Tests
    //---

    /**
     * Tests a rapid series of requests for an inline XML datastream.
     */
    public void testManyDatastreamDisseminationsX() throws Exception {
        doDissemTest(DATA_OBJECT_PID + "/" + X_DS, false);
    }

    /**
     * Tests a rapid series of requests for an External datastream.
     */
    public void testManyDatastreamDisseminationsE() throws Exception {
        doDissemTest(DATA_OBJECT_PID + "/" + E_DS, false);
    }

    /**
     * Tests a rapid series of requests for a Redirect datastream.
     */
    public void testManyDatastreamDisseminationsR() throws Exception {
        doDissemTest(DATA_OBJECT_PID + "/" + R_DS, true);
    }

    /**
     * Tests a rapid series of requests for a Saxon dissemination that uses an
     * inline XML datastream.
     */
    public void testManySaxonDisseminationsX() throws Exception {
        doDissemTest(DATA_OBJECT_PID + "/" + SDEF_OBJECT_PID + "/getIDFrom"
                + X_DS, false);
    }

    /**
     * Tests a rapid series of requests for a Saxon dissemination that uses an
     * External datastream.
     */
    public void testManySaxonDisseminationsE() throws Exception {
        doDissemTest(DATA_OBJECT_PID + "/" + SDEF_OBJECT_PID + "/getIDFrom"
                + E_DS, false);
    }

    /**
     * Tests a rapid series of requests for a Saxon dissemination that uses a
     * Redirect datastream.
     */
    public void testManySaxonDisseminationsR() throws Exception {
        doDissemTest(DATA_OBJECT_PID + "/" + SDEF_OBJECT_PID + "/getIDFrom"
                + R_DS, false);
    }

    /**
     * Starts getting a dissemination, then aborts the connection (forcibly
     * closes the socket on the client side) 30 times in a row.
     */
    private void doDissemTest(String what, boolean redirectOK) throws Exception {
        final int num = 30;
        System.out.println("Getting " + what + " " + num + " times...");
        int i = 0;
        try {
            URL url = new URL(BASE_URL + "/get/" + what);
            for (i = 0; i < num; i++) {
                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();
                InputStream in = conn.getInputStream();
                in.read();
                in.close();
                conn.disconnect();
            }
        } catch (Exception e) {
            fail("Dissemination of " + what + " failed on iter " + i + ": "
                    + e.getMessage());
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAuthentication.class);
    }

    public static class ManyDisseminationsTestSetup
            extends TestSetup {

        private static final String CR = System.getProperty("line.separator");

        private static final String FOXML_FORMAT = FOXML1_1.uri;

        private static final String FOXML_NAMESPACE =
                "info:fedora/fedora-system:def/foxml#";

        public ManyDisseminationsTestSetup(Test test)
                throws Exception {
            super(test);
        }

        @Override
        public void setUp() throws Exception {
            APIM.ingest(getCModelObject(), FOXML_FORMAT, "");
            APIM.ingest(getSDefObject(), FOXML_FORMAT, "");
            APIM.ingest(getSDepObject(), FOXML_FORMAT, "");
            APIM.ingest(getDataObject(), FOXML_FORMAT, "");
        }

        @Override
        public void tearDown() throws Exception {
            APIM.purgeObject(DATA_OBJECT_PID, "", false);
            APIM.purgeObject(SDEP_OBJECT_PID, "", false);
            APIM.purgeObject(SDEF_OBJECT_PID, "", false);
            APIM.purgeObject(CMODEL_OBJECT_PID, "", false);
        }

        private static byte[] getCModelObject() throws Exception {
            StringBuilder buf = new StringBuilder();
            openFOXML(buf, CMODEL_OBJECT_PID);

            StringBuilder rdf = new StringBuilder();
            openRDF(rdf, CMODEL_OBJECT_PID);
            appendRel(rdf, MODEL.HAS_MODEL.localName, PID
                    .getInstance(Models.CONTENT_MODEL_3_0.uri).toString());
            appendRel(rdf, MODEL.HAS_SERVICE.localName, SDEF_OBJECT_PID);
            closeRDF(rdf);
            appendInlineDatastream(buf,
                                   "RELS-EXT",
                                   "application/rdf+xml",
                                   Constants.RELS_EXT1_0.uri,
                                   rdf.toString());

            closeFOXML(buf);
            return buf.toString().getBytes("UTF-8");
        }

        private static byte[] getSDefObject() throws Exception {
            StringBuilder buf = new StringBuilder();
            openFOXML(buf, SDEF_OBJECT_PID);
            StringBuilder rdf = new StringBuilder();
            openRDF(rdf, SDEF_OBJECT_PID);
            appendRel(rdf, MODEL.HAS_MODEL.localName, PID
                    .getInstance(Models.SERVICE_DEFINITION_3_0.uri).toString());
            closeRDF(rdf);
            appendInlineDatastream(buf,
                                   "RELS-EXT",
                                   "application/rdf+xml",
                                   Constants.RELS_EXT1_0.uri,
                                   rdf.toString());
            appendInlineDatastream(buf,
                                   "METHODMAP",
                                   "text/xml",
                                   Constants.SDEF_METHOD_MAP1_0.uri,
                                   getSDefMethodMap());
            closeFOXML(buf);
            return buf.toString().getBytes("UTF-8");
        }

        private static byte[] getSDepObject() throws Exception {
            StringBuilder buf = new StringBuilder();
            openFOXML(buf, SDEP_OBJECT_PID);
            appendInlineDatastream(buf,
                                   "METHODMAP",
                                   "text/xml",
                                   Constants.SDEP_METHOD_MAP1_1.uri,
                                   getSDepMethodMap());
            appendInlineDatastream(buf,
                                   "DSINPUTSPEC",
                                   "text/xml",
                                   Constants.DS_INPUT_SPEC1_1.uri,
                                   getDSInputSpec());
            appendInlineDatastream(buf,
                                   "WSDL",
                                   "text/xml",
                                   Constants.WSDL.uri,
                                   getWSDL());

            StringBuilder rdf = new StringBuilder();
            openRDF(rdf, SDEP_OBJECT_PID);
            appendRel(rdf, MODEL.HAS_MODEL.localName, PID
                    .getInstance(Models.SERVICE_DEPLOYMENT_3_0.uri).toString());
            appendRel(rdf, MODEL.IS_DEPLOYMENT_OF.localName, SDEF_OBJECT_PID);
            appendRel(rdf, MODEL.IS_CONTRACTOR_OF.localName, CMODEL_OBJECT_PID);
            closeRDF(rdf);
            appendInlineDatastream(buf,
                                   "RELS-EXT",
                                   "application/rdf+xml",
                                   Constants.RELS_EXT1_0.uri,
                                   rdf.toString());

            closeFOXML(buf);
            return buf.toString().getBytes("UTF-8");
        }

        private static void openRDF(StringBuilder buf, String sPID) {
            buf.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-"
                    + "rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/"
                    + "fedora-system:def/model#\">\n  <rdf:Description "
                    + "rdf:about=\"info:fedora/" + sPID + "\">\n");
        }

        private static void closeRDF(StringBuilder buf) {
            buf.append("  </rdf:Description>\n</rdf:RDF>");
        }

        private static void appendRel(StringBuilder buf,
                                      String pName,
                                      String oPID) {
            buf.append("    <fedora-model:" + pName
                    + " rdf:resource=\"info:fedora/" + oPID + "\"/>\n");
        }

        private static String getWSDL() {
            return "<wsdl:definitions name=\"ManyDissSDep\" targetNamespace=\"bmech\""
                    + CR
                    + "    xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\""
                    + CR
                    + "    xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap\""
                    + CR
                    + "    xmlns:soapenc=\"http://schemas.xmlsoap.org/wsdl/soap/encoding\" xmlns:this=\"bmech\""
                    + CR
                    + "    xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                    + CR
                    + "  <wsdl:types>"
                    + CR
                    + "    <xsd:schema targetNamespace=\"bmech\">"
                    + CR
                    + "      <xsd:simpleType name=\"XSLTType\">"
                    + CR
                    + "        <xsd:restriction base=\"xsd:string\"/>"
                    + CR
                    + "      </xsd:simpleType>"
                    + CR
                    + "      <xsd:simpleType name=\"DCType\">"
                    + CR
                    + "        <xsd:restriction base=\"xsd:string\"/>"
                    + CR
                    + "      </xsd:simpleType>"
                    + CR
                    + "      <xsd:simpleType name=\"DC_REF_EType\">"
                    + CR
                    + "        <xsd:restriction base=\"xsd:string\"/>"
                    + CR
                    + "      </xsd:simpleType>"
                    + CR
                    + "      <xsd:simpleType name=\"DC_REF_RType\">"
                    + CR
                    + "        <xsd:restriction base=\"xsd:string\"/>"
                    + CR
                    + "      </xsd:simpleType>"
                    + CR
                    + "    </xsd:schema>"
                    + CR
                    + "  </wsdl:types>"
                    + CR
                    + "  <wsdl:message name=\"getIDFromDCRequest\">"
                    + CR
                    + "    <wsdl:part name=\"XSLT\" type=\"this:XSLTType\"/>"
                    + CR
                    + "    <wsdl:part name=\"DC\" type=\"this:DCType\"/>"
                    + CR
                    + "  </wsdl:message>"
                    + CR
                    + "  <wsdl:message name=\"getIDFromDC_REF_ERequest\">"
                    + CR
                    + "    <wsdl:part name=\"XSLT\" type=\"this:XSLTType\"/>"
                    + CR
                    + "    <wsdl:part name=\"DC_REF_E\" type=\"this:DC_REF_EType\"/>"
                    + CR
                    + "  </wsdl:message>"
                    + CR
                    + "  <wsdl:message name=\"getIDFromDC_REF_RRequest\">"
                    + CR
                    + "    <wsdl:part name=\"XSLT\" type=\"this:XSLTType\"/>"
                    + CR
                    + "    <wsdl:part name=\"DC_REF_R\" type=\"this:DC_REF_RType\"/>"
                    + CR
                    + "  </wsdl:message>"
                    + CR
                    + "  <wsdl:message name=\"dissemResponse\">"
                    + CR
                    + "    <wsdl:part name=\"dissem\" type=\"xsd:base64Binary\"/>"
                    + CR
                    + "  </wsdl:message>"
                    + CR
                    + "  <wsdl:portType name=\"ManyDissSDepPortType\">"
                    + CR
                    + "    <wsdl:operation name=\"getIDFromDC\">"
                    + CR
                    + "      <wsdl:input message=\"this:getIDFromDCRequest\"/>"
                    + CR
                    + "      <wsdl:output message=\"this:dissemResponse\"/>"
                    + CR
                    + "    </wsdl:operation>"
                    + CR
                    + "    <wsdl:operation name=\"getIDFromDC_REF_E\">"
                    + CR
                    + "      <wsdl:input message=\"this:getIDFromDC_REF_ERequest\"/>"
                    + CR
                    + "      <wsdl:output message=\"this:dissemResponse\"/>"
                    + CR
                    + "    </wsdl:operation>"
                    + CR
                    + "    <wsdl:operation name=\"getIDFromDC_REF_R\">"
                    + CR
                    + "      <wsdl:input message=\"this:getIDFromDC_REF_RRequest\"/>"
                    + CR
                    + "      <wsdl:output message=\"this:dissemResponse\"/>"
                    + CR
                    + "    </wsdl:operation>"
                    + CR
                    + "  </wsdl:portType>"
                    + CR
                    + "  <wsdl:service name=\"ManyDissSDep\">"
                    + CR
                    + "    <wsdl:port binding=\"this:ManyDissSDep_http\" name=\"ManyDissSDep_port\">"
                    + CR
                    + "      <http:address location=\"http://local.fedora.server/saxon/\"/>"
                    + CR
                    + "    </wsdl:port>"
                    + CR
                    + "  </wsdl:service>"
                    + CR
                    + "  <wsdl:binding name=\"ManyDissSDep_http\" type=\"this:ManyDissSDepPortType\">"
                    + CR
                    + "    <http:binding verb=\"GET\"/>"
                    + CR
                    + "    <wsdl:operation name=\"getIDFromDC\">"
                    + CR
                    + "      <http:operation location=\"SaxonServlet?clear-stylesheet-cache=true&amp;source=(DC)&amp;style=(XSLT)\"/>"
                    + CR
                    + "      <wsdl:input>"
                    + CR
                    + "        <http:urlReplacement/>"
                    + CR
                    + "      </wsdl:input>"
                    + CR
                    + "      <wsdl:output>"
                    + CR
                    + "        <mime:content type=\"text/xml\"/>"
                    + CR
                    + "      </wsdl:output>"
                    + CR
                    + "    </wsdl:operation>"
                    + CR
                    + "    <wsdl:operation name=\"getIDFromDC_REF_E\">"
                    + CR
                    + "      <http:operation location=\"SaxonServlet?clear-stylesheet-cache=true&amp;source=(DC_REF_E)&amp;style=(XSLT)\"/>"
                    + CR
                    + "      <wsdl:input>"
                    + CR
                    + "        <http:urlReplacement/>"
                    + CR
                    + "      </wsdl:input>"
                    + CR
                    + "      <wsdl:output>"
                    + CR
                    + "        <mime:content type=\"text/xml\"/>"
                    + CR
                    + "      </wsdl:output>"
                    + CR
                    + "    </wsdl:operation>"
                    + CR
                    + "    <wsdl:operation name=\"getIDFromDC_REF_R\">"
                    + CR
                    + "      <http:operation location=\"SaxonServlet?clear-stylesheet-cache=true&amp;source=(DC_REF_R)&amp;style=(XSLT)\"/>"
                    + CR
                    + "      <wsdl:input>"
                    + CR
                    + "        <http:urlReplacement/>"
                    + CR
                    + "      </wsdl:input>"
                    + CR
                    + "      <wsdl:output>"
                    + CR
                    + "        <mime:content type=\"text/xml\"/>"
                    + CR
                    + "      </wsdl:output>"
                    + CR
                    + "    </wsdl:operation>"
                    + CR + "  </wsdl:binding>" + CR + "</wsdl:definitions>";
        }

        private static String getDSInputSpec() {
            return "<fbs:DSInputSpec "
                    + "  label=\"label\" xmlns:fbs=\"http://fedora.comm.nsdlib.org/service/bindspec\">"
                    + CR
                    + "  <fbs:DSInput DSMax=\"1\" DSMin=\"1\" DSOrdinality=\"false\" wsdlMsgPartName=\"XSLT\">"
                    + CR
                    + "    <fbs:DSInputLabel>XSLT Binding</fbs:DSInputLabel>"
                    + CR
                    + "    <fbs:DSMIME>text/xml</fbs:DSMIME>"
                    + CR
                    + "    <fbs:DSInputInstruction/>"
                    + CR
                    + "  </fbs:DSInput>"
                    + CR
                    + "  <fbs:DSInput DSMax=\"1\" DSMin=\"1\" DSOrdinality=\"false\" wsdlMsgPartName=\"DC\">"
                    + CR
                    + "    <fbs:DSInputLabel>DC Binding</fbs:DSInputLabel>"
                    + CR
                    + "    <fbs:DSMIME>text/xml</fbs:DSMIME>"
                    + CR
                    + "    <fbs:DSInputInstruction/>"
                    + CR
                    + "  </fbs:DSInput>"
                    + CR
                    + "  <fbs:DSInput DSMax=\"1\" DSMin=\"1\" DSOrdinality=\"false\" wsdlMsgPartName=\"DC_REF_E\">"
                    + CR
                    + "    <fbs:DSInputLabel>DC_REF_E Binding</fbs:DSInputLabel>"
                    + CR
                    + "    <fbs:DSMIME>text/xml</fbs:DSMIME>"
                    + CR
                    + "    <fbs:DSInputInstruction/>"
                    + CR
                    + "  </fbs:DSInput>"
                    + CR
                    + "  <fbs:DSInput DSMax=\"1\" DSMin=\"1\" DSOrdinality=\"false\" wsdlMsgPartName=\"DC_REF_R\">"
                    + CR
                    + "    <fbs:DSInputLabel>DC_REF_R Binding</fbs:DSInputLabel>"
                    + CR
                    + "    <fbs:DSMIME>text/xml</fbs:DSMIME>"
                    + CR
                    + "    <fbs:DSInputInstruction/>"
                    + CR
                    + "  </fbs:DSInput>"
                    + CR + "</fbs:DSInputSpec>";
        }

        private static String getSDepMethodMap() {
            return "<fmm:MethodMap bDefPID=\""
                    + SDEF_OBJECT_PID
                    + "\" name=\"name\" xmlns:fmm=\"http://fedora.comm.nsdlib.org/service/methodmap\">"
                    + CR
                    + "  <fmm:Method operationLabel=\"label\" operationName=\"getIDFromDC\" wsdlMsgName=\"getIDFromDCRequest\" wsdlMsgOutput=\"dissemResponse\">"
                    + CR
                    + "    <fmm:DatastreamInputParm defaultValue=\"\" label=\"\" parmName=\"XSLT\" passBy=\"URL_REF\" required=\"true\"/>"
                    + CR
                    + "    <fmm:DatastreamInputParm defaultValue=\"\" label=\"\" parmName=\"DC\" passBy=\"URL_REF\" required=\"true\"/>"
                    + CR
                    + "    <fmm:MethodReturnType wsdlMsgName=\"dissemResponse\" wsdlMsgTOMIME=\"text/xml\"/>"
                    + CR
                    + "  </fmm:Method>"
                    + CR
                    + "  <fmm:Method operationLabel=\"label\" operationName=\"getIDFromDC_REF_E\" wsdlMsgName=\"getIDFromDC_REF_ERequest\" wsdlMsgOutput=\"dissemResponse\">"
                    + CR
                    + "    <fmm:DatastreamInputParm defaultValue=\"\" label=\"\" parmName=\"XSLT\" passBy=\"URL_REF\" required=\"true\"/>"
                    + CR
                    + "    <fmm:DatastreamInputParm defaultValue=\"\" label=\"\" parmName=\"DC_REF_E\" passBy=\"URL_REF\" required=\"true\"/>"
                    + CR
                    + "    <fmm:MethodReturnType wsdlMsgName=\"dissemResponse\" wsdlMsgTOMIME=\"text/xml\"/>"
                    + CR
                    + "  </fmm:Method>"
                    + CR
                    + "  <fmm:Method operationLabel=\"label\" operationName=\"getIDFromDC_REF_R\" wsdlMsgName=\"getIDFromDC_REF_RRequest\" wsdlMsgOutput=\"dissemResponse\">"
                    + CR
                    + "    <fmm:DatastreamInputParm defaultValue=\"\" label=\"\" parmName=\"XSLT\" passBy=\"URL_REF\" required=\"true\"/>"
                    + CR
                    + "    <fmm:DatastreamInputParm defaultValue=\"\" label=\"\" parmName=\"DC_REF_R\" passBy=\"URL_REF\" required=\"true\"/>"
                    + CR
                    + "    <fmm:MethodReturnType wsdlMsgName=\"dissemResponse\" wsdlMsgTOMIME=\"text/xml\"/>"
                    + CR + "  </fmm:Method>" + CR + "</fmm:MethodMap>";
        }

        private static String getSDefMethodMap() {
            return "<fmm:MethodMap name=\"name\" xmlns:fmm=\"http://fedora.comm.nsdlib.org/service/methodmap\">"
                    + CR
                    + "  <fmm:Method label=\"label\" operationName=\"getIDFromDC\"/>"
                    + CR
                    + "  <fmm:Method label=\"label\" operationName=\"getIDFromDC_REF_E\"/>"
                    + CR
                    + "  <fmm:Method label=\"label\" operationName=\"getIDFromDC_REF_R\"/>"
                    + CR + "</fmm:MethodMap>" + CR;
        }

        private static byte[] getDataObject() throws Exception {
            StringBuilder buf = new StringBuilder();
            openFOXML(buf, DATA_OBJECT_PID);
            final String mimeType = "text/xml";
            InetAddress addr = InetAddress.getLocalHost();
            final String url =
                    "http://" + addr.getHostAddress() + ":"
                            + FedoraServerTestCase.getPort() + "/"
                            + FedoraServerTestCase.getFedoraAppServerContext()
                            + "/get/" + DATA_OBJECT_PID + "/" + X_DS;
            appendInlineDatastream(buf,
                                   X_DS,
                                   "text/xml",
                                   Constants.OAI_DC2_0.uri,
                                   getDC());
            appendInlineDatastream(buf, "XSLT", "text/xml", null, getXSLT());
            appendRemoteDatastream(buf, E_DS, "E", mimeType, url);
            appendRemoteDatastream(buf, R_DS, "R", mimeType, url);

            StringBuilder rdf = new StringBuilder();
            openRDF(rdf, DATA_OBJECT_PID);
            appendRel(rdf, MODEL.HAS_MODEL.localName, PID
                    .getInstance(Models.FEDORA_OBJECT_3_0.uri).toString());
            appendRel(rdf, MODEL.HAS_MODEL.localName, CMODEL_OBJECT_PID);
            closeRDF(rdf);
            appendInlineDatastream(buf,
                                   "RELS-EXT",
                                   "application/rdf+xml",
                                   Constants.RELS_EXT1_0.uri,
                                   rdf.toString());

            closeFOXML(buf);
            return buf.toString().getBytes("UTF-8");
        }

        private static String getDC() {
            return "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">"
                    + CR
                    + "  <dc:identifier>test:ManyDiss</dc:identifier>"
                    + CR + "</oai_dc:dc>";
        }

        private static String getXSLT() {
            StringBuilder buf = new StringBuilder();
            buf
                    .append("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
                            + CR
                            + "  <xsl:output encoding=\"UTF-8\" indent=\"yes\" method=\"xml\"/>"
                            + CR
                            + "  <xsl:template match=\"dc:identifier\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
                            + CR);
            for (int i = 0; i < 5000; i++) {
                buf.append("    <identifier>" + CR);
                buf.append("      <xsl:value-of select=\"text()\"/>" + CR);
                buf.append("    </identifier>" + CR);
            }
            buf.append("  </xsl:template>" + CR + "</xsl:stylesheet>");
            return buf.toString();
        }

        private static void openFOXML(StringBuilder buf, String pid) {
            buf.append("<foxml:digitalObject");
            buf.append(" VERSION=\"1.1\"");
            buf.append(" PID=\"" + pid + "\"");
            buf.append(" xmlns:foxml=\"" + FOXML_NAMESPACE + "\"");
            buf.append(">" + CR + "<foxml:objectProperties>" + CR);
            buf.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/>");
            buf.append(CR + "</foxml:objectProperties>" + CR);
        }

        private static void appendInlineDatastream(StringBuilder buf,
                                                   String dsID,
                                                   String mimeType,
                                                   String formatURI,
                                                   String xml) {
            openDatastream(buf, dsID, "X");
            openDatastreamVersion(buf, dsID + ".0", mimeType, formatURI);
            buf.append("      <foxml:xmlContent>" + CR);
            buf.append(xml);
            buf.append(CR + "      </foxml:xmlContent>" + CR);
            closeDatastreamVersion(buf);
            closeDatastream(buf);
        }

        private static void appendRemoteDatastream(StringBuilder buf,
                                                   String dsID,
                                                   String controlGroup,
                                                   String mimeType,
                                                   String location) {
            openDatastream(buf, dsID, controlGroup);
            openDatastreamVersion(buf, dsID + ".0", mimeType, null);
            buf.append("      <foxml:contentLocation REF=\"" + location
                    + "\" TYPE=\"URL\"/>" + CR);
            closeDatastreamVersion(buf);
            closeDatastream(buf);
        }

        private static void openDatastream(StringBuilder buf,
                                           String dsID,
                                           String controlGroup) {
            buf.append("  <foxml:datastream");
            buf.append(" ID=\"" + dsID + "\"");
            buf.append(" CONTROL_GROUP=\"" + controlGroup + "\"");
            buf.append(">" + CR);
        }

        private static void openDatastreamVersion(StringBuilder buf,
                                                  String versionID,
                                                  String mimeType,
                                                  String formatURI) {
            buf.append("    <foxml:datastreamVersion");
            buf.append(" ID=\"" + versionID + "\"");
            buf.append(" MIMETYPE=\"" + mimeType + "\"");
            if (formatURI != null) {
                buf.append(" FORMAT_URI=\"" + formatURI + "\"");
            }
            buf.append(">" + CR);
        }

        private static void closeDatastreamVersion(StringBuilder buf) {
            buf.append("    </foxml:datastreamVersion>" + CR);
        }

        private static void closeDatastream(StringBuilder buf) {
            buf.append("  </foxml:datastream>" + CR);
        }

        private static void closeFOXML(StringBuilder buf) {
            buf.append("</foxml:digitalObject>");
        }
    }

}
