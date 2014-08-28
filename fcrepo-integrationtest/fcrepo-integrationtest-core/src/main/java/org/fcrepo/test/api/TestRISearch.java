/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static junit.framework.Assert.fail;
import static org.fcrepo.test.api.RISearchUtil.checkSPOCount;

import java.io.StringReader;
import java.net.URLEncoder;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import junit.framework.JUnit4TestAdapter;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.PID;
import org.fcrepo.server.resourceIndex.UvaStdImgTripleGenerator_1;
import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.ManagedContentTranslator;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;


/**
 * Tests risearch functionality when the resource index is enabled.
 *
 * @author Chris Wilper
 */
public class TestRISearch
extends FedoraServerTestCase {

    private static FedoraClient s_client;
    
    private static final String[] SMILEYS =
        new String[] {
            "demo:SmileyPens",
            "demo:SmileyPens_M",
            "demo:SmileyGreetingCard" };

    private static String ri_impl;
    
    @BeforeClass
    public static void bootStrap() throws Exception {
        s_client = getFedoraClient();
        ingestSimpleImageDemoObjects(s_client);
        ingestImageCollectionDemoObjects(s_client);
        ri_impl = getRIImplementation();
        // clone some demo objects to managed-content equivalents for reserved datastreams (RELS-*, DC)
        ManagedContentTranslator.createManagedClone(s_client.getAPIMMTOM(), "demo:SmileyPens", "demo:SmileyPens_M");
        ManagedContentTranslator.createManagedClone(s_client.getAPIMMTOM(), "demo:SmileyBeerGlass", "demo:SmileyBeerGlass_M");
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        purgeDemoObjects(s_client);
        s_client.shutdown();
    }

    /**
     * Implicit relationship to Fedora object CModel
     * @throws Exception
     */
    @Test
    public void testRISearchBasicCModel() throws Exception {
        for (String pid : SMILEYS) {
            String query = "<" + PID.toURI(pid) + ">"
                    + " <" + Constants.MODEL.HAS_MODEL.uri + ">"
                    + " <" + Models.FEDORA_OBJECT_CURRENT.uri + ">";
            RISearchUtil.checkSPOCount(s_client, query, 1);
        }
    }

    /**
     * Check for SPARQL_W3C result format
     * 
     */
    @Test
    public void testRISearchSparqlW3cResult() throws Exception{
        /* skip if MPTTriplestore implementation */
        Assume.assumeTrue(! "localPostgresMPTTriplestore".equals(ri_impl));
        
    	String query="select $object $modified from <#ri> where  " +
    			"$object <fedora-model:hasModel> " +
    			"<info:fedora/fedora-system:ServiceDefinition-3.0> and " +
    			"$object <fedora-view:lastModifiedDate> $modified";
    	String xml=s_client.getResponseAsString("/risearch?lang=itql&format=Sparql_W3C&query=" + URLEncoder.encode(query,"UTF-8"), true, true).trim();
    	validateXML(xml,this.getClass().getClassLoader().getResourceAsStream("schema/sparql/sparql_result.xsd"));
    }
    
    private void validateXML(String xml,java.io.InputStream schemaIn) throws Exception {
    	SchemaFactory sf =
    	    SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    	Schema schema = sf.newSchema(new StreamSource(schemaIn));
    	Validator validator = schema.newValidator();
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
    @Test
    public void testRISearchRelsExtCollection() throws Exception {
        String collectionPid = "demo:SmileyStuff";
        for (String pid : SMILEYS) {
            String query = "<" + PID.toURI(pid) + ">"
                    + " <" + Constants.RELS_EXT.IS_MEMBER_OF.uri + ">"
                    + " <" + PID.toURI(collectionPid) + ">";
            RISearchUtil.checkSPOCount(s_client, query, 1);
        }
    }

    /**
     * RELS-INT relationships specifying image size for jpeg datastreams
     * @throws Exception
     */
    @Test
    public void testRISearchRelsInt() throws Exception {
        for (String pid : SMILEYS) {
            String query = "<" + PID.toURI(pid) + "/MEDIUM_SIZE" + ">"
                    + " <" + "http://ns.adobe.com/exif/1.0/PixelXDimension" + ">"
                    + " \"320\"";
            RISearchUtil.checkSPOCount(s_client, query, 1);
        }
    }
    
    /**
     * Test that Spring-configured triple generators are working
     */
    @Test
    public void testSpringTripleGenerators() throws Exception {
        String query = "<info:fedora/demo:5>"
        + " <" + UvaStdImgTripleGenerator_1.TEST_PREDICATE + ">"
        + " \"true\"";
        checkSPOCount(s_client, query, 1);
    }
    
    /**
     * Test that RELS-EXT statements with a xml:lang attribute have
     * the language attribute propagated to the resource index
     * @throws Exception
     */
    @Test
    public void testLanguageAttributes() throws Exception {
        // skos: <http://www.w3.org/2004/02/skos/core#>
        // skos:prefLabel \"Immagine del Colosseo a Roma\"
        
        /* skip if MPTTriplestore implementation */
        Assume.assumeTrue(! "localPostgresMPTTriplestore".equals(ri_impl));
        
        String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                       "SELECT ?x FROM <#ri>\n" +
                       "WHERE { ?x skos:prefLabel \"Immagine del Colosseo a Roma\"@it }";
        RISearchUtil.checkSPARQLCount(s_client, query, 1);
    }

    // Supports legacy test runners

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestRISearch.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestRISearch.class);
    }
}
