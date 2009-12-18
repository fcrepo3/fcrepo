/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.awt.Dimension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import fedora.server.utilities.StreamUtility;

/**
 * A client for performing HTTP GET requests on a Fedora server (with
 * authentication) or any other server (without authentication). Each kind of
 * request can either request an InputStream or request that the Downloader
 * write the content directly to a provided OutputStream.
 *
 * @author Chris Wilper
 */
public class Downloader {

    private final MultiThreadedHttpConnectionManager m_cManager =
            new MultiThreadedHttpConnectionManager();

    private final String m_fedoraUrlStart;

    private final AuthScope m_authScope;

    private final UsernamePasswordCredentials m_creds;

    /**
     * Construct a downloader for a certain repository as a certain user.
     */
    public Downloader(String host,
                      int port,
                      String context,
                      String user,
                      String pass)
            throws IOException {
        m_fedoraUrlStart =
                Administrator.getProtocol() + "://" + host + ":" + port + "/"
                        + context + "/" + "get/";
        m_authScope =
                new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
        m_creds = new UsernamePasswordCredentials(user, pass);
    }

    public void getDatastreamContent(String pid,
                                     String dsID,
                                     String asOfDateTime,
                                     OutputStream out) throws IOException {
        InputStream in = getDatastreamContent(pid, dsID, asOfDateTime);
        StreamUtility.pipeStream(in, out, 4096);
    }

    public InputStream getDatastreamContent(String pid,
                                            String dsID,
                                            String asOfDateTime)
            throws IOException {
        StringBuffer buf = new StringBuffer();
        buf.append(m_fedoraUrlStart);
        buf.append(pid);
        buf.append('/');
        buf.append(dsID);
        if (asOfDateTime != null) {
            buf.append('/');
            buf.append(asOfDateTime);
        }
        return get(buf.toString());
    }

    public void getDatastreamDissemination(String pid,
                                           String dsId,
                                           String asOfDateTime,
                                           OutputStream out) throws IOException {
        InputStream in = getDatastreamDissemination(pid, dsId, asOfDateTime);
        StreamUtility.pipeStream(in, out, 4096);
    }

    public InputStream getDatastreamDissemination(String pid,
                                                  String dsId,
                                                  String asOfDateTime)
            throws IOException {
        StringBuffer buf = new StringBuffer();
        buf.append(m_fedoraUrlStart);
        buf.append(pid);
        buf.append('/');
        buf.append(dsId);
        if (asOfDateTime != null) {
            buf.append('/');
            buf.append(asOfDateTime);
        }
        return get(buf.toString());
    }

    /**
     * Get data via HTTP and write it to an OutputStream, following redirects,
     * and supplying credentials if the host is the Fedora server.
     */
    public void get(String url, OutputStream out) throws IOException {
        InputStream in = get(url);
        StreamUtility.pipeStream(in, out, 4096);
    }

    /**
     * Get data via HTTP as an InputStream, following redirects, and supplying
     * credentials if the host is the Fedora server.
     */
    public InputStream get(String url) throws IOException {
        GetMethod get = null;
        boolean ok = false;
        try {
            m_cManager.getParams().setConnectionTimeout(20000);
            HttpClient client = new HttpClient(m_cManager);
            client.getState().setCredentials(m_authScope, m_creds);
            client.getParams().setAuthenticationPreemptive(true); // don't bother with challenges
            int redirectCount = 0; // how many redirects did we follow
            int resultCode = 300; // not really, but enter the loop that way
            Dimension d = null;
            while (resultCode > 299 && resultCode < 400 && redirectCount < 25) {
                get = new GetMethod(url);
                get.setDoAuthentication(true);
                get.setFollowRedirects(true);
                if (Administrator.INSTANCE != null) {
                    d = Administrator.PROGRESS.getSize();
                    // if they're using Administrator, tell them we're downloading...
                    Administrator.PROGRESS.setString("Downloading " + url
                            + " . . .");
                    Administrator.PROGRESS.setValue(100);
                    Administrator.PROGRESS.paintImmediately(0, 0, (int) d
                            .getWidth() - 1, (int) d.getHeight() - 1);
                }
                resultCode = client.executeMethod(get);
                if (resultCode > 299 && resultCode < 400) {
                    redirectCount++;
                    url = get.getResponseHeader("Location").getValue();
                }
            }
            if (resultCode != 200) {
                System.err.println(get.getResponseBodyAsString());
                throw new IOException("Server returned error: " + resultCode
                        + " " + HttpStatus.getStatusText(resultCode));
            }
            ok = true;
            if (Administrator.INSTANCE != null) {
                // cache it to a file
                File tempFile =
                        File.createTempFile("fedora-client-download-", null);
                tempFile.deleteOnExit();
                HashMap PARMS = new HashMap();
                PARMS.put("in", get.getResponseBodyAsStream());
                PARMS.put("out", new FileOutputStream(tempFile));
                // do the actual download in a safe thread
                SwingWorker worker = new SwingWorker(PARMS) {

                    @Override
                    public Object construct() {
                        try {
                            StreamUtility.pipeStream((InputStream) parms
                                                             .get("in"),
                                                     (OutputStream) parms
                                                             .get("out"),
                                                     8192);
                        } catch (Exception e) {
                            thrownException = e;
                        }
                        return "";
                    }
                };
                worker.start();
                // The following code will run in the (safe)
                // Swing event dispatcher thread.
                int ms = 200;
                while (!worker.done) {
                    try {
                        Administrator.PROGRESS.setValue(ms);
                        Administrator.PROGRESS.paintImmediately(0, 0, (int) d
                                .getWidth() - 1, (int) d.getHeight() - 1);
                        Thread.sleep(100);
                        ms = ms + 100;
                        if (ms >= 2000) {
                            ms = 200;
                        }
                    } catch (InterruptedException ie) {
                    }
                }
                if (worker.thrownException != null) {
                    throw worker.thrownException;
                }
                Administrator.PROGRESS.setValue(2000);
                Administrator.PROGRESS.paintImmediately(0, 0, (int) d
                        .getWidth() - 1, (int) d.getHeight() - 1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                }
                return new FileInputStream(tempFile);
            }
            return get.getResponseBodyAsStream();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            if (get != null && !ok) {
                get.releaseConnection();
            }
            if (Administrator.INSTANCE != null) {
                Administrator.PROGRESS.setValue(0);
                Administrator.PROGRESS.setString("");
            }
        }

    }

    /**
     * Test this class.
     */
    public static void main(String[] args) {
        try {
            if (args.length == 8 || args.length == 9) {
                String host = args[0];
                int port = Integer.parseInt(args[1]);
                String user = args[2];
                String password = args[3];
                String pid = args[4];
                String dsid = args[5];
                File outfile = new File(args[6]);
                String asOfDateTime = args.length == 8 ? args[7] : null;
                String context = args.length == 9 ? args[8] : null;

                FileOutputStream outStream = new FileOutputStream(outfile);
                Downloader downloader =
                        new Downloader(host, port, context, user, password);

                downloader.getDatastreamContent(pid,
                                                dsid,
                                                asOfDateTime,
                                                outStream);
            } else {
                System.err
                        .println("Usage: Downloader host port user password pid dsid outfile [MMDDYYTHH:MM:SS] [context]");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: " + e.getMessage());
        }
    }

}