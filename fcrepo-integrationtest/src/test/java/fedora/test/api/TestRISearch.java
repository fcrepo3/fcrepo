/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test.api;

import junit.framework.Test;
import junit.framework.TestSuite;

import fedora.client.FedoraClient;

import fedora.common.Constants;
import fedora.common.Models;
import fedora.common.PID;

import fedora.test.DemoObjectTestSetup;
import fedora.test.FedoraServerTestCase;

import static fedora.test.api.RISearchUtil.checkSPOCount;

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
                                         "demo:SmileyGreetingCard" }) {
            String query = "<" + PID.toURI(pid) + ">"
                        + " <" + Constants.MODEL.HAS_MODEL.uri + ">"
                        + " <" + Models.FEDORA_OBJECT_CURRENT.uri + ">";
            checkSPOCount(client, query, 1);
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
                                         "demo:SmileyGreetingCard" }) {
            String query = "<" + PID.toURI(pid) + ">"
                        + " <" + Constants.RELS_EXT.IS_MEMBER_OF.uri + ">"
                        + " <" + PID.toURI(collectionPid) + ">";
            checkSPOCount(client, query, 1);
        }
    }

    /**
     * RELS-INT relationships specifying image size for jpeg datastreams
     * @throws Exception
     */
    public void testRISearchRelsInt() throws Exception {
        FedoraClient client = getFedoraClient();
        for (String pid : new String[] { "demo:SmileyPens" ,
                                         "demo:SmileyGreetingCard" }) {
            String query = "<" + PID.toURI(pid) + "/MEDIUM_SIZE" + ">"
                        + " <" + "http://ns.adobe.com/exif/1.0/PixelXDimension" + ">"
                        + " \"320\"";
            checkSPOCount(client, query, 1);
        }
    }
}
