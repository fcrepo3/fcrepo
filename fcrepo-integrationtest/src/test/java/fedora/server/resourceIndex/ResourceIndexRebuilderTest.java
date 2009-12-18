/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.resourceIndex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.net.ConnectException;
import java.net.Socket;

import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fedora.common.Constants;
import fedora.common.PID;

import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;
import fedora.server.management.FedoraAPIM;
import fedora.server.utilities.ServerUtility;
import fedora.server.utilities.rebuild.Rebuild;
import fedora.server.utilities.rebuild.Rebuilder;

import fedora.test.FedoraTestCase;

import fedora.utilities.ExecUtility;
import fedora.utilities.Foxml11Document;
import fedora.utilities.Foxml11Document.ControlGroup;
import fedora.utilities.Foxml11Document.Property;
import fedora.utilities.Foxml11Document.State;

/**
 * Test of the ResourceIndexRebuilder. Requires that Fedora already be installed.
 * Note that this is a long-running test because of the ingest & purge cycle.
 * 
 * @author Edwin Shin
 * @since 3.0
 * @version $Id: ResourceIndexRebuilderTest.java 7508 2008-07-15 04:00:43Z pangloss $
 */
public class ResourceIndexRebuilderTest {

    private FedoraAPIM apim;

    private String osName;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        osName = System.getProperty("os.name");

        if (!isTomcatRunning()) {
            startTomcat();
        }
        apim = FedoraTestCase.getFedoraClient().getAPIM();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testRebuild() throws Exception {
        int count = 10;
        rebuild();
        try {
            ingestObjects(count);

            // shutdown tomcat
            shutdownTomcat();

            // rebuild
            rebuild();

            // start tomcat
            startTomcat();
        } finally {
            if (!isTomcatRunning()) {
                startTomcat();
            }
            purgeObjects(count);
        }
    }

    private void rebuild() throws Exception {
        Rebuilder rebuilder;
        File serverDir = new File(new File(Constants.FEDORA_HOME), "server");
        ServerConfigurationParser parser =
                new ServerConfigurationParser(new FileInputStream(new File(serverDir,
                                                                           "config/fedora.fcfg")));
        ServerConfiguration serverConfig = parser.parse();
        rebuilder = new ResourceIndexRebuilder();
        Map<String, String> options = rebuilder.init(serverDir, serverConfig);

        try {
            new Rebuild(rebuilder, options, serverConfig);
        } catch (Exception e) {
            e.printStackTrace();
            junit.framework.Assert.fail(e.getMessage());
        }
    }

    private void startTomcat() throws Exception {
        String cmd = Constants.FEDORA_HOME + "/tomcat/bin/startup";

        if (!osName.startsWith("Windows")) {
            cmd += ".sh";
        }
        ExecUtility.execCommandLineUtility(cmd);
        System.out.print("Starting Tomcat ");
        int count = 0;
        int timeout = 1000 * 30;
        while (!isFedoraRunning()) {
            System.out.print(".");
            Thread.sleep(1000);
            count += 1000;
            if (count > timeout) {
                throw new RuntimeException("Tomcat startup timeout");
            }
        }
        System.out.println();
    }

    private boolean isFedoraRunning() throws Exception {
        return ServerUtility.pingServer("http", "fedoraAdmin", "fedoraAdmin");
    }

    private boolean isTomcatRunning() throws Exception {
        Socket socket = null;
        try {
            socket = new Socket("localhost", 8080);
            return socket.isConnected();
        } catch (ConnectException e) {
            return false;
        }
    }

    private void shutdownTomcat() throws Exception {
        if (!isTomcatRunning()) {
            System.out.println("Tomcat was already shut down.");
            return;
        }
        
        String cmd = Constants.FEDORA_HOME + "/tomcat/bin/shutdown";

        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            cmd += ".sh";
        }

        System.out.print("Shutting down Tomcat ");

        ExecUtility.execCommandLineUtility(cmd);

        int count = 0;
        int timeout = 1000 * 30;

        while (isTomcatRunning()) {
            System.out.print(".");
            Thread.sleep(1000);
            count += 1000;
            if (count > timeout) {
                throw new RuntimeException("Tomcat shutdown timeout");
            }
        }
        Thread.sleep(5000);
        System.out.println();
    }

    private void ingestObjects(int count) throws Exception {
        String url = "http://www.fedora-commons.org/";
        System.out.print("ingesting " + count + " objects ");
        for (int i = 0; i < count; i++) {
            String pid = String.format("demo:ri%s", i);
            apim.ingest(getFoxmlObject(pid, url), Constants.FOXML1_1.uri, null);
            if (i % 100 == 0) {
                System.out.print("\n\t");
            }
            System.out.print(".");
        }
        System.out.println();
    }

    private void purgeObjects(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            String pid = String.format("demo:ri%s", i);
            apim.purgeObject(pid, null, false);
        }
    }

    private byte[] getFoxmlObject(String pid, String contentLocation)
            throws Exception {
        Foxml11Document doc = createFoxmlObject(pid, contentLocation);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.serialize(out);
        return out.toByteArray();
    }

    private Foxml11Document createFoxmlObject(String spid,
                                              String contentLocation)
            throws Exception {
        PID pid = PID.getInstance(spid);
        Date date = new Date(1);

        Foxml11Document doc = new Foxml11Document(pid.toString());
        doc.addObjectProperty(Property.STATE, "A");

        if (contentLocation != null && contentLocation.length() > 0) {
            String ds = "DS";
            String dsv = "DS1.0";
            doc.addDatastream(ds, State.A, ControlGroup.E, true);
            doc.addDatastreamVersion(ds, dsv, "text/html", "label", 1, date);
            doc.setContentLocation(dsv, contentLocation, "URL");
        }
        return doc;
    }
}
