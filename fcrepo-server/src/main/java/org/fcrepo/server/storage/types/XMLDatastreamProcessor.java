package org.fcrepo.server.storage.types;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.errors.StreamIOException;

/**
 * Wrapper class for a datastream that has XML metadata.
 *
 * Allows managed content and inline xml datastreams to be treated generically
 * as datastreams having XML metadata content, eg for DC, RELS-EXT and RELS-INT which
 * may be stored either inline or as managed content.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */

public class XMLDatastreamProcessor {

    private static final Logger logger =
        LoggerFactory.getLogger(XMLDatastreamProcessor.class);


    protected Datastream m_ds; // the wrapped datastream

    private enum DS_TYPE {
        INLINE_XML,
        MANAGED
    }

    // when creating new datastreams via this class, these determine the
    // type (M or X) of datastream to create.  See fedora.fcfg for config
    private static String DC_DEFAULT_CONTROLGROUP;
    private static String RELS_DEFAULT_CONTROLGROUP;

    private static boolean initialized = false;

    protected DS_TYPE m_dsType;

    @SuppressWarnings("unused")
    private XMLDatastreamProcessor() {}

    /**
     * Construct a new XML datastream processor and the associated datastream
     * using the datastream ID to determine the type (M or X) of datastream to
     * construct (see fedora.fcfg for configuring the default types)
     *
     * @param dsId
     */
    public XMLDatastreamProcessor(String dsId) {
        init();

        String controlGroup = null;
        if (dsId.equals("DC")) {
            controlGroup = DC_DEFAULT_CONTROLGROUP;
        } else if (dsId.equals("RELS-EXT") || dsId.equals("RELS-INT")) {
            controlGroup = RELS_DEFAULT_CONTROLGROUP;
        } else {
            // coding error if trying to handle other types of datastream
            throw new RuntimeException("XML Datastream Processor only handles DC and RELS datastreams.  Datastream ID supplied was " + dsId);
        }

        if (controlGroup.equals("X")) {
            m_ds = new DatastreamXMLMetadata();
            m_dsType = DS_TYPE.INLINE_XML;
        } else if (controlGroup.equals("M")) {
            m_ds = new DatastreamManagedContent();
            m_dsType = DS_TYPE.MANAGED;
        }
        m_ds.DSControlGrp = controlGroup;
        m_ds.DatastreamID = dsId;
    }

    /**
     * Construct an XML Datastream Processor by wrapping an existing datastream
     *
     * @param ds - datastream to wrap
     */
    public XMLDatastreamProcessor(Datastream ds) {

        if (ds instanceof DatastreamXMLMetadata) {
            m_ds = ds;
            m_dsType = DS_TYPE.INLINE_XML;
        } else if (ds instanceof DatastreamManagedContent) {
            m_ds = ds;
            m_dsType = DS_TYPE.MANAGED;
        } else {
            // unhandled type is a coding error
            throw new RuntimeException("XML datastreams must be of type Managed or Inline,"
                    + " but type was " + ds.getClass().getName());
        }
    }

    private static void init() {
        if (!initialized) {
            Server server;
            // get default types of datastream (M or X) to be used for reserved datastreams
            try {
                server = Server.getInstance(new File(Constants.FEDORA_HOME),
                                            false);
                Module module = server.getModule("org.fcrepo.server.storage.DOManager");
                DC_DEFAULT_CONTROLGROUP = module.getParameter("defaultDCControlGroup");
                RELS_DEFAULT_CONTROLGROUP = module.getParameter("defaultRELSControlGroup");
            } catch (ServerInitializationException e) {
                logger.error("Unable to get server", e);
            } catch (ModuleInitializationException e) {
                logger.error("Unable to get DOManager module", e);
            }
            if (DC_DEFAULT_CONTROLGROUP == null) {
                logger.error("Unable to determine default controlgroup for DC datastreams, using X");
                DC_DEFAULT_CONTROLGROUP = "X";
            }
            if (RELS_DEFAULT_CONTROLGROUP == null) {
                logger.error("Unable to determine default controlgroup for RELS datastreams, using X");
                RELS_DEFAULT_CONTROLGROUP = "X";
            }
            initialized = true;
        }
    }

    /**
     * Return a new XML Datastream processor wrapping a new datastream.  The type (M or X) of the
     * new datastream will be the same as the existing one.  Use to generate new versions of existing
     * datastreams and wrap them in an XML datastream processor.
     *
     * @throws ServerInitializationException
     * @throws ModuleInitializationException
     */
    public XMLDatastreamProcessor newVersion() throws ServerInitializationException, ModuleInitializationException {

        // create new datastream (version) based on existing datastream control group
        Datastream ds;
        if (m_dsType == DS_TYPE.INLINE_XML) {
            ds = new DatastreamXMLMetadata();
            ds.DSControlGrp = "X";
        } else if (m_dsType == DS_TYPE.MANAGED) {
            ds = new DatastreamManagedContent();
            ds.DSControlGrp = "M";
        } else {
            // unhandled type is a coding error
            throw new RuntimeException("XML datastreams must be of type Managed or Inline");
        }
        return new XMLDatastreamProcessor(ds);

    }


    /**
     * Get the XML content of the datastream wrapped by this class
     * @return
     */
    public byte [] getXMLContent() {
        return getXMLContent(null);
    }
    public byte[] getXMLContent(Context ctx) {
        // could use getContentStream generically instead?
        if (m_dsType == DS_TYPE.INLINE_XML)
            return ((DatastreamXMLMetadata)m_ds).xmlContent;
        else if (m_dsType == DS_TYPE.MANAGED)
            try {
                if (ctx == null) {
                    return IOUtils.toByteArray(m_ds.getContentStream());
                } else {
                    return IOUtils.toByteArray(m_ds.getContentStream(ctx));
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to read managed stream contents", e);
            } catch (StreamIOException e) {
                throw new RuntimeException("Unable to read managed stream contents", e);
            }
            else
                throw new RuntimeException("XML datastreams must be of type Managed or Inline");

    }

    /**
     * Get the datastream wrapped by this class
     * @return
     */
    public Datastream getDatastream() {
        return m_ds;
    }

    /**
     * Update the XML content of the datastream wrapped by this class
     * @param xmlContent
     */
    public void setXMLContent(byte[] xmlContent) {
        if (m_dsType == DS_TYPE.INLINE_XML) {
            ((DatastreamXMLMetadata)m_ds).xmlContent = xmlContent;
        } else if (m_dsType == DS_TYPE.MANAGED) {
            ByteArrayInputStream bais = new ByteArrayInputStream(xmlContent);
            MIMETypedStream s = new MIMETypedStream("text/xml", bais, null,xmlContent.length);
            try {
                ((DatastreamManagedContent)m_ds).putContentStream(s);
            } catch (StreamIOException e) {
                throw new RuntimeException("Unable to update managed datastream contents", e);
            }
        } else
            // coding error if trying to use other datastream type
            throw new RuntimeException("XML datastreams must be of type Managed or Inline");
    }

    /**
     * Set the DSMDClass of the datastream wrapped by this class
     * @param DSMDClass
     */
    public void setDSMDClass(int DSMDClass) {
        if (m_dsType == DS_TYPE.INLINE_XML)
            ((DatastreamXMLMetadata)m_ds).DSMDClass = DSMDClass;
        else if (m_dsType == DS_TYPE.MANAGED)
            ((DatastreamManagedContent)m_ds).DSMDClass = DSMDClass;
        else
            // coding error if trying to use other datastream type
            throw new RuntimeException("XML datastreams must be of type Managed or Inline");

    }
    /**
     * Get the DSMDClass of the datastream wrapped by this class
     * @return
     */
    public int getDSMDClass() {
        if (m_dsType == DS_TYPE.INLINE_XML)
            return ((DatastreamXMLMetadata)m_ds).DSMDClass;
        else if (m_dsType == DS_TYPE.MANAGED)
            return ((DatastreamManagedContent)m_ds).DSMDClass;
        else
            // coding error if trying to use other datastream type
            throw new RuntimeException("XML datastreams must be of type Managed or Inline");
    }


}
