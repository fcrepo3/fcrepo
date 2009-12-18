/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.trippi.TriplestoreConnector;

import fedora.server.config.DatastoreConfiguration;
import fedora.server.config.ModuleConfiguration;
import fedora.server.config.Parameter;
import fedora.server.config.ServerConfiguration;
import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.SimpleDOReader;
import fedora.server.storage.types.DigitalObject;
import fedora.server.utilities.rebuild.Rebuilder;

/**
 * A Rebuilder for the resource index.
 */
public class ResourceIndexRebuilder
        implements Rebuilder {

    private ServerConfiguration m_serverConfig;

    private ResourceIndex m_ri;

    private TriplestoreConnector m_conn;

    /**
     * Get a short phrase describing what the user can do with this rebuilder.
     */
    public String getAction() {
        return "Rebuild the Resource Index.";
    }

    /**
     * Returns true is the server _must_ be shut down for this rebuilder to
     * safely operate.
     */
    public boolean shouldStopServer() {
        return true;
    }

    /**
     * Initialize the rebuilder, given the server configuration.
     * 
     * @returns a map of option names to plaintext descriptions.
     */
    public Map<String, String> init(File serverDir,
                                    ServerConfiguration serverConfig) {
        m_serverConfig = serverConfig;
        Map<String, String> m = new HashMap<String, String>();

        return m;
    }

    /**
     * Validate the provided options and perform any necessary startup tasks.
     */
    public void start(Map<String, String> options)
            throws ResourceIndexException {
        // validate options

        // do startup tasks
        ModuleConfiguration riMC =
                m_serverConfig
                        .getModuleConfiguration("fedora.server.resourceIndex.ResourceIndex");
        int riLevel = Integer.parseInt(riMC.getParameter("level").getValue());
        String riDatastore = riMC.getParameter("datastore").getValue();
        DatastoreConfiguration tsDC =
                m_serverConfig.getDatastoreConfiguration(riDatastore);
        String tsConnector = tsDC.getParameter("connectorClassName").getValue();

        String tsPath = null;
        if (tsConnector.equals("org.trippi.impl.mulgara.MulgaraConnector")) {
            Parameter remoteParm = tsDC.getParameter("remote");
            if (remoteParm != null
                    && remoteParm.getValue().equalsIgnoreCase("false")) {
                tsPath = tsDC.getParameter("path").getValue(true);
            }
        }

        Iterator<Parameter> it;
        Parameter p;

        Map<String, String> tsTC = new HashMap<String, String>();
        it = tsDC.getParameters().iterator();
        while (it.hasNext()) {
            p = it.next();
            tsTC.put(p.getName(), p.getValue(p.getIsFilePath()));
        }

        Map<String, String> aliasMap = new HashMap<String, String>();
        it = riMC.getParameters().iterator();
        while (it.hasNext()) {
            p = it.next();
            String pName = p.getName();
            String[] parts = pName.split(":");
            if (parts.length == 2 && parts[0].equals("alias")) {
                aliasMap.put(parts[1], p.getValue(p.getIsFilePath()));
            }
        }

        if (tsPath == null) {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(System.in));
            System.out.println();
            System.out
                    .println("NOTE: You must now manually re-initialize (clear) ");
            System.out
                    .println("      the existing triplestore.  The RI rebuilder");
            System.out
                    .println("      cannot yet automatically perform this step ");
            System.out
                    .println("      for this type of triplestore.  Press enter");
            System.out.println("      when finished.");
            try {
                reader.readLine();
            } catch (IOException e) {
            }
            System.out.println("OK, continuing...");
        } else {
            System.out.println("Clearing directory " + tsPath + "...");
            deleteDirectory(tsPath);
            File cleanDir = new File(tsPath);
            cleanDir.mkdir();
        }

        System.out.println("Initializing triplestore interface...");
        try {
            m_conn = TriplestoreConnector.init(tsConnector, tsTC);

            TripleGenerator generator = new ModelBasedTripleGenerator();

            m_ri = new ResourceIndexImpl(m_conn, generator, riLevel, false);
            m_ri.setAliasMap(aliasMap);
        } catch (Exception e) {
            throw new ResourceIndexException("Failed to initialize new Resource Index",
                                             e);
        }
    }

    /**
     * Add the data of interest for the given object.
     * 
     * @throws ResourceIndexException
     */
    public void addObject(DigitalObject obj) throws ResourceIndexException {
        m_ri.addObject(new SimpleDOReader(null, null, null, null, null, obj));
    }

    /**
     * Free up any system resources associated with rebuilding.
     */
    public void finish() throws Exception {
        if (m_ri != null) {
            m_ri.flushBuffer();
            m_ri.close();
        }
    }

    private boolean deleteDirectory(String directory) {

        boolean result = false;

        if (directory != null) {
            File file = new File(directory);
            if (file.exists() && file.isDirectory()) {
                // 1. delete content of directory:
                File[] files = file.listFiles();
                result = true; //init result flag
                int count = files.length;
                for (int i = 0; i < count; i++) { //for each file:
                    File f = files[i];
                    if (f.isFile()) {
                        result = result && f.delete();
                    } else if (f.isDirectory()) {
                        result = result && deleteDirectory(f.getAbsolutePath());
                    }
                }//next file

                file.delete(); //finally delete (empty) input directory
            }//else: input directory does not exist or is not a directory
        }//else: no input value

        return result;
    }//deleteDirectory()
}
