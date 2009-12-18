/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.test;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.Set;

import org.custommonkey.xmlunit.XMLUnit;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;

import fedora.client.FedoraClient;
import fedora.client.search.SearchResultParser;
import fedora.client.utility.AutoPurger;
import fedora.client.utility.ingest.Ingest;
import fedora.client.utility.ingest.IngestCounter;

import fedora.common.Constants;

import fedora.server.management.FedoraAPIM;

/**
 * Base class for JUnit tests that assume a running Fedora instance.
 *
 * @author Edwin Shin
 */
public abstract class FedoraServerTestCase
        extends FedoraTestCase
        implements Constants {

    public FedoraServerTestCase() {
        super();
    }

    public FedoraServerTestCase(String name) {
        super(name);
    }

    /**
     * Returns the requested HTTP resource as an XML Document
     *
     * @param location
     *        a URL relative to the Fedora base URL
     * @return Document
     * @throws Exception
     */
    public Document getXMLQueryResult(String location) throws Exception {
        return getXMLQueryResult(getFedoraClient(), location);
    }

    public Document getXMLQueryResult(FedoraClient client, String location)
            throws Exception {
        InputStream is = client.get(getBaseURL() + location, true, true);
        Document result = XMLUnit.buildControlDocument(new InputSource(is));
        is.close();
        return result;
    }

    public static boolean testingMETS() {
        String format = System.getProperty("demo.format");
        return format != null && format.equalsIgnoreCase("mets");
    }

    public static boolean testingAtom() {
        String format = System.getProperty("demo.format");
        return format != null && format.equalsIgnoreCase("atom");
    }

    public static boolean testingAtomZip() {
        String format = System.getProperty("demo.format");
        return format != null && format.equalsIgnoreCase("atom-zip");
    }

    public static void ingestDemoObjects() throws Exception {
        ingestDemoObjects("/");
    }

    /**
     * Ingest a specific directory of demo objects.
     * <p>
     * Given a path relative to the format-independent demo object hierarchy,
     * will ingest all files in the hierarchy denoted by the path.
     * </p>
     * <h2>example</h2>
     * <p>
     * <code>ingestDemoObjects(local-server-demos)</code> will ingest all files
     * underneath the <code>client/demo/[format]/local-server-demos/</code>
     * hierarchy
     * </p>
     *
     * @param path
     *        format-independent path to a directory within the demo object
     *        hierarchy.
     * @throws Exception
     */
    public static void ingestDemoObjects(String path) throws Exception {
        File dir = null;

        String specificPath = File.separator + path;

        String ingestFormat;
        if (testingMETS()) {
            System.out.println("Ingesting demo objects in METS format from " + specificPath);
            dir = new File(FEDORA_HOME, "client/demo/mets" + specificPath);
            ingestFormat = METS_EXT1_1.uri;
        } else if (testingAtom()) {
            System.out.println("Ingesting demo objects in Atom format from " + specificPath);
            dir = new File(FEDORA_HOME, "client/demo/atom" + specificPath);
            ingestFormat = ATOM1_1.uri;
        } else if (testingAtomZip()) {
            System.out.println("Ingesting all demo objects in Atom Zip format from " + specificPath);
            dir = new File(FEDORA_HOME, "client/demo/atom-zip" + specificPath);
            ingestFormat = ATOM_ZIP1_1.uri;
        } else {
            System.out.println("Ingesting demo objects in FOXML format from " + specificPath);
            dir = new File(FEDORA_HOME, "client/demo/foxml" + specificPath);
            ingestFormat = FOXML1_1.uri;
        }

        FedoraClient client = FedoraTestCase.getFedoraClient();

        Ingest.multiFromDirectory(dir,
                                  ingestFormat,
                                  client.getAPIA(),
                                  client.getAPIM(),
                                  null,
                                  new PrintStream(File.createTempFile("demo",
                                                                      null)),
                                  new IngestCounter());
    }

    /**
     * Gets the PIDs of objects in the "demo" pid namespace that are in the
     * repository
     *
     * @return set of PIDs of the specified object type
     * @throws Exception
     */
    public static Set<String> getDemoObjects() throws Exception {

        FedoraClient client = getFedoraClient();
        InputStream queryResult;
        queryResult =
                client.get(getBaseURL() + "/search?query=pid~demo:*"
                        + "&maxResults=1000&pid=true&xml=true", true, true);
        SearchResultParser parser = new SearchResultParser(queryResult);

        return parser.getPIDs();
    }

    public static void purgeDemoObjects() throws Exception {
        FedoraClient client = getFedoraClient();
        FedoraAPIM apim = client.getAPIM();

        for (String pid : getDemoObjects()) {
            AutoPurger.purge(apim, pid, null, false);
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FedoraServerTestCase.class);
    }
}
