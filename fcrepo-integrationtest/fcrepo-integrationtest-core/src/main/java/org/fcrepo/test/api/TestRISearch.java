/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.PID;

import org.fcrepo.server.resourceIndex.UvaStdImgTripleGenerator_1;

import org.fcrepo.test.DemoObjectTestSetup;
import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.api.TestRESTAPI.ValidatorErrorHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import static org.fcrepo.test.api.RISearchUtil.checkSPOCount;


/**
 * Tests risearch functionality when the resource index is enabled.
 *
 * @author Chris Wilper
 */
public class TestRISearch
extends FedoraServerTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("TestRISearch TestSuite");
        suite.addTestSuite(TestRISearch.class);
        return new DemoObjectTestSetup(suite);
    }

    /**
     * Implicit relationship to Fedora object CModel
     * @throws Exception
     */
    public void testRISearchBasicCModel() throws Exception {
        FedoraClient client = getFedoraClient();
        for (String pid : new String[] { "demo:SmileyPens",
                "demo:SmileyPens_M",
                                         "demo:SmileyGreetingCard" }) {
            String query = "<" + PID.toURI(pid) + ">"
                        + " <" + Constants.MODEL.HAS_MODEL.uri + ">"
                        + " <" + Models.FEDORA_OBJECT_CURRENT.uri + ">";
            RISearchUtil.checkSPOCount(client, query, 1);
        }
    }

    /**
     * Check for SPARQL_W3C result format
     * 
     */
    public void testRISearchSparqlW3cResult() throws Exception{
    	FedoraClient client=getFedoraClient();
    	String query="select $object $modified from <#ri> where  " +
    			"$object <fedora-model:hasModel> " +
    			"<info:fedora/fedora-system:ServiceDefinition-3.0> and " +
    			"$object <fedora-view:lastModifiedDate> $modified";
    	String xml=client.getResponseAsString("/risearch?lang=itql&format=Sparql_W3C&query=" + URLEncoder.encode(query,"UTF-8"), true, true).trim();
    	validateXML(xml,this.getClass().getClassLoader().getResourceAsStream("schema/sparql/sparql_result.xsd"));
    }
    
    private void validateXML(String xml,java.io.InputStream schemaIn) throws Exception {
    	SchemaFactory sf=SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    	Schema schema=sf.newSchema(new StreamSource(schemaIn));
    	Validator validator=schema.newValidator();
    	StringBuilder errorBuilder=new StringBuilder();
    	try{
    		validator.validate(new StreamSource(new StringReader(xml)));
    	}catch (Exception e) {
    		e.printStackTrace();
    		fail("Error during validation of XML:\n" + e.getLocalizedMessage());
		}
    }


    /**
     * Explicit RELS-EXT relation to collection object
     * @throws Exception
     */
    public void testRISearchRelsExtCollection() throws Exception {
        FedoraClient client = getFedoraClient();
        String collectionPid = "demo:SmileyStuff";
        for (String pid : new String[] { "demo:SmileyPens",
                "demo:SmileyPens_M",
                                         "demo:SmileyGreetingCard" }) {
            String query = "<" + PID.toURI(pid) + ">"
                        + " <" + Constants.RELS_EXT.IS_MEMBER_OF.uri + ">"
                        + " <" + PID.toURI(collectionPid) + ">";
            RISearchUtil.checkSPOCount(client, query, 1);
        }
    }

    /**
     * RELS-INT relationships specifying image size for jpeg datastreams
     * @throws Exception
     */
    public void testRISearchRelsInt() throws Exception {
        FedoraClient client = getFedoraClient();
        for (String pid : new String[] { "demo:SmileyPens" ,
                "demo:SmileyPens_M",
                                         "demo:SmileyGreetingCard" }) {
            String query = "<" + PID.toURI(pid) + "/MEDIUM_SIZE" + ">"
                        + " <" + "http://ns.adobe.com/exif/1.0/PixelXDimension" + ">"
                        + " \"320\"";
            RISearchUtil.checkSPOCount(client, query, 1);
        }
    }
    
    /**
     * Test that Spring-configured triple generators are working
     */
    public void testSpringTripleGenerators() throws Exception {
        FedoraClient client = getFedoraClient();
        String query = "<info:fedora/demo:5>"
        + " <" + UvaStdImgTripleGenerator_1.TEST_PREDICATE + ">"
        + " \"true\"";
        checkSPOCount(client, query, 1);
    }
    
    /**
     * Test that RELS-EXT statements with a xml:lang attribute have
     * the language attribute propagated to the resource index
     * @throws Exception
     */
    public void testLanguageAttributes() throws Exception {
        FedoraClient client = getFedoraClient();
        // skos: <http://www.w3.org/2004/02/skos/core#>
        // skos:prefLabel \"Immagine del Colosseo a Roma\"
        String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                       "SELECT ?x FROM <#ri>\n" +
                       "WHERE { ?x skos:prefLabel \"Immagine del Colosseo a Roma\"@it }";
        RISearchUtil.checkSPARQLCount(client, query, 1);
    }
}
