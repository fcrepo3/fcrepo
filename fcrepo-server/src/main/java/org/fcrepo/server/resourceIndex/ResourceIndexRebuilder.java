/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.resourceIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;

import org.trippi.TriplestoreConnector;
import org.trippi.impl.mulgara.MulgaraConnector;

import org.fcrepo.server.Module;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.config.Parameter;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.errors.ResourceIndexException;
import org.fcrepo.server.storage.SimpleDOReader;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.utilities.rebuild.Rebuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;



/**
 * A Rebuilder for the resource index.
 */
public class ResourceIndexRebuilder
        implements ApplicationContextAware, Rebuilder {

    private static Logger logger = LoggerFactory.getLogger(ResourceIndexRebuilder.class.getName());

    private static final String moduleName = "org.fcrepo.server.resourceIndex.ResourceIndex";
    private static final String configName = "org.fcrepo.server.resourceIndex.ResourceIndexConfiguration";

    private ModuleConfiguration m_riConfig;

    private ApplicationContext m_context;

    private ResourceIndex m_ri;

    private TriplestoreConnector m_conn;

    private TripleGenerator m_generator;

    public ResourceIndexRebuilder(){

    }

    @Resource(name = "org.trippi.TriplestoreConnector")
    public void setTriplestoreConnector(TriplestoreConnector conn){
        m_conn = conn;
    }

    @Resource(name = configName)
    public void setModuleConfiguration(ModuleConfiguration riConfig) {
        m_riConfig = riConfig;
    }

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
    public void setServerConfiguration(ServerConfiguration serverConfig){
        // not needed
    }

    public void setServerDir(File serverBaseDir) {
        // not needed
    }

    public void init() {

    }

    public Map<String, String> getOptions() {
        Map<String, String> m = new HashMap<String, String>();
        return m;
    }

    @Resource(name = "org.fcrepo.server.resourceIndex.ModelBasedTripleGenerator")
    public void setTripleGenerator(TripleGenerator generator) {
        m_generator = generator;
    }

    /**
     * Validate the provided options and perform any necessary startup tasks.
     */
    public void start(Map<String, String> options)
    throws ResourceIndexException {
        // validate options

        // do startup tasks

        String levelValue;
        if (m_riConfig == null){ //must have been configured outside fcfg
            Module riModule = m_context.getBean(moduleName,Module.class);
            if (riModule != null){
                logger.warn("ModuleConfiguration bean unavailable; getting Module bean");
                levelValue = riModule.getParameter("level");
            }
            else {
                logger.error("Cannot load ResourceIndex module definition from Spring config or Fedora config");
                throw new ResourceIndexException("Cannot locate ResourceIndex module definition in Spring config or Fedora config");
            }
        }
        else {
            levelValue = m_riConfig.getParameter("level",Parameter.class).getValue();
        }
        int riLevel = Integer.parseInt(levelValue);

        Map<String, String> aliasMap = new HashMap<String, String>();
        Iterator<Parameter> it = m_riConfig.getParameters(Parameter.class).iterator();
        Parameter p;
        while (it.hasNext()) {
            p = it.next();
            String pName = p.getName();
            String[] parts = pName.split(":");
            if (parts.length == 2 && parts[0].equals("alias")) {
                aliasMap.put(parts[1], p.getValue(p.getIsFilePath()));
            }
        }


        System.out.println("Initializing triplestore interface...");
        try {
            if (m_conn instanceof MulgaraConnector){
                String path = m_conn.getConfiguration().get("path");
                dropIndex(path);
            }

            m_ri = new ResourceIndexImpl(m_conn, m_generator, riLevel, false);
            m_ri.setAliasMap(aliasMap);
        } catch (Exception e) {
            logger.error("Failed to initialize new Resource Index",e);
            e.printStackTrace(System.err);
            throw new ResourceIndexException("Failed to initialize new Resource Index",
                                             e);
        }

    }

    private void dropIndex(String tsPath){
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

    @Override
    public void setApplicationContext(ApplicationContext context)
            throws BeansException {
        m_context = context;

    }

}
