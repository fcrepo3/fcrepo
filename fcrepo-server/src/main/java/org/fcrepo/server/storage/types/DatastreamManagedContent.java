
/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.validation.ValidationUtility;



/**
 * @author Chris Wilper
 * @version $Id$
 */
public class DatastreamManagedContent
        extends Datastream {

    private static final Logger logger =
        LoggerFactory.getLogger(DatastreamManagedContent.class);


    /**
     * Internal scheme to indicating that a copy should made of the resource.
     */
    public static final String COPY_SCHEME = "copy://";

    public static final String TEMP_SCHEME = "temp://";

    public static final String UPLOADED_SCHEME = "uploaded://";

    private static ILowlevelStorage s_llstore;

    private static Server m_server;

    private static ExternalContentManager s_ecm;

    private static File m_tempUploadDir;

    public int DSMDClass = 0;

    public DatastreamManagedContent() {
    }

    @Override
    public Datastream copy() {
        DatastreamManagedContent ds = new DatastreamManagedContent();
        copy(ds);
        return ds;
    }

    private ILowlevelStorage getLLStore() throws Exception {
        if (s_llstore == null) {
            try {
                s_llstore =
                        (ILowlevelStorage) getServer()
                                .getModule("org.fcrepo.server.storage.lowlevel.ILowlevelStorage");
            } catch (InitializationException ie) {
                throw new Exception("Unable to get LLStore Module: "
                        + ie.getMessage(), ie);
            }
        }
        return s_llstore;
    }
    /**
     * Get the location for storing temporary uploaded files (not for general temporary files)
     * @return the directory
     * @throws Exception
     */
    private File getTempUploadDir() throws Exception {
        if (m_tempUploadDir == null) {
            try {
                m_tempUploadDir = getServer().getUploadDir();
            } catch (InitializationException e) {
                throw new Exception("Unable to get server: " + e.getMessage(), e);
            }
        }
        return m_tempUploadDir;
    }

    private Server getServer() throws Exception {
        if (m_server == null) {
            try {
                m_server = Server.getInstance(new File(Constants.FEDORA_HOME),
                                              false);
            } catch (InitializationException e) {
                throw new Exception("Unable to get Server: " + e.getMessage(), e);
            }
        }
        return m_server;
    }

    private ExternalContentManager getExternalContentManager() throws Exception {
        if (s_ecm == null) {
            try {
                s_ecm = (ExternalContentManager) getServer()
                        .getModule("org.fcrepo.server.storage.ExternalContentManager");
            } catch (InitializationException e) {
                throw new Exception("Unable to get ExternalContentManager Module: "
                                    + e.getMessage(), e);
            }
        }
        return s_ecm;
    }

    @Override
    public InputStream getContentStream() throws StreamIOException {
        try {
            // For new or modified datastreams, the new bytestream hasn't yet been
            // committed. However, we need to access it in order to compute
            // the datastream checksum
            if (DSLocation.startsWith(UPLOADED_SCHEME)) {
                // TODO: refactor to use proper temp file management - FCREPO-718
                // for now, just get the file directly (see also DefaultManagement.getTempStream(...))
                String internalId = DSLocation.substring(UPLOADED_SCHEME.length());
                File uploadedFile = new File(getTempUploadDir(), internalId);
                // check it has not been automatically purged (see DefaultManagement.purgeUploadedFiles())
                if (uploadedFile.exists()) {
                    return new FileInputStream(uploadedFile);
                } else {
                    throw new StreamIOException("Uploaded file " + DSLocation + " no longer exists.");
                }

            } else if (DSLocation.startsWith(TEMP_SCHEME)) {
                // TODO: refactor to use proper temp file management - FCREPO-718
                String fileName = DSLocation.substring(TEMP_SCHEME.length());
                File tempFile = new File(fileName);
                // check it has not been removed elsewhere (should not happen)
                if (tempFile.exists()) {
                    return new FileInputStream(tempFile);
                } else {
                    throw new StreamIOException("Temp file " + DSLocation + " no longer exists.");
                }

            } else {
                try {
                    // validation precludes internal DSLocations, which
                    // have the form pid+dsid+dsvid, e.g. demo:foo+DS1+DS1.0
                    ValidationUtility.validateURL(DSLocation, this.DSControlGrp);
                    // If validation has succeeded, assume an external resource.
                    // Fetch it, store it locally, update DSLocation
                    Context ctx = ReadOnlyContext.EMPTY;
                               ContentManagerParams params = new ContentManagerParams(DSLocation);
                    params.setContext(ctx);
                    MIMETypedStream stream = getExternalContentManager()
                            .getExternalContent(params);

                    // TODO: refactor temp file management - see FCREPO-718; for now create temp file and write to it
                    // note - don't use temp upload directory, use (container's) temp dir (upload dir is for uploads)
                    File tempFile = File.createTempFile("managedcontentupdate", null);
                    OutputStream os = new FileOutputStream(tempFile);
                    StreamUtility.pipeStream(stream.getStream(), os, 32768);
                    DSLocation = TEMP_SCHEME + tempFile.getAbsolutePath();
                    return new FileInputStream(new File(tempFile.getAbsolutePath()));

                } catch(ValidationException e) {
                    // At this point, assume it's an internal id
                    // (e.g. demo:foo+DS1+DS1.0)
                    return getLLStore().retrieveDatastream(DSLocation);
                }
            }
        } catch (Throwable th) {
            throw new StreamIOException("[DatastreamManagedContent] returned "
                    + " the error: \"" + th.getClass().getName()
                    + "\". Reason: " + th.getMessage(), th);
        }
    }

    /**
     * Set the contents of this managed datastream by storing as a temp file.  If the previous content
     * was stored in a temp file, clean up this file.
     * @param stream - the data to store in this datastream
     * @throws StreamIOException
     */
    public void putContentStream(MIMETypedStream stream) throws StreamIOException {

        // TODO: refactor to use proper temp file management - FCREPO-718
        // for now, write to new temp file, clean up existing location
        String oldDSLocation = DSLocation;
        try {
            // note: don't use temp upload dir, use (container's) temp dir (upload dir is for uploads)
            File tempFile = File.createTempFile("managedcontentupdate", null);
            OutputStream os = new FileOutputStream(tempFile);
            StreamUtility.pipeStream(stream.getStream(), os, 32768);
            DSLocation = TEMP_SCHEME + tempFile.getAbsolutePath();
        } catch (Exception e) {
            throw new StreamIOException("Error creating new temp file for updated managed content (existing content is:" + oldDSLocation + ")", e);
        }

        // if old location was a temp location, clean it up
        // (if old location was uploaded, DefaultManagement should do this, but refactor up as part of FCREPO-718)
        if (oldDSLocation != null && oldDSLocation.startsWith(TEMP_SCHEME)) {
            File oldFile;
            try {
                oldFile = new File(oldDSLocation.substring(TEMP_SCHEME.length()));
            } catch (Exception e) {
                throw new StreamIOException("Error removing old temp file while updating managed content (location: " + oldDSLocation + ")", e);
            }
            if (oldFile.exists()) {
                if (!oldFile.delete()) {
                    logger.warn("Failed to delete temp file, marked for deletion when VM closes " + oldFile.getAbsolutePath());
                    oldFile.deleteOnExit();
                }
            } else
                logger.warn("Cannot delete temp file as it no longer exists " + oldFile.getAbsolutePath());
        }
    }

}
