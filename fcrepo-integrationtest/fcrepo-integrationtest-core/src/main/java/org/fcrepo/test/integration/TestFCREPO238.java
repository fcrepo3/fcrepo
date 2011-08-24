package org.fcrepo.test.integration;

import java.io.FileInputStream;
import java.util.Date;

import org.fcrepo.common.xml.format.FedoraPIDList1_0Format;
import org.fcrepo.server.management.FedoraAPIMBindingSOAPHTTPImpl;
import org.fcrepo.test.FedoraTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamDef;
import org.fcrepo.server.types.gen.MIMETypedStream;

public class TestFCREPO238 extends FedoraTestCase {
	private FedoraAPIM apim;
	private FedoraAPIA apia;

	private static final String PID = "demo:fcrepo238";
	private static final byte[] FCREPO238_FOXML;

	static {
		StringBuilder xmlBuilder = new StringBuilder();
		xmlBuilder
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.append("<foxml:digitalObject PID=\"demo:5\" VERSION=\"1.1\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">")
				.append("<foxml:objectProperties>")
				.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/>")
				.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\"Data Object (Coliseum) for Local Simple Image Demo\"/>")
				.append("<foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/>")
				.append("</foxml:objectProperties>")
				.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"DC\" STATE=\"A\" VERSIONABLE=\"true\">")
				.append("<foxml:datastreamVersion FORMAT_URI=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ID=\"DC1.0\" LABEL=\"Dublin Core Record for this object\" MIMETYPE=\"text/xml\">")
				.append("<foxml:xmlContent>")
				.append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">")
				.append("<dc:title>Coliseum in Rome</dc:title>")
				.append("<dc:creator>Thornton Staples</dc:creator>")
				.append("<dc:subject>Architecture, Roman</dc:subject>")
				.append("<dc:description>Image of Coliseum in Rome</dc:description>")
				.append("<dc:publisher>University of Virginia Library</dc:publisher>")
				.append("<dc:format>image/jpeg</dc:format>")
				.append("<dc:identifier>demo:5</dc:identifier>")
				.append("</oai_dc:dc>")
				.append("</foxml:xmlContent>")
				.append("</foxml:datastreamVersion>")
				.append("</foxml:datastream>")
				.append("<foxml:datastream CONTROL_GROUP=\"X\" ID=\"RELS-EXT\" STATE=\"A\" VERSIONABLE=\"true\">")
				.append("<foxml:datastreamVersion FORMAT_URI=\"info:fedora/fedora-system:FedoraRELSExt-1.0\" ID=\"RELS-EXT1.0\" LABEL=\"RDF Statements about this object\" MIMETYPE=\"application/rdf+xml\">")
				.append("<foxml:xmlContent>")
				.append("<rdf:RDF xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">")
				.append("<rdf:Description rdf:about=\"info:fedora/demo:5\">")
				.append("<fedora-model:hasModel rdf:resource=\"info:fedora/demo:UVA_STD_IMAGE_1\"/>")
				.append("</rdf:Description>")
				.append("</rdf:RDF>")
				.append("</foxml:xmlContent>")
				.append("</foxml:datastreamVersion>")
				.append("</foxml:datastream>")
				.append("<foxml:datastream CONTROL_GROUP=\"E\" ID=\"THUMBRES_IMG\" STATE=\"A\" VERSIONABLE=\"true\">")
				.append("<foxml:datastreamVersion ID=\"THUMBRES_IMG1.0\" LABEL=\"Thorny's Coliseum thumbnail jpg image\" MIMETYPE=\"image/jpeg\">")
				.append("<foxml:contentLocation REF=\"http://local.fedora.server/fedora-demo/simple-image-demo/coliseum-thumb.jpg\" TYPE=\"INTERNAL_ID\"/>")
				.append("</foxml:datastreamVersion>")
				.append("</foxml:datastream>")
				.append("<foxml:datastream CONTROL_GROUP=\"E\" ID=\"MEDRES_IMG\" STATE=\"A\" VERSIONABLE=\"true\">")
				.append("<foxml:datastreamVersion ID=\"MEDRES_IMG1.0\" LABEL=\"Thorny's Coliseum medium jpg image\" MIMETYPE=\"image/jpeg\">")
				.append("<foxml:contentLocation REF=\"http://local.fedora.server/fedora-demo/simple-image-demo/coliseum-medium.jpg\" TYPE=\"INTERNAL_ID\"/>")
				.append("</foxml:datastreamVersion>")
				.append("</foxml:datastream>")
				.append("<foxml:datastream CONTROL_GROUP=\"E\" ID=\"HIGHRES_IMG\" STATE=\"A\" VERSIONABLE=\"true\">")
				.append("<foxml:datastreamVersion ID=\"HIGHRES_IMG1.0\" LABEL=\"Thorny's Coliseum high jpg image\" MIMETYPE=\"image/jpeg\">")
				.append("<foxml:contentLocation REF=\"http://local.fedora.server/fedora-demo/simple-image-demo/coliseum-high.jpg\" TYPE=\"INTERNAL_ID\"/>")
				.append("</foxml:datastreamVersion>")
				.append("</foxml:datastream>")
				.append("<foxml:datastream CONTROL_GROUP=\"E\" ID=\"VERYHIGHRES_IMG\" STATE=\"A\" VERSIONABLE=\"true\">")
				.append("<foxml:datastreamVersion ID=\"VERYHIGHRES_IMG1.0\" LABEL=\"Thorny's Coliseum veryhigh jpg image\" MIMETYPE=\"image/jpeg\">")
				.append("<foxml:contentLocation REF=\"http://local.fedora.server/fedora-demo/simple-image-demo/coliseum-veryhigh.jpg\" TYPE=\"INTERNAL_ID\"/>")
				.append("</foxml:datastreamVersion>").append("</foxml:datastream>").append("</foxml:digitalObject>");
		FCREPO238_FOXML = xmlBuilder.toString().getBytes();

	}

	@Before
	public void setup() {
		apia = getFedoraClient().getAPIA();
		apim = getFedoraClient().getAPIM();
	}

	@After
	public void tearDown() {

	}

	@Test
	public void testfcrepo238() {
		apim.ingest(FCREPO238_FOXML, FOXML1_1.uri, "fcrepo 238 testobject");
		ObjectProfile profile = apia.getObjectProfile(PID, null);
		Date lastMod = profile.objectLastModDate;
		DatastreamDef[] streamDefs = apia.listDatastreams(PID, null);
		System.out.println("checking fcrepo 238");
		for (DatastreamDef def : streamDefs) {
			Datastream ds = apim.getDatastream(PID, def.dsID, null);
			assertTrue(ds.DSCreateDT.compareTo(lastMod) >= 0);
			System.out.println(ds.DatastreamID + ": " + ds.DSLabel + " ok.");
		}
	}

}
