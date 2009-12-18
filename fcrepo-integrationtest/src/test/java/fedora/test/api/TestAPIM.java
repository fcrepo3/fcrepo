/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.axis.AxisFault;
import org.apache.axis.types.NonNegativeInteger;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.common.Constants;

import fedora.server.management.FedoraAPIM;
import fedora.server.types.gen.Datastream;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;

public class TestAPIM
        extends FedoraServerTestCase
        implements Constants {

    private FedoraAPIM apim;

    public static byte[] dsXML;

    public static byte[] demo997FOXML10ObjectXML;

    public static byte[] demo998FOXMLObjectXML;

    public static byte[] demo999METSObjectXML;

    public static byte[] demo999bMETS10ObjectXML;

    public static byte[] demo1000ATOMObjectXML;

    public static byte[] demo1001ATOMZip;

    public static byte[] demo1001_relsext;

    public static byte[] changeme1FOXMLObjectXML;

    public static byte[] changeme2METSObjectXML;

    static {

        // create test xml datastream
        StringBuffer sb = new StringBuffer();
        sb
                .append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">");
        sb.append("<dc:title>Dublin Core Record 5</dc:title>");
        sb.append("<dc:creator>Author 5</dc:creator>");
        sb.append("<dc:subject>Subject 5</dc:subject>");
        sb.append("<dc:description>Description 5</dc:description>");
        sb.append("<dc:publisher>Publisher 5</dc:publisher>");
        sb.append("<dc:format>MIME type 5</dc:format>");
        sb.append("<dc:identifier>Identifier 5</dc:identifier>");
        sb.append("</oai_dc:dc>");
        try {
            dsXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // create test FOXML 1.0 object specifying pid=demo:997
        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject PID=\"demo:997\" xmlns:METS=\"http://www.loc.gov/METS/\" xmlns:audit=\"info:fedora/fedora-system:def/audit#\" xmlns:fedoraAudit=\"http://fedora.comm.nsdlib.org/audit\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" xmlns:uvalibadmin=\"http://dl.lib.virginia.edu/bin/dtd/admin/admin.dtd\" xmlns:uvalibdesc=\"http://dl.lib.virginia.edu/bin/dtd/descmeta/descmeta.dtd\" xmlns:xlink=\"http://www.w3.org/TR/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-0.xsd\">");
        sb.append("<foxml:objectProperties>");
        sb.append("<foxml:property NAME=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\" VALUE=\"FedoraObject\"/>");
        sb.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
        sb.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"Data Object (Coliseum) for Local Simple Image Demo\"/>");
        sb.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#contentModel\" VALUE=\"UVA_STD_IMAGE\"/>");
        sb.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/>");
        sb.append("</foxml:objectProperties>");
        sb.append("<foxml:datastream ID=\"DC\" CONTROL_GROUP=\"X\" STATE=\"A\">");
        sb.append("<foxml:datastreamVersion ID=\"DC1.0\" MIMETYPE=\"text/xml\" LABEL=\"Dublin Core Record for this object\">");
        sb.append("<foxml:xmlContent>");
        sb.append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">");
        sb.append("<dc:title>Coliseum in Rome</dc:title>");
        sb.append("<dc:creator>Thornton Staples</dc:creator>");
        sb.append("<dc:subject>Architecture, Roman</dc:subject>");
        sb.append("<dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("<dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("<dc:format>image/jpeg</dc:format>");
        sb.append("<dc:identifier>demo:5</dc:identifier>");
        sb.append("</oai_dc:dc>");
        sb.append("</foxml:xmlContent>");
        sb.append("</foxml:datastreamVersion>");
        sb.append("</foxml:datastream>");
        sb.append("<foxml:datastream ID=\"DS1\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("<foxml:datastreamVersion ID=\"DS1.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum thumbnail jpg image\">");
        sb.append("<foxml:contentLocation REF=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" TYPE=\"URL\"/>");
        sb.append("</foxml:datastreamVersion>");
        sb.append("</foxml:datastream>");
        sb.append("<foxml:datastream ID=\"DS2\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("<foxml:datastreamVersion ID=\"DS2.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum medium jpg image\">");
        sb.append("<foxml:contentLocation REF=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\" TYPE=\"URL\"/>");
        sb.append("</foxml:datastreamVersion>");
        sb.append("</foxml:datastream>");
        sb.append("<foxml:datastream ID=\"DS3\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("<foxml:datastreamVersion ID=\"DS3.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum high jpg image\">");
        sb.append("<foxml:contentLocation REF=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-high.jpg\" TYPE=\"URL\"/>");
        sb.append("</foxml:datastreamVersion>");
        sb.append("</foxml:datastream>");
        sb.append("<foxml:datastream ID=\"DS4\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("<foxml:datastreamVersion ID=\"DS4.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum veryhigh jpg image\">");
        sb.append("<foxml:contentLocation REF=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" TYPE=\"URL\"/>");
        sb.append("</foxml:datastreamVersion>");
        sb.append("</foxml:datastream>");
        sb.append("<foxml:disseminator ID=\"DISS1\" BDEF_CONTRACT_PID=\"demo:1\" STATE=\"A\">");
        sb.append("<foxml:disseminatorVersion ID=\"DISS1.0\" BMECH_SERVICE_PID=\"demo:2\" LABEL=\"UVA Simple Image Behaviors\">");
        sb.append("<foxml:serviceInputMap>");
        sb.append("<foxml:datastreamBinding DATASTREAM_ID=\"DS1\" KEY=\"THUMBRES_IMG\" LABEL=\"Binding to thumbnail photo of Coliseum\"/>");
        sb.append("<foxml:datastreamBinding DATASTREAM_ID=\"DS2\" KEY=\"MEDRES_IMG\" LABEL=\"Binding to medium resolution photo of Coliseum\"/>");
        sb.append("<foxml:datastreamBinding DATASTREAM_ID=\"DS3\" KEY=\"HIGHRES_IMG\" LABEL=\"Binding to high resolution photo of Coliseum\"/>");
        sb.append("<foxml:datastreamBinding DATASTREAM_ID=\"DS4\" KEY=\"VERYHIGHRES_IMG\" LABEL=\"Binding to very high resolution photo of Coliseum\"/>");
        sb.append("</foxml:serviceInputMap>");
        sb.append("</foxml:disseminatorVersion>");
        sb.append("</foxml:disseminator>");
        sb.append("</foxml:digitalObject>");

        try {
            demo997FOXML10ObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // create test FOXML object specifying pid=demo:998
        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" PID=\"demo:998\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"Data Object (Coliseum) for Local Simple Image Demo\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("  <foxml:datastream ID=\"DC\" CONTROL_GROUP=\"X\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion FORMAT_URI=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ID=\"DC1.0\" MIMETYPE=\"text/xml\" LABEL=\"Dublin Core Record for this object\">");
        sb.append("         <foxml:xmlContent>");
        sb.append("        <oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">");
        sb.append("          <dc:title>Coliseum in Rome</dc:title>");
        sb.append("          <dc:creator>Thornton Staples</dc:creator>");
        sb.append("          <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("          <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("          <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("          <dc:format>image/jpeg</dc:format>");
        sb.append("          <dc:identifier>demo:5</dc:identifier>");
        sb.append("        </oai_dc:dc>");
        sb.append("      </foxml:xmlContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS1\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS1.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum thumbnail jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS2\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS2.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum medium jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS3\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS3.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum high jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-high.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS4\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS4.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum veryhigh jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("</foxml:digitalObject>");

        try {
            demo998FOXMLObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // create test FOXML object not specifying pid (allow server to assign)
        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");
        sb.append("  <foxml:objectProperties>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"Data Object (Coliseum) for Local Simple Image Demo\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
        sb.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\"2004-12-10T00:21:57Z\"/>");
        sb.append("  </foxml:objectProperties>");
        sb.append("  <foxml:datastream ID=\"DC\" CONTROL_GROUP=\"X\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion FORMAT_URI=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ID=\"DC1.0\" MIMETYPE=\"text/xml\" LABEL=\"Dublin Core Record for this object\">");
        sb.append("         <foxml:xmlContent>");
        sb.append("        <oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">");
        sb.append("          <dc:title>Coliseum in Rome</dc:title>");
        sb.append("          <dc:creator>Thornton Staples</dc:creator>");
        sb.append("          <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("          <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("          <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("          <dc:format>image/jpeg</dc:format>");
        sb.append("          <dc:identifier>demo:5</dc:identifier>");
        sb.append("        </oai_dc:dc>");
        sb.append("      </foxml:xmlContent>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS1\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS1.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum thumbnail jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS2\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS2.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum medium jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS3\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS3.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum high jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-high.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("  <foxml:datastream ID=\"DS4\" CONTROL_GROUP=\"M\" STATE=\"A\">");
        sb.append("    <foxml:datastreamVersion ID=\"DS4.0\" MIMETYPE=\"image/jpeg\" LABEL=\"Thorny's Coliseum veryhigh jpg image\">");
        sb.append("      <foxml:contentLocation REF=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" TYPE=\"URL\"/>");
        sb.append("    </foxml:datastreamVersion>");
        sb.append("  </foxml:datastream>");
        sb.append("</foxml:digitalObject>");

        try {
            changeme1FOXMLObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // create test METS object specifying pid=demo:999
        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<METS:mets EXT_VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:METS=\"http://www.loc.gov/METS/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.loc.gov/standards/METS/ http://www.fedora.info/definitions/1/0/mets-fedora-ext1-1.xsd\" OBJID=\"demo:999\" LABEL=\"Data Object (Coliseum) for Local Simple Image Demo\" >");
        sb.append("  <METS:dmdSecFedora ID=\"DC\" STATUS=\"A\">");
        sb.append("    <METS:descMD ID=\"DC1.0\">");
        sb.append("      <METS:mdWrap FORMAT_URI=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" MIMETYPE=\"text/xml\" MDTYPE=\"OTHER\" LABEL=\"Dublin Core Record for this object\">");
        sb.append("        <METS:xmlData>");
        sb.append("          <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        sb.append("            <dc:title>Coliseum in Rome</dc:title>");
        sb.append("            <dc:creator>Thornton Staples</dc:creator>");
        sb.append("            <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("            <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("            <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("            <dc:format>image/jpeg</dc:format>");
        sb.append("            <dc:identifier>demo:5</dc:identifier>");
        sb.append("          </oai_dc:dc>");
        sb.append("        </METS:xmlData>");
        sb.append("      </METS:mdWrap>");
        sb.append("    </METS:descMD>");
        sb.append("  </METS:dmdSecFedora>");
        sb.append("  <METS:fileSec>");
        sb.append("    <METS:fileGrp ID=\"DATASTREAMS\">");
        sb.append("      <METS:fileGrp ID=\"DS1\" STATUS=\"A\">");
        sb.append("        <METS:file ID=\"DS1.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" xlink:title=\"Thorny's Coliseum thumbnail jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS2\" STATUS=\"A\">");
        sb.append("        <METS:file ID=\"DS2.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\" xlink:title=\"Thorny's Coliseum medium jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS3\">");
        sb.append("          <METS:file ID=\"DS3.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("            <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-high.jpg\" xlink:title=\"Thorny's Coliseum high jpg image\"/>");
        sb.append("          </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS4\">");
        sb.append("        <METS:file ID=\"DS4.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" xlink:title=\"Thorny's Coliseum veryhigh jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("    </METS:fileGrp>");
        sb.append("  </METS:fileSec>");
        sb.append("</METS:mets>");

        try {
            demo999METSObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // create test METS 1.0 object specifying pid=demo:999b
        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<METS:mets xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:METS=\"http://www.loc.gov/METS/\" xmlns:xlink=\"http://www.w3.org/TR/xlink\" xsi:schemaLocation=\"http://www.loc.gov/standards/METS/ http://www.fedora.info/definitions/1/0/mets-fedora-ext1-0.xsd\" OBJID=\"demo:999b\" TYPE=\"FedoraObject\" LABEL=\"Data Object (Coliseum) for Local Simple Image Demo\" >");
        sb.append("  <METS:dmdSecFedora ID=\"DC\" STATUS=\"A\">");
        sb.append("    <METS:descMD ID=\"DC1.0\">");
        sb.append("      <METS:mdWrap MIMETYPE=\"text/xml\" MDTYPE=\"OTHER\" LABEL=\"Dublin Core Record for this object\">");
        sb.append("        <METS:xmlData>");
        sb.append("          <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        sb.append("            <dc:title>Coliseum in Rome</dc:title>");
        sb.append("            <dc:creator>Thornton Staples</dc:creator>");
        sb.append("            <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("            <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("            <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("            <dc:format>image/jpeg</dc:format>");
        sb.append("            <dc:identifier>demo:5</dc:identifier>");
        sb.append("          </oai_dc:dc>");
        sb.append("        </METS:xmlData>");
        sb.append("      </METS:mdWrap>");
        sb.append("    </METS:descMD>");
        sb.append("  </METS:dmdSecFedora>");
        sb.append("  <METS:fileSec>");
        sb.append("    <METS:fileGrp ID=\"DATASTREAMS\">");
        sb.append("      <METS:fileGrp ID=\"DS1\" STATUS=\"A\">");
        sb.append("        <METS:file ID=\"DS1.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://" + getHost() + ":" + getPort()
                              + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" xlink:title=\"Thorny's Coliseum thumbnail jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS2\" STATUS=\"A\">");
        sb.append("        <METS:file ID=\"DS2.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://" + getHost() + ":" + getPort()
                              + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\" xlink:title=\"Thorny's Coliseum medium jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS3\">");
        sb.append("          <METS:file ID=\"DS3.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("            <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://" + getHost() + ":" + getPort()
                                + "/fedora-demo/simple-image-demo/coliseum-high.jpg\" xlink:title=\"Thorny's Coliseum high jpg image\"/>");
        sb.append("          </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS4\">");
        sb.append("        <METS:file ID=\"DS4.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://" + getHost() + ":" + getPort()
                              + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" xlink:title=\"Thorny's Coliseum veryhigh jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("    </METS:fileGrp>");
        sb.append("  </METS:fileSec>");
        sb.append("</METS:mets>");

        try {
            demo999bMETS10ObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        // create test METS object not specifying pid (allowing server to assign)
        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<METS:mets EXT_VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:METS=\"http://www.loc.gov/METS/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.loc.gov/standards/METS/ http://www.fedora.info/definitions/1/0/mets-fedora-ext1-1.xsd\" LABEL=\"Data Object (Coliseum) for Local Simple Image Demo\" >");
        sb.append("  <METS:dmdSecFedora ID=\"DC\" STATUS=\"A\">");
        sb.append("    <METS:descMD ID=\"DC1.0\">");
        sb.append("      <METS:mdWrap FORMAT_URI=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" MIMETYPE=\"text/xml\" MDTYPE=\"OTHER\" LABEL=\"Dublin Core Record for this object\">");
        sb.append("        <METS:xmlData>");
        sb.append("          <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        sb.append("            <dc:title>Coliseum in Rome</dc:title>");
        sb.append("            <dc:creator>Thornton Staples</dc:creator>");
        sb.append("            <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("            <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("            <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("            <dc:format>image/jpeg</dc:format>");
        sb.append("            <dc:identifier>demo:5</dc:identifier>");
        sb.append("          </oai_dc:dc>");
        sb.append("        </METS:xmlData>");
        sb.append("      </METS:mdWrap>");
        sb.append("    </METS:descMD>");
        sb.append("  </METS:dmdSecFedora>");
        sb.append("  <METS:fileSec>");
        sb.append("    <METS:fileGrp ID=\"DATASTREAMS\">");
        sb.append("      <METS:fileGrp ID=\"DS1\" STATUS=\"A\">");
        sb.append("        <METS:file ID=\"DS1.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" xlink:title=\"Thorny's Coliseum thumbnail jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS2\" STATUS=\"A\">");
        sb.append("        <METS:file ID=\"DS2.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\" xlink:title=\"Thorny's Coliseum medium jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS3\">");
        sb.append("          <METS:file ID=\"DS3.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("            <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-high.jpg\" xlink:title=\"Thorny's Coliseum high jpg image\"/>");
        sb.append("          </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("      <METS:fileGrp ID=\"DS4\">");
        sb.append("        <METS:file ID=\"DS4.0\" MIMETYPE=\"image/jpeg\" OWNERID=\"M\" STATUS=\"A\">");
        sb.append("          <METS:FLocat LOCTYPE=\"URL\" xlink:href=\"http://"
                        + getHost() + ":" + getPort()
                        + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" xlink:title=\"Thorny's Coliseum veryhigh jpg image\"/>");
        sb.append("        </METS:file>");
        sb.append("      </METS:fileGrp>");
        sb.append("    </METS:fileGrp>");
        sb.append("  </METS:fileSec>");
        sb.append("</METS:mets>");

        try {
            changeme2METSObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
        sb.append("  <id>info:fedora/demo:1000</id>");
        sb.append("  <title type=\"text\">Data Object (Coliseum) for Local Simple Image Demo</title>");
        sb.append("  <updated>2008-04-30T03:54:31.525Z</updated>");
        sb.append("  <author>");
        sb.append("    <name>fedoraAdmin</name>");
        sb.append("  </author>");
        sb.append("  <category term=\"Active\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("  <category term=\"2008-04-30T03:54:31.525Z\" scheme=\"info:fedora/fedora-system:def/model#createdDate\"></category>");
        sb.append("  <icon>http://www.fedora-commons.org/images/logo_vertical_transparent_200_251.png</icon>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1000/DC</id>");
        sb.append("    <title type=\"text\">DC</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1000/DC/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"X\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1000/DC/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">DC1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1000/DC\"></thr:in-reply-to>");
        sb.append("    <category term=\"Dublin Core Record for this object\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"491\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <category term=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" scheme=\"info:fedora/fedora-system:def/model#formatURI\"></category>");
        sb.append("    <content type=\"text/xml\">");
        sb.append("      <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        sb.append("        <dc:title>Coliseum in Rome</dc:title>");
        sb.append("        <dc:creator>Thornton Staples</dc:creator>");
        sb.append("        <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("        <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("        <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("        <dc:format>image/jpeg</dc:format>");
        sb.append("        <dc:identifier>demo:1000</dc:identifier>");
        sb.append("      </oai_dc:dc>");
        sb.append("    </content>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1000/RELS-EXT</id>");
        sb.append("    <title type=\"text\">RELS-EXT</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1000/RELS-EXT/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"X\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"false\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1000/RELS-EXT/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">RELS-EXT1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1000/RELS-EXT\"></thr:in-reply-to>");
        sb.append("    <category term=\"Relationships\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"472\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <category term=\"info:fedora/fedora-system:FedoraRELSExt-1.0\" scheme=\"info:fedora/fedora-system:def/model#formatURI\"></category>");
        sb.append("    <content type=\"application/rdf+xml\">");
        sb.append("      <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">");
        sb.append("        <rdf:Description rdf:about=\"info:fedora/demo:1000\">");
        sb.append("          <fedora-model:hasModel rdf:resource=\"info:fedora/demo:UVA_STD_IMAGE_1\"></fedora-model:hasModel>");
        sb.append("          </rdf:Description>");
        sb.append("        </rdf:RDF>");
        sb.append("    </content>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1000/THUMBRES_IMG</id>");
        sb.append("    <title type=\"text\">THUMBRES_IMG</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1000/THUMBRES_IMG/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"M\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1000/THUMBRES_IMG/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">THUMBRES_IMG1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1000/THUMBRES_IMG\"></thr:in-reply-to>");
        sb.append("    <category term=\"Thorny's Coliseum thumbnail jpg image\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"0\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <summary type=\"text\">THUMBRES_IMG1.0</summary>");
        sb.append("    <content type=\"image/jpeg\" src=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-thumb.jpg\"></content>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1000/MEDRES_IMG</id>");
        sb.append("    <title type=\"text\">MEDRES_IMG</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1000/MEDRES_IMG/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"M\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1000/MEDRES_IMG/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">MEDRES_IMG1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1000/MEDRES_IMG\"></thr:in-reply-to>");
        sb.append("    <category term=\"Thorny's Coliseum medium jpg image\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"0\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <summary type=\"text\">MEDRES_IMG1.0</summary>");
        sb.append("    <content type=\"image/jpeg\" src=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-medium.jpg\"></content>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1000/HIGHRES_IMG</id>");
        sb.append("    <title type=\"text\">HIGHRES_IMG</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1000/HIGHRES_IMG/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"M\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1000/HIGHRES_IMG/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">HIGHRES_IMG1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1000/HIGHRES_IMG\"></thr:in-reply-to>");
        sb.append("    <category term=\"Thorny's Coliseum high jpg image\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"0\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <summary type=\"text\">HIGHRES_IMG1.0</summary>");
        sb.append("    <content type=\"image/jpeg\" src=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-high.jpg\"></content>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1000/VERYHIGHRES_IMG</id>");
        sb.append("    <title type=\"text\">VERYHIGHRES_IMG</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1000/VERYHIGHRES_IMG/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"M\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1000/VERYHIGHRES_IMG/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">VERYHIGHRES_IMG1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1000/VERYHIGHRES_IMG\"></thr:in-reply-to>");
        sb.append("    <category term=\"Thorny's Coliseum veryhigh jpg image\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"0\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <summary type=\"text\">VERYHIGHRES_IMG1.0</summary>");
        sb.append("    <content type=\"image/jpeg\" src=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\"></content>");
        sb.append("  </entry>");
        sb.append("</feed>");

        try {
            demo1000ATOMObjectXML = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }

        sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
        sb.append("  <id>info:fedora/demo:1001</id>");
        sb.append("  <title type=\"text\">Data Object (Coliseum) for Local Simple Image Demo</title>");
        sb.append("  <updated>2008-04-30T03:54:31.525Z</updated>");
        sb.append("  <author>");
        sb.append("    <name>fedoraAdmin</name>");
        sb.append("  </author>");
        sb.append("  <category term=\"Active\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("  <category term=\"2008-04-30T03:54:31.525Z\" scheme=\"info:fedora/fedora-system:def/model#createdDate\"></category>");
        sb.append("  <icon>http://www.fedora-commons.org/images/logo_vertical_transparent_200_251.png</icon>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1001/DC</id>");
        sb.append("    <title type=\"text\">DC</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1001/DC/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"X\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1001/DC/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">DC1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1001/DC\"></thr:in-reply-to>");
        sb.append("    <category term=\"DC Record for Coliseum image object\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"491\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <category term=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" scheme=\"info:fedora/fedora-system:def/model#formatURI\"></category>");
        sb.append("       <summary type=\"text\">DC1.0</summary>");
        sb.append("    <content type=\"text/xml\" src=\"DC1.0.xml\"/>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1001/RELS-EXT</id>");
        sb.append("    <title type=\"text\">RELS-EXT</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1001/RELS-EXT/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"X\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"false\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1001/RELS-EXT/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">RELS-EXT1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1001/RELS-EXT\"></thr:in-reply-to>");
        sb.append("    <category term=\"Relationships\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"472\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <category term=\"info:fedora/fedora-system:FedoraRELSExt-1.0\" scheme=\"info:fedora/fedora-system:def/model#formatURI\"></category>");
        sb.append("    <content type=\"application/rdf+xml\" src=\"RELS-EXT1.0.xml\"/>");
        sb.append("    <summary type=\"text\">RELS-EXT1.0</summary>");
        sb.append("  </entry>");
        sb.append("  <entry>");
        sb.append("    <id>info:fedora/demo:1001/VERYHIGHRES_IMG</id>");
        sb.append("    <title type=\"text\">VERYHIGHRES_IMG</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <link href=\"info:fedora/demo:1001/VERYHIGHRES_IMG/2008-04-30T03:54:31.459Z\" rel=\"alternate\"></link>");
        sb.append("    <category term=\"A\" scheme=\"info:fedora/fedora-system:def/model#state\"></category>");
        sb.append("    <category term=\"M\" scheme=\"info:fedora/fedora-system:def/model#controlGroup\"></category>");
        sb.append("    <category term=\"true\" scheme=\"info:fedora/fedora-system:def/model#versionable\"></category>");
        sb.append("  </entry>");
        sb.append("  <entry xmlns:thr=\"http://purl.org/syndication/thread/1.0\">");
        sb.append("    <id>info:fedora/demo:1001/VERYHIGHRES_IMG/2008-04-30T03:54:31.459Z</id>");
        sb.append("    <title type=\"text\">VERYHIGHRES_IMG1.0</title>");
        sb.append("    <updated>2008-04-30T03:54:31.459Z</updated>");
        sb.append("    <thr:in-reply-to ref=\"info:fedora/demo:1001/VERYHIGHRES_IMG\"></thr:in-reply-to>");
        sb.append("    <category term=\"Thorny's Coliseum veryhigh jpg image\" scheme=\"info:fedora/fedora-system:def/model#label\"></category>");
        sb.append("    <category term=\"DISABLED\" scheme=\"info:fedora/fedora-system:def/model#digestType\"></category>");
        sb.append("    <category term=\"none\" scheme=\"info:fedora/fedora-system:def/model#digest\"></category>");
        sb.append("    <category term=\"0\" scheme=\"info:fedora/fedora-system:def/model#length\"></category>");
        sb.append("    <summary type=\"text\">VERYHIGHRES_IMG1.0</summary>");
        sb.append("    <content type=\"image/jpeg\" src=\"http://" + getHost() + ":" + getPort() + "/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\"></content>");
        sb.append("  </entry>");
        sb.append("</feed>");

        byte[] demo1001_manifest = null;
        try {
            demo1001_manifest = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {}

        sb = new StringBuffer();
        sb.append("      <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        sb.append("        <dc:title>Coliseum in Rome</dc:title>");
        sb.append("        <dc:creator>Thornton Staples</dc:creator>");
        sb.append("        <dc:subject>Architecture, Roman</dc:subject>");
        sb.append("        <dc:description>Image of Coliseum in Rome</dc:description>");
        sb.append("        <dc:publisher>University of Virginia Library</dc:publisher>");
        sb.append("        <dc:format>image/jpeg</dc:format>");
        sb.append("        <dc:identifier>demo:1001</dc:identifier>");
        sb.append("      </oai_dc:dc>");
        byte[] demo1001_dc = null;
        try {
            demo1001_dc = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {}

        sb = new StringBuffer();
        sb.append("      <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">");
        sb.append("        <rdf:Description rdf:about=\"info:fedora/demo:1001\">");
        sb.append("          <fedora-model:hasModel rdf:resource=\"info:fedora/demo:UVA_STD_IMAGE_1\"></fedora-model:hasModel>");
        sb.append("        </rdf:Description>");
        sb.append("      </rdf:RDF>");

        try {
            demo1001_relsext = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {}

        ZipEntry manifest = new ZipEntry("atommanifest.xml");
        ZipEntry dc = new ZipEntry("DC1.0.xml");
        ZipEntry relsext = new ZipEntry("RELS-EXT1.0.xml");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bout);
        try {
            zip.putNextEntry(manifest);
            zip.write(demo1001_manifest);
            zip.putNextEntry(dc);
            zip.write(demo1001_dc);
            zip.putNextEntry(relsext);
            zip.write(demo1001_relsext);
            zip.flush();
            zip.close();
            demo1001ATOMZip = bout.toByteArray();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("APIM TestSuite");
        suite.addTestSuite(TestAPIM.class);
        return new DemoObjectTestSetup(suite);
    }

    @Override
    public void setUp() throws Exception {
        apim = getFedoraClient().getAPIM();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        nsMap.put("dc", "http://purl.org/dc/elements/1.1/");
        nsMap.put("foxml", "info:fedora/fedora-system:def/foxml#");
        nsMap.put("audit", "info:fedora/fedora-system:def/audit#");
        nsMap.put("METS", "http://www.loc.gov/METS/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Override
    @After
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }

    public void testGetObjectXML() throws Exception {

        // test getting xml for object demo:5
        System.out.println("Running TestAPIM.testGetObjectXML...");
        byte[] objectXML = apim.getObjectXML("demo:5");
        assertTrue(objectXML.length > 0);
        String xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testGetObjectXML demo:5\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:5']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("6",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);
    }

    public void testObjectMethods() throws Exception {

        // test the object methods
        // 1) ingest
        // 2) modifyObject
        // 3) export
        // 4) purgeObject

        Set<String> serverAssignedPIDs = new HashSet<String>();

        // (1) test ingest
        System.out.println("Running TestAPIM.testIngest...");
        String pid =
                apim.ingest(demo998FOXMLObjectXML,
                            FOXML1_1.uri,
                            "ingesting new foxml object");
        assertNotNull(pid);
        serverAssignedPIDs.add(pid);

        byte[] objectXML = apim.getObjectXML(pid);
        assertTrue(objectXML.length > 0);
        String xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathExists("//foxml:datastream[@ID='AUDIT']", xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        pid = apim.ingest(changeme1FOXMLObjectXML, FOXML1_1.uri, null);
        assertNotNull(pid);
        serverAssignedPIDs.add(pid);

        objectXML = apim.getObjectXML(pid);
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathNotExists("//foxml:datastream[@ID='AUDIT']", xmlIn); // No audit trail should be created
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        pid =
                apim.ingest(demo999METSObjectXML,
                            METS_EXT1_1.uri,
                            "ingesting new mets object");
        assertNotNull(pid);
        serverAssignedPIDs.add(pid);

        objectXML = apim.getObjectXML(pid);
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        pid =
                apim.ingest(changeme2METSObjectXML,
                            METS_EXT1_1.uri,
                            "ingesting new mets object");
        assertNotNull(pid);
        serverAssignedPIDs.add(pid);

        objectXML = apim.getObjectXML(pid);
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        pid =
            apim.ingest(demo1000ATOMObjectXML,
                        ATOM1_1.uri,
                        "ingesting new atom object");
            assertNotNull(pid);
            serverAssignedPIDs.add(pid);

            objectXML = apim.getObjectXML(pid);
            assertTrue(objectXML.length > 0);
            xmlIn = new String(objectXML, "UTF-8");
            assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
            assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                      xmlIn);
            assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                      xmlIn);
            assertXpathEvaluatesTo("6",
                           "count(//foxml:datastream[@ID!='AUDIT'])",
                           xmlIn);

        // (2) test modifyObject
        System.out.println("Running TestAPIM.testModifyObject...");
        // test changing object demo:5 by modifying state to Inactive; leave label unchanged
        String result =
                apim.modifyObject("demo:5",
                                  "I",
                                  null,
                                  null,
                                  "changed state to Inactive");

        objectXML = apim.getObjectXML("demo:5");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state'and @VALUE='Inactive']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label'and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['modifyObject']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:justification['changed state to Inactive']",
                          xmlIn);

        // test changing object demo:5 by modifying label to "changed label"; leave state unchanged from last value
        result =
                apim.modifyObject("demo:5",
                                  null,
                                  "changed label",
                                  null,
                                  "changed label");
        objectXML = apim.getObjectXML("demo:5");
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Inactive']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='changed label']",
                          xmlIn);

        // test changing object demo:5 by modifying both state and label
        result =
                apim.modifyObject("demo:5",
                                  "D",
                                  "label of object to be deleted",
                                  null,
                                  "changed label and state");
        objectXML = apim.getObjectXML("demo:5");
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Deleted']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='label of object to be deleted']",
                          xmlIn);

        // reset demo:5
        result =
                apim.modifyObject("demo:5",
                                  "A",
                                  "Data Object (Coliseum) for Local Simple Image Demo",
                                  null,
                                  "reset label and state");
        objectXML = apim.getObjectXML("demo:5");
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);

        // (3) test export
        System.out.println("Running TestAPIM.testExport...");
        // test exporting object as foxml with exportContext of default
        objectXML = apim.export("demo:998", FOXML1_1.uri, "default");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:998']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // test exporting object as foxml with exportContext of default
        objectXML = apim.export("demo:998", FOXML1_1.uri, "public");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:998']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // test exporting object as foxml with exportContext of migrate
        objectXML = apim.export("demo:998", FOXML1_1.uri, "migrate");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:998']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // test exporting object as mets with exportContext of default
        objectXML = apim.export("demo:999", METS_EXT1_1.uri, "default");
        objectXML = apim.getObjectXML("demo:999");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:999']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // test exporting object as mets with exportContext of public
        objectXML = apim.export("demo:999", METS_EXT1_1.uri, "public");
        objectXML = apim.getObjectXML("demo:999");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:999']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // test exporting object as mets with exportContext of migrate
        objectXML = apim.export("demo:999", METS_EXT1_1.uri, "migrate");
        objectXML = apim.getObjectXML("demo:999");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:999']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#state' and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property[@NAME='info:fedora/fedora-system:def/model#label' and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // (4) test purgeObject
        System.out.println("Running TestAPIM.testPurgeObject...");
        Iterator<String> it = serverAssignedPIDs.iterator();
        while (it.hasNext()) {
            pid = it.next();
            result = apim.purgeObject(pid, "purging object " + pid, false);
            assertNotNull(result);
        }
    }

    public void testDatastreamMethods() throws Exception {

        // test datastream methods
        // 1) addDatastream
        // 2) modifyDatastreamByReference
        // 3) modifyDatastreamByValue
        // 4) purgeDatastream
        // 5) getDatastream
        // 6) getDatastreams
        // 7) getDatastreamHistory

        // (1) test addDatastream
        System.out.println("Running TestAPIM.testAddDatastream...");
        // test adding M type datastream with unknown checksum type and no altIDs, should fail throwing exception
        try {
            apim.addDatastream("demo:14",
                               "NEWDS1",
                               null,
                               "A New M-type Datastream",
                               true,
                               "text/xml",
                               "info:myFormatURI/Mtype/stuff#junk",
                               getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC",
                               "M",
                               "A",
                               "MD6",
                               null,
                               "adding new datastream");
            // fail if datastream was added
            Assert.fail();
        } catch (AxisFault af) {
            assertTrue(af.getFaultString()
                    .contains("Unknown checksum algorithm specified:"));
        }
        // test adding M type datastream with unimplemented checksum type and no altIDs, should fail throwing exception
        try {
            apim.addDatastream("demo:14",
                               "NEWDS1",
                               null,
                               "A New M-type Datastream",
                               true,
                               "text/xml",
                               "info:myFormatURI/Mtype/stuff#junk",
                               getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC",
                               "M",
                               "A",
                               "TIGER",
                               null,
                               "adding new datastream");
            // fail if datastream was added
            Assert.fail();
        } catch (AxisFault af) {
            assertTrue(af.getFaultString()
                    .contains("Checksum algorithm not yet implemented:"));
        }
        // test adding M type datastream
        String[] altIds = new String[1];
        altIds[0] = "Datastream 1 Alternate ID";
        String datastreamId =
                apim
                        .addDatastream("demo:14",
                                       "NEWDS1",
                                       altIds,
                                       "A New M-type Datastream",
                                       true,
                                       "text/xml",
                                       "info:myFormatURI/Mtype/stuff#junk",
                                       getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC",
                                       "M",
                                       "A",
                                       null,
                                       null,
                                       "adding new datastream");

        // test that datastream was added
        assertEquals(datastreamId, "NEWDS1");
        byte[] objectXML = apim.getObjectXML("demo:14");
        assertTrue(objectXML.length > 0);
        String xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testAddDatastream NEWDS1 as type M\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:14']", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='NEWDS1' and @CONTROL_GROUP='M' and @STATE='A']",
                          xmlIn);
        assertXpathExists("//foxml:datastreamVersion[@ID='NEWDS1.0' and @MIMETYPE='text/xml' and @LABEL='A New M-type Datastream' and @ALT_IDS='Datastream 1 Alternate ID' and @FORMAT_URI='info:myFormatURI/Mtype/stuff#junk']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['addDatastream']",
                          xmlIn);
        assertXpathEvaluatesTo("6",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        //test adding X type datastream
        altIds[0] = "Datastream 2 Alternate ID";
        datastreamId =
                apim
                        .addDatastream("demo:14",
                                       "NEWDS2",
                                       altIds,
                                       "A New X-type Datastream",
                                       true,
                                       "text/xml",
                                       "info:myFormatURI/Xtype/stuff#junk",
                                       getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC",
                                       "X",
                                       "A",
                                       null,
                                       null,
                                       "adding new datastream");

        // test that datastream was added
        objectXML = apim.getObjectXML("demo:14");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testAddDatastream NEWDS2 as type X\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:14']", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='NEWDS2' and @CONTROL_GROUP='X' and @STATE='A']",
                          xmlIn);
        assertXpathExists("//foxml:datastreamVersion[@ID='NEWDS2.0' and @MIMETYPE='text/xml' and @LABEL='A New X-type Datastream' and @ALT_IDS='Datastream 2 Alternate ID' and @FORMAT_URI='info:myFormatURI/Xtype/stuff#junk']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['addDatastream']",
                          xmlIn);
        assertXpathEvaluatesTo("7",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        altIds[0] = "Datastream 3 Alternate ID";
        datastreamId =
                apim
                        .addDatastream("demo:14",
                                       "NEWDS3",
                                       altIds,
                                       "A New E-type Datastream",
                                       true,
                                       "text/xml",
                                       "info:myFormatURI/Etype/stuff#junk",
                                       getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC",
                                       "E",
                                       "A",
                                       null,
                                       null,
                                       "adding new datastream");

        // test adding E type datastream
        objectXML = apim.getObjectXML("demo:14");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testAddDatastream NEWDS3 as type E\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:14']", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='NEWDS3' and @CONTROL_GROUP='E' and @STATE='A']",
                          xmlIn);
        assertXpathExists("//foxml:datastreamVersion[@ID='NEWDS3.0' and @MIMETYPE='text/xml' and @LABEL='A New E-type Datastream' and @ALT_IDS='Datastream 3 Alternate ID' and @FORMAT_URI='info:myFormatURI/Etype/stuff#junk']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['addDatastream']",
                          xmlIn);
        assertXpathEvaluatesTo("8",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);


        // test adding RELS-EXT and RELS-INT datastreams triggers validation
        // add RELS-EXT from a different object; will be invalid for this object
        // FIXME: consider refactoring into a general validation test suite

        for (String relsDsId : new String[] { "RELS-EXT", "RELS-INT" }) {
            try {
                altIds[0] = "Datastream 2 Alternate ID";
                datastreamId =
                        apim
                                .addDatastream("demo:18",
                                               relsDsId,
                                               altIds,
                                               "A New RELS Datastream",
                                               true,
                                               "application/rdf+xml",
                                               "info:fedora/fedora-system:FedoraRELSExt-1.0",
                                               getBaseURL() + "/get/fedora-system:ContentModel-3.0/RELS-EXT",
                                               "X",
                                               "A",
                                               null,
                                               null,
                                               "adding new datastream");
                fail(relsDsId + " was not validated on addDatastream");
            } catch (RemoteException e) {
            }
        }


        // (2) test modifyDatastreamByReference
        System.out
                .println("Running TestAPIM.testModifyDatastreamByReference...");
        altIds = new String[1];
        altIds[0] = "Datastream 1 Modified Alternate ID";
        datastreamId = apim.modifyDatastreamByReference("demo:14",
                                                        "NEWDS1",
                                                        altIds,
                                                        "Modified M-type Datastream",
                                                        "text/xml",
                                                        "info:newMyFormatURI/Mtype/stuff#junk",
                                                        getBaseURL() + "/get/fedora-system:ContentModel-3.0/DC",
                                                        null,
                                                        null,
                                                        "modified datastream",
                                                        false);

        // test that datastream was modified
        objectXML = apim.getObjectXML("demo:14");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testModifyDatastreamByReference NEWDS1\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:14']", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='NEWDS1' and @CONTROL_GROUP='M' and @STATE='A']",
                          xmlIn);
        assertXpathExists("//foxml:datastreamVersion[@ID='NEWDS1.1' and @MIMETYPE='text/xml' and @LABEL='Modified M-type Datastream' and @ALT_IDS='Datastream 1 Modified Alternate ID' and @FORMAT_URI='info:newMyFormatURI/Mtype/stuff#junk']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['modifyDatastreamByReference']",
                          xmlIn);
        assertXpathEvaluatesTo("8",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // (3) test modifyDatastreamByValue
        System.out.println("Running TestAPIM.testModifyDatastreamByValue...");
        altIds = new String[1];
        altIds[0] = "Datastream 2 Modified Alternate ID";
        datastreamId =
                apim
                        .modifyDatastreamByValue("demo:14",
                                                 "NEWDS2",
                                                 altIds,
                                                 "Modified X-type Datastream",
                                                 "text/xml",
                                                 "info:newMyFormatURI/Xtype/stuff#junk",
                                                 dsXML,
                                                 null,
                                                 null,
                                                 "modified datastream",
                                                 false);

        // test that datastream was modified
        objectXML = apim.getObjectXML("demo:14");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testModifyDatastreamByValue NEWDS2\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:14']", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='NEWDS2' and @CONTROL_GROUP='X' and @STATE='A']",
                          xmlIn);
        assertXpathExists("//foxml:datastreamVersion[@ID='NEWDS2.1' and @MIMETYPE='text/xml' and @LABEL='Modified X-type Datastream' and @ALT_IDS='Datastream 2 Modified Alternate ID' and @FORMAT_URI='info:newMyFormatURI/Xtype/stuff#junk']",
                          xmlIn);
        assertXpathExists("foxml:digitalObject/foxml:datastream[@ID='NEWDS2'][//dc:identifier='Identifier 5']",
                          xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['modifyDatastreamByValue']",
                          xmlIn);
        assertXpathEvaluatesTo("8",
                               "count(//foxml:datastream[@ID!='AUDIT'])",
                               xmlIn);

        // test modifyDatastreamByValue triggers RELS-EXT and RELS-INT validation
        // RELS datastream content is invalid as it's for a different object
        // FIXME: consider refactoring into a general validation test suite
        for (String relsDsId : new String[] { "RELS-EXT", "RELS-INT" }) {
            try {
                altIds[0] = "Datastream 2 Alternate ID";
                datastreamId =
                        apim
                                .modifyDatastreamByValue("demo:SmileyGreetingCard",
                                               relsDsId,
                                               altIds,
                                               "Modified RELS Datastream",
                                               "application/rdf+xml",
                                               "info:fedora/fedora-system:FedoraRELSExt-1.0",
                                               demo1001_relsext,
                                               null,
                                               null,
                                               "modifying datastream",
                                               false);
                fail(relsDsId + " was not validated on modifyDatastreamByValue");
            } catch (RemoteException e) {
            }
        }


        // (3.5) test modifyDatastreamByValue of METHODMAP datastream of BMech object
        System.out
                .println("Running TestAPIM.testModifyDatastreamByValue for METHODMAP...");
        datastreamId =
                apim
                        .modifyDatastreamByValue("demo:2",
                                                 "METHODMAP",
                                                 null,
                                                 "Mapping of WSDL to Fedora Notion of Method Definitions",
                                                 "text/xml",
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 "modified datastream",
                                                 false);

        // test that datastream was modified
        objectXML = apim.getObjectXML("demo:2");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        //System.out.println("***** Testcase: TestAPIM.testModifyDatastreamByValue NEWDS2\n"+xmlIn);
        assertXpathExists("foxml:digitalObject[@PID='demo:2']", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='METHODMAP' and @CONTROL_GROUP='X' and @STATE='A']",
                          xmlIn);
        assertXpathExists("//foxml:datastreamVersion[@ID='METHODMAP.1' and @MIMETYPE='text/xml' and @LABEL='Mapping of WSDL to Fedora Notion of Method Definitions']",
                          xmlIn);

        // (4) test modifyDatastreamByValue for checksumming and compareDatastreamChecksum
        System.out.println("Running TestAPIM.compareDatastreamChecksum...");
        try {
            datastreamId =
                    apim.modifyDatastreamByValue("demo:14",
                                                 "NEWDS2",
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 "MD6",
                                                 null,
                                                 "turned on checksumming",
                                                 false);
            // fail if datastream was modified
            Assert.fail();
        } catch (AxisFault af) {
            assertTrue(af.getFaultString()
                    .contains("Unknown checksum algorithm specified:"));
        }
        // test adding M type datastream with unimplemented checksum type and no altIDs, should fail throwing exception
        try {
            datastreamId =
                    apim.modifyDatastreamByValue("demo:14",
                                                 "NEWDS2",
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 "TIGER",
                                                 null,
                                                 "turned on checksumming",
                                                 false);
            // fail if datastream was modified
            Assert.fail();
        } catch (AxisFault af) {
            assertTrue(af.getFaultString()
                    .contains("Checksum algorithm not yet implemented:"));
        }
        datastreamId =
                apim.modifyDatastreamByValue("demo:14",
                                             "NEWDS2",
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             "MD5",
                                             null,
                                             "turned on checksumming",
                                             false);

        // test that datastream has a checksum that compares correctly
        String checksum =
                apim.compareDatastreamChecksum("demo:14", "NEWDS2", null);
        assertTrue(checksum.length() > 0);
        assertTrue(!checksum.equals("none"));

        datastreamId =
                apim.modifyDatastreamByValue("demo:14",
                                             "NEWDS2",
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             "MD5",
                                             checksum,
                                             "turned off checksumming",
                                             false);

        // test that datastream has a checksum that compares correctly
        String checksum2 =
                apim.compareDatastreamChecksum("demo:14", "NEWDS2", null);
        assertTrue(checksum2.length() > 0);
        assertTrue(checksum2.equals(checksum));

        datastreamId =
                apim.modifyDatastreamByValue("demo:14",
                                             "NEWDS2",
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             "DISABLED",
                                             null,
                                             "turned off checksumming",
                                             false);

        // test that datastream has a checksum that compares correctly
        checksum = apim.compareDatastreamChecksum("demo:14", "NEWDS2", null);
        assertTrue(checksum.length() > 0);
        assertTrue(checksum.equals("none"));

        // (5) test purgeDatastream
        System.out.println("Running TestAPIM.testPurgeDatastream...");
        // test specifying date-time for startDate and null for endDate
        String[] results =
                apim.purgeDatastream("demo:14",
                                     "NEWDS1",
                                     "1900-01-01T00:00:00.000Z",
                                     null,
                                     "purging datastream NEWDS1",
                                     false);
        for (String element : results) {
            System.out
                    .println("***** Testcase: TestAPIM.testPurgeDatastream specifying startDate=\"1900-01-01T00:00:00.000Z\" and endDate=null dsID: "
                            + element);
        }
        assertTrue(results.length > 0);

        // test specifying null for both startDate and endDate
        results =
                apim.purgeDatastream("demo:14",
                                     "NEWDS2",
                                     null,
                                     null,
                                     "purging datastream NEWDS2",
                                     false);
        for (String element : results) {
            System.out
                    .println("***** Testcase: TestAPIM.testPurgeDatastream specifying startDate=null and endDate=null dsID: "
                            + element);
        }
        assertTrue(results.length > 0);

        // test specifying date-time for both startDate and endDate
        results =
                apim.purgeDatastream("demo:14",
                                     "NEWDS3",
                                     "1900-01-01T00:00:00.000Z",
                                     "9999-01-01T00:00:00.000Z",
                                     "purging datastream NEWDS3",
                                     false);
        for (String element : results) {
            System.out
                    .println("***** Testcase: TestAPIM.testPurgeDatastream specifying startDate=\"1900-01-01T00:00:00.000Z\" endDate=\"9999-01-01T00:00:00.000Z\" dsID: "
                            + element);
        }
        assertTrue(results.length > 0);

        // test purgeDatastream audit
        objectXML = apim.getObjectXML("demo:14");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:14']", xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['purgeDatastream']",
                          xmlIn);

        // (6) test getDatastream
        System.out.println("Running TestAPIM.testGetDatastream...");
        // test getting datastream id XML_SOURCE for object demo:26 specifying null for datetime
        Datastream ds = apim.getDatastream("demo:26", "XML_SOURCE", null);
        assertNotNull(ds);
        Datastream[] dsArray = new Datastream[1];
        dsArray[0] = ds;
        System.out
                .println("***** Testcase: TestAPIM.testGetDatastream getDatastream(\"demo:26\", \"XML_SOURCE\", null)");

        checkDatastream(dsArray,
                        "XML_SOURCE",
                        null,
                        "FOP Dissemination as Datastream",
                        "http://" + getHost()
                                + ":" + getPort() + "/" + getFedoraAppServerContext() + "/get/demo:26/demo:22/getFO",
                        "text/xml",
                        "A",
                        "XML_SOURCE1.0",
                        true,
                        "E",
                        -1,
                        new String[] {});

        // test getting datastream id XML_SOURCE for object demo:26 specifying datetime
        ds =
                apim.getDatastream("demo:26",
                                   "XML_SOURCE",
                                   "9999-01-01T00:00:00.000Z");
        dsArray[0] = ds;
        System.out
                .println("***** Testcase: TestAPIM.testGetDatastream getDatastream(\"demo:26\", ,\"XML_SOURCE\", \"9999-01-01T00:00:00.000Z\")");

        checkDatastream(dsArray,
                        "XML_SOURCE",
                        null,
                        "FOP Dissemination as Datastream",
                        "http://" + getHost()
                                + ":" + getPort() + "/" + getFedoraAppServerContext() + "/get/demo:26/demo:22/getFO",
                        "text/xml",
                        "A",
                        "XML_SOURCE1.0",
                        true,
                        "E",
                        -1,
                        new String[] {});

        // (7) test getDatastreams
        System.out.println("Running TestAPIM.testGetDatastreams...");
        // test getting all datastreams for object demo:26 specifying null for datetime and state
        dsArray = apim.getDatastreams("demo:26", null, null);
        assertEquals(dsArray.length, 4);
        System.out
                .println("***** Testcase: TestAPIM.testGetDatastreams getDatastreams(\"demo:26\", null, null) number of Datastreams: "
                        + dsArray.length);

        checkDatastream(dsArray,
                        "DC",
                        OAI_DC2_0.uri,
                        "Dublin Core Record for this object",
                        null,
                        "text/xml",
                        "A",
                        "DC1.0",
                        true,
                        "X",
                        -1,
                        new String[] {});

        checkDatastream(dsArray,
                        "XML_SOURCE",
                        null,
                        "FOP Dissemination as Datastream",
                        "http://" + getHost()
                                + ":" + getPort() +  "/" + getFedoraAppServerContext() + "/get/demo:26/demo:22/getFO",
                        "text/xml",
                        "A",
                        "XML_SOURCE1.0",
                        true,
                        "E",
                        -1,
                        new String[] {});

        checkDatastream(dsArray,
                        "TEI_SOURCE",
                        null,
                        "TEI Source",
                        null,
                        "text/xml",
                        "A",
                        "TEI_SOURCE1.0",
                        true,
                        "X",
                        -1,
                        new String[] {});

        checkDatastream(dsArray,
                        "RELS-EXT",
                        RELS_EXT1_0.uri,
                        "RDF Statements about this object",
                        null,
                        "application/rdf+xml",
                        "A",
                        "RELS-EXT1.0",
                        true,
                        "X",
                        -1,
                        new String[] {});

        // test getting all datastreams for object demo:26 specifying null for state
        dsArray =
                apim
                        .getDatastreams("demo:26",
                                        "9999-01-01T00:00:00.000Z",
                                        null);
        System.out
                .println("***** Testcase: TestAPIM.testGetDatastreams getDatastreams(\"demo:26\", \"9999-01-01T00:00:00.000Z\", null) number of Datastreams: "
                        + dsArray.length);
        assertEquals(dsArray.length, 4);

        checkDatastream(dsArray,
                        "DC",
                        OAI_DC2_0.uri,
                        "Dublin Core Record for this object",
                        null,
                        "text/xml",
                        "A",
                        "DC1.0",
                        true,
                        "X",
                        -1,
                        new String[] {});

        checkDatastream(dsArray,
                        "XML_SOURCE",
                        null,
                        "FOP Dissemination as Datastream",
                        "http://" + getHost()
                                + ":" + getPort() +  "/" + getFedoraAppServerContext() + "/get/demo:26/demo:22/getFO",
                        "text/xml",
                        "A",
                        "XML_SOURCE1.0",
                        true,
                        "E",
                        -1,
                        new String[] {});

        checkDatastream(dsArray,
                        "TEI_SOURCE",
                        null,
                        "TEI Source",
                        null,
                        "text/xml",
                        "A",
                        "TEI_SOURCE1.0",
                        true,
                        "X",
                        -1,
                        new String[] {});

        checkDatastream(dsArray,
                        "RELS-EXT",
                        RELS_EXT1_0.uri,
                        "RDF Statements about this object",
                        null,
                        "application/rdf+xml",
                        "A",
                        "RELS-EXT1.0",
                        true,
                        "X",
                        -1,
                        new String[] {});

        // (8) test getDatastreamHistory
        System.out.println("Running TestAPIM.testGetDatastreamHistory...");
        // test getting datastream history for datastream DC of object demo:5
        dsArray = apim.getDatastreamHistory("demo:5", "DC");
        assertEquals(dsArray.length, 1);
    }

    /**
     * @param size If non-negative, the size of the datastream must match
     *             this size.
     */
    private void checkDatastream(Datastream[] dsArray,
                                 String id,
                                 String formatURI,
                                 String label,
                                 String location,
                                 String mimeType,
                                 String state,
                                 String versionID,
                                 boolean isVersionable,
                                 String controlGroup,
                                 int size,
                                 String[] altIDs) {
        Datastream ds = null;
        for (Datastream candidate : dsArray) {
            if (candidate.getID().equals(id)) {
                ds = candidate;
            }
        }
        if (ds != null) {
            if (testingMETS() && formatURI == null && ds.getFormatURI() != null) {
                assertTrue(ds.getFormatURI().endsWith("MD.OTHER.UNSPECIFIED"));
            } else {
                assertEquals(formatURI, ds.getFormatURI());
            }
            assertEquals(label, ds.getLabel());
            assertEquals(location, ds.getLocation());
            assertEquals(mimeType, ds.getMIMEType());
            assertEquals(state, ds.getState());
            assertEquals(versionID, ds.getVersionID());
            assertEquals(isVersionable, ds.isVersionable());
            assertEquals(controlGroup, ds.getControlGroup().getValue());
            if (size > -1) {
                assertEquals(size, ds.getSize());
            }
            if (altIDs == null) {
                assertEquals(null, ds.getAltIDs());
            } else {
                assertEquals(altIDs.length, ds.getAltIDs().length);
                for (int i = 0; i < altIDs.length; i++) {
                    assertEquals("AltID at position " + i + " did not match",
                                 altIDs[i],
                                 ds.getAltIDs()[i]);
                }
            }
        } else {
            assertEquals("Datastream with id " + id + " not found in dsArray",
                         true,
                         false);
        }
    }

    public void testSetDatastreamState() throws Exception {

        // test setting datastream state to "I" for datastream id DC of object demo:5
        System.out.println("Running TestAPIM.testSetDatastreamState...");
        String result =
                apim
                        .setDatastreamState("demo:5",
                                            "DC",
                                            "I",
                                            "changed state of datstream DC to Inactive");
        assertNotNull(result);
        Datastream ds = apim.getDatastream("demo:5", "DC", null);
        assertEquals("I", ds.getState());
        //System.out.println("***** Testcase: TestAPIM.testSetDatastreamState new state: "+ds.getState());

        // test setDatastreamState audit
        byte[] objectXML = apim.getObjectXML("demo:5");
        assertTrue(objectXML.length > 0);
        String xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:5']", xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['setDatastreamState']",
                          xmlIn);

        // reset datastream state
        result =
                apim
                        .setDatastreamState("demo:5",
                                            "DC",
                                            "A",
                                            "reset state of datastream DC to Active");
        assertNotNull(result);
        ds = apim.getDatastream("demo:5", "DC", null);
        assertEquals("A", ds.getState());
    }

    public void testSetDatastreamVersionable() throws Exception {

        // test setting datastream to not versionalble for datastream id DC of object demo:5
        System.out.println("Running TestAPIM.testSetDatastreamVersionable...");
        String result =
                apim
                        .setDatastreamVersionable("demo:5",
                                                  "DC",
                                                  false,
                                                  "changed versionable on datastream DC to false");
        assertNotNull(result);
        Datastream ds = apim.getDatastream("demo:5", "DC", null);
        assertEquals(false, ds.isVersionable());

        // test setDatastreamVersionable audit
        byte[] objectXML = apim.getObjectXML("demo:5");
        assertTrue(objectXML.length > 0);
        String xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:5']", xmlIn);
        assertXpathExists("//audit:auditTrail/audit:record[last()]/audit:action['setDatastreamVersionable']",
                          xmlIn);

        // reset datastream to versionable
        result =
                apim
                        .setDatastreamVersionable("demo:5",
                                                  "DC",
                                                  true,
                                                  "reset versionable on datastream DC to true");
        assertNotNull(result);
        ds = apim.getDatastream("demo:5", "DC", null);
        assertEquals(true, ds.isVersionable());


    }

    public void testGetNextPID() throws Exception {

        // test null for both arguments
        System.out.println("Running TestAPIM.testGetNextPID...");
        String[] pids = apim.getNextPID(null, null);
        assertTrue(pids.length > 0);
        //System.out.println("***** Testcase: TestAPIM.testGetNextPID  nextPid(null, null): "+pids[0]);
        assertEquals(pids.length, 1);
        assertTrue(pids[0].startsWith("changeme"));

        // test null for numPids argument
        pids = apim.getNextPID(null, "dummy");
        assertTrue(pids.length > 0);
        //System.out.println("***** Testcase: TestAPIM.testGetNextPID  nextPid(null, \"dummy\"): "+pids[0]);
        assertEquals(pids.length, 1);
        assertTrue(pids[0].startsWith("dummy:"));

        // test null for namespace argument
        pids = apim.getNextPID(new NonNegativeInteger("1"), null);
        assertTrue(pids.length > 0);
        //System.out.println("***** Testcase: TestAPIM.testGetNextPID  nextPid(1, null): "+pids[0]);
        assertEquals(pids.length, 1);
        assertTrue(pids[0].startsWith("changeme"));

        // test both arguments non-null
        pids = apim.getNextPID(new NonNegativeInteger("2"), "namespace");
        assertTrue(pids.length > 0);
        //System.out.println("***** Testcase: TestAPIM.testGetNextPID  nextPid(2, \"namespace\"): "+pids[0]+" , "+pids[1]);
        assertEquals(pids.length, 2);
        assertTrue(pids[0].startsWith("namespace:"));
        assertTrue(pids[1].startsWith("namespace:"));
    }

    public void testLegacyDOFormats() throws Exception {
        System.out.println("Running TestAPIM.testDigitalObjectFormat...");

        // test ingesting foxml 1.0 Object
        String pid = apim.ingest(demo997FOXML10ObjectXML,
                                 FOXML1_0.uri,
                                "ingesting new foxml 1.0 object");
        assertNotNull(pid);

        byte[] objectXML = apim.getObjectXML(pid);
        assertTrue(objectXML.length > 0);
        String xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property" +
                          "[@NAME='info:fedora/fedora-system:def/model#state'" +
                          " and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property" +
                          "[@NAME='info:fedora/fedora-system:def/model#label'" +
                          " and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathNotExists("//foxml:objectProperties/foxml:property" +
                             "[@NAME='info:fedora/fedora-system:def/model#contentModel']",
                             xmlIn);
        assertXpathNotExists("//foxml:disseminator", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='AUDIT']", xmlIn);
        assertXpathEvaluatesTo("5", "count(//foxml:datastream[@ID!='AUDIT'])", xmlIn);


        // test exporting foxml 1.0 object
        objectXML = apim.export("demo:997", FOXML1_0.uri, "migrate");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='demo:997']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property" +
                          "[@NAME='info:fedora/fedora-system:def/model#state'" +
                          " and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property" +
                          "[@NAME='info:fedora/fedora-system:def/model#label'" +
                          " and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathNotExists("//foxml:objectProperties/foxml:property" +
                             "[@NAME='info:fedora/fedora-system:def/model#contentModel']",
                             xmlIn);
        assertXpathNotExists("//foxml:disseminator", xmlIn);
        assertXpathExists("//foxml:datastream[@ID='AUDIT']", xmlIn);
        assertXpathEvaluatesTo("5", "count(//foxml:datastream[@ID!='AUDIT'])", xmlIn);

        // purge foxml 1.0 object
        apim.purgeObject(pid, "purging object demo:997", false);


        // test ingesting mets 1.0 object
        pid =  apim.ingest(demo999bMETS10ObjectXML,
                           METS_EXT1_0.uri,
                           "ingesting new mets 1.0 object");
        assertNotNull(pid);

        objectXML = apim.getObjectXML(pid);
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("foxml:digitalObject[@PID='" + pid + "']", xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property" +
                          "[@NAME='info:fedora/fedora-system:def/model#state' " +
                          "and @VALUE='Active']",
                          xmlIn);
        assertXpathExists("//foxml:objectProperties/foxml:property" +
                          "[@NAME='info:fedora/fedora-system:def/model#label' " +
                          "and @VALUE='Data Object (Coliseum) for Local Simple Image Demo']",
                          xmlIn);
        assertXpathEvaluatesTo("5", "count(//foxml:datastream[@ID!='AUDIT'])", xmlIn);

        // test exporting mets 1.0 object
        objectXML = apim.export("demo:999b", METS_EXT1_0.uri, "migrate");
        assertTrue(objectXML.length > 0);
        xmlIn = new String(objectXML, "UTF-8");
        assertXpathExists("METS:mets[@OBJID='demo:999b']", xmlIn);
        assertXpathExists("METS:mets[@LABEL='Data Object (Coliseum) for Local Simple Image Demo']", xmlIn);

        // purge mets 1.0 object
        apim.purgeObject(pid, "purging object demo:999b", false);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAPIM.class);
    }

}
