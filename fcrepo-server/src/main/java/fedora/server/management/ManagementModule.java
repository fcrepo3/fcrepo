/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.management;

import java.io.File;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import fedora.server.Context;
import fedora.server.Module;
import fedora.server.Server;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ModuleShutdownException;
import fedora.server.errors.ServerException;
import fedora.server.proxy.AbstractInvocationHandler;
import fedora.server.proxy.ProxyFactory;
import fedora.server.security.Authorization;
import fedora.server.storage.DOManager;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.RelationshipTuple;

/**
 * @author Edwin Shin
 * @version $Id$
 */
public class ManagementModule
        extends Module
        implements Management, ManagementDelegate {

    /** Logger for this class. */
    private static Logger LOG =
            Logger.getLogger(ManagementModule.class.getName());

    private Authorization m_fedoraXACMLModule;

    private DOManager m_manager;

    private ExternalContentManager m_contentManager;

    private int m_uploadStorageMinutes;

    private int m_lastId;

    private File m_tempDir;

    private Hashtable<String, Long> m_uploadStartTime;

    private Management mgmt;
    
    private AbstractInvocationHandler[] invocationHandlers;
    
    /** Delay between purge of two uploaded files. */
    private long m_purgeDelayInMillis;

    public ManagementModule(Map<String, String> moduleParameters,
                            Server server,
                            String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
    }

    @Override
    public void initModule() throws ModuleInitializationException {

        // how many minutes should we hold on to uploaded files? default=5
        String min = getParameter("uploadStorageMinutes");
        if (min == null) {
            min = "5";
        }
        try {
            m_uploadStorageMinutes = Integer.parseInt(min);
            if (m_uploadStorageMinutes < 1) {
                throw new ModuleInitializationException("uploadStorageMinutes "
                        + "must be 1 or more, if specified.", getRole());
            }
        } catch (NumberFormatException nfe) {
            throw new ModuleInitializationException("uploadStorageMinutes must "
                                                            + "be an integer, if specified.",
                                                    getRole());
        }
        // initialize storage area by 1) ensuring the directory is there
        // and 2) reading in the existing files, if any, and setting their
        // startTime to the current time.
        try {
            m_tempDir = new File(getServer().getHomeDir(), "management/upload");
            if (!m_tempDir.isDirectory()) {
                m_tempDir.mkdirs();
            }
            // put leftovers in hash, while saving highest id as m_lastId
            m_uploadStartTime = new Hashtable<String, Long>();
            String[] fNames = m_tempDir.list();
            Long leftoverStartTime = new Long(System.currentTimeMillis());
            m_lastId = 0;
            for (String element : fNames) {
                try {
                    int id = Integer.parseInt(element);
                    if (id > m_lastId) {
                        m_lastId = id;
                    }
                    m_uploadStartTime.put(element, leftoverStartTime);
                } catch (NumberFormatException nfe) {
                    // skip files that aren't named numerically
                }
            }
        } catch (Exception e) {
            throw new ModuleInitializationException("Error while initializing "
                    + "temporary storage area: " + e.getClass().getName()
                    + ": " + e.getMessage(), getRole(), e);
        }

        // initialize variables pertaining to checksumming datastreams.
        String auto = getParameter("autoChecksum");
        LOG.debug("Got Parameter: autoChecksum = " + auto);
        if (auto.equalsIgnoreCase("true")) {
            Datastream.autoChecksum = true;
            Datastream.defaultChecksumType = getParameter("checksumAlgorithm");
        }
        LOG.debug("autoChecksum is " + auto);
        LOG.debug("defaultChecksumType is " + Datastream.defaultChecksumType);
       
        // get delay between purge of two uploaded files (default 1 minute)
        String purgeDelayInMillis = getParameter("purgeDelayInMillis");
        if (purgeDelayInMillis == null) {
          purgeDelayInMillis = "60000";
        }
        try {
            this.m_purgeDelayInMillis = Integer.parseInt(purgeDelayInMillis);
        } catch (NumberFormatException nfe) {
            throw new ModuleInitializationException(
                "purgeDelayInMillis must be an integer, if specified.",
                getRole());
        }
    }
    
    @Override
    public void postInitModule() throws ModuleInitializationException {
        // Verify required modules have been loaded
        m_manager =
                (DOManager) getServer()
                        .getModule("fedora.server.storage.DOManager");
        if (m_manager == null) {
            throw new ModuleInitializationException("Can't get a DOManager "
                    + "from Server.getModule", getRole());
        }
        m_contentManager =
                (ExternalContentManager) getServer()
                        .getModule("fedora.server.storage.ExternalContentManager");
        if (m_contentManager == null) {
            throw new ModuleInitializationException("Can't get an ExternalContentManager "
                                                            + "from Server.getModule",
                                                    getRole());
        }

        m_fedoraXACMLModule =
                (Authorization) getServer()
                        .getModule("fedora.server.security.Authorization");
        if (m_fedoraXACMLModule == null) {
            throw new ModuleInitializationException("Can't get Authorization module (in default management) from Server.getModule",
                                                    getRole());
        }

        Management m =
                new DefaultManagement(m_fedoraXACMLModule,
                                      m_manager,
                                      m_contentManager,
                                      m_uploadStorageMinutes,
                                      m_lastId,
                                      m_tempDir,
                                      m_uploadStartTime,
                                      m_purgeDelayInMillis);

        mgmt = getProxyChain(m);
    }
    
    @Override
    public void shutdownModule() throws ModuleShutdownException {
        if (invocationHandlers != null) {
            for (AbstractInvocationHandler h : invocationHandlers) {
                h.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String addDatastream(Context context,
                                String pid,
                                String dsID,
                                String[] altIDs,
                                String dsLabel,
                                boolean versionable,
                                String MIMEType,
                                String formatURI,
                                String location,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws ServerException {
        return mgmt.addDatastream(context,
                                  pid,
                                  dsID,
                                  altIDs,
                                  dsLabel,
                                  versionable,
                                  MIMEType,
                                  formatURI,
                                  location,
                                  controlGroup,
                                  dsState,
                                  checksumType,
                                  checksum,
                                  logMessage);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addRelationship(Context context,
                                   String pid,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        return mgmt.addRelationship(context,
                                    pid,
                                    relationship,
                                    object,
                                    isLiteral,
                                    datatype);
    }

    /**
     * {@inheritDoc}
     */
    public String compareDatastreamChecksum(Context context,
                                            String pid,
                                            String dsID,
                                            Date asOfDateTime)
            throws ServerException {
        return mgmt.compareDatastreamChecksum(context, pid, dsID, asOfDateTime);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        return mgmt.export(context, pid, format, exportContext, encoding);
    }

    /**
     * {@inheritDoc}
     */
    public Datastream getDatastream(Context context,
                                    String pid,
                                    String datastreamID,
                                    Date asOfDateTime) throws ServerException {
        return mgmt.getDatastream(context, pid, datastreamID, asOfDateTime);
    }

    /**
     * {@inheritDoc}
     */
    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String datastreamID)
            throws ServerException {
        return mgmt.getDatastreamHistory(context, pid, datastreamID);
    }

    /**
     * {@inheritDoc}
     */
    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String dsState) throws ServerException {
        return mgmt.getDatastreams(context, pid, asOfDateTime, dsState);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        return mgmt.getNextPID(context, numPIDs, namespace);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getObjectXML(Context context, String pid, String encoding)
            throws ServerException {
        return mgmt.getObjectXML(context, pid, encoding);
    }

    /**
     * {@inheritDoc}
     */
    public RelationshipTuple[] getRelationships(Context context,
                                                String pid,
                                                String relationship)
            throws ServerException {
        return mgmt.getRelationships(context, pid, relationship);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getTempStream(String id) throws ServerException {
        return mgmt.getTempStream(id);
    }

    /**
     * {@inheritDoc}
     */
    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         boolean newPid) throws ServerException {
        return mgmt.ingest(context,
                           serialization,
                           logMessage,
                           format,
                           encoding,
                           newPid);
    }

    /**
     * {@inheritDoc}
     */
    public Date modifyDatastreamByReference(Context context,
                                            String pid,
                                            String datastreamID,
                                            String[] altIDs,
                                            String dsLabel,
                                            String mimeType,
                                            String formatURI,
                                            String dsLocation,
                                            String checksumType,
                                            String checksum,
                                            String logMessage,
                                            boolean force)
            throws ServerException {
        return mgmt.modifyDatastreamByReference(context,
                                                pid,
                                                datastreamID,
                                                altIDs,
                                                dsLabel,
                                                mimeType,
                                                formatURI,
                                                dsLocation,
                                                checksumType,
                                                checksum,
                                                logMessage,
                                                force);
    }

    /**
     * {@inheritDoc}
     */
    public Date modifyDatastreamByValue(Context context,
                                        String pid,
                                        String datastreamID,
                                        String[] altIDs,
                                        String dsLabel,
                                        String mimeType,
                                        String formatURI,
                                        InputStream dsContent,
                                        String checksumType,
                                        String checksum,
                                        String logMessage,
                                        boolean force) throws ServerException {
        return mgmt.modifyDatastreamByValue(context,
                                            pid,
                                            datastreamID,
                                            altIDs,
                                            dsLabel,
                                            mimeType,
                                            formatURI,
                                            dsContent,
                                            checksumType,
                                            checksum,
                                            logMessage,
                                            force);
    }

    /**
     * {@inheritDoc}
     */
    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerId,
                             String logMessage) throws ServerException {

        return mgmt.modifyObject(context,
                                 pid,
                                 state,
                                 label,
                                 ownerId,
                                 logMessage);
    }

    /**
     * {@inheritDoc}
     */
    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String datastreamID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage,
                                  boolean force) throws ServerException {
        return mgmt.purgeDatastream(context,
                                    pid,
                                    datastreamID,
                                    startDT,
                                    endDT,
                                    logMessage,
                                    force);
    }

    /**
     * {@inheritDoc}
     */
    public Date purgeObject(Context context,
                            String pid,
                            String logMessage,
                            boolean force) throws ServerException {
        return mgmt.purgeObject(context, pid, logMessage, force);
    }

    /**
     * {@inheritDoc}
     */
    public boolean purgeRelationship(Context context,
                                     String pid,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        return mgmt.purgeRelationship(context,
                                      pid,
                                      relationship,
                                      object,
                                      isLiteral,
                                      datatype);
    }

    /**
     * {@inheritDoc}
     */
    public String putTempStream(Context context, InputStream in)
            throws ServerException {
        return mgmt.putTempStream(context, in);
    }

    /**
     * {@inheritDoc}
     */
    public Date setDatastreamState(Context context,
                                   String pid,
                                   String dsID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        return mgmt.setDatastreamState(context, pid, dsID, dsState, logMessage);
    }

    /**
     * {@inheritDoce}
     */
    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String dsID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        return mgmt.setDatastreamVersionable(context,
                                             pid,
                                             dsID,
                                             versionable,
                                             logMessage);
    }

    /**
     * Build a proxy chain as configured by the module parameters.
     * 
     * @param m
     *        The concrete Management implementation to wrap.
     * @return A proxy chain for Management
     * @throws ModuleInitializationException
     */
    private Management getProxyChain(Management m)
            throws ModuleInitializationException {
        try {
            invocationHandlers = getInvocationHandlers();
            return (Management) ProxyFactory.getProxy(m, invocationHandlers);
        } catch (Exception e) {
            throw new ModuleInitializationException(e.getMessage(), getRole());
        }
    }

    /**
     * Get an <code>Array</code> of <code>AbstractInvocationHandler</code>s. The
     * ordering is ascending alphabetical, determined by the module parameter
     * names that begin with the string "decorator", e.g. "decorator1",
     * "decorator2".
     * 
     * @return An array InvocationHandlers
     */
    private AbstractInvocationHandler[] getInvocationHandlers() throws Exception {
        List<String> pNames = new ArrayList<String>();
        Iterator<String> it = parameterNames();
        String param;
        while (it.hasNext()) {
            param = it.next();
            if (param.startsWith("decorator")) {
                pNames.add(param);
            }
        }
        Collections.sort(pNames);

        AbstractInvocationHandler[] invocationHandlers = new AbstractInvocationHandler[pNames.size()];
        for (int i = 0; i < pNames.size(); i++) {
            invocationHandlers[i] =
                    getInvocationHandler(getParameter(pNames.get(i)));
        }

        return invocationHandlers;
    }

    private static AbstractInvocationHandler getInvocationHandler(String invocationHandler)
            throws Exception {
        if (invocationHandler == null) return null;
        Class<?> c = Class.forName(invocationHandler);
        return (AbstractInvocationHandler)c.newInstance();
    }
}
