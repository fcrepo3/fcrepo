/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.io.File;
import java.io.InputStream;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.ReadOnlyContext;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.ValidationException;
import fedora.server.management.Management;
import fedora.server.storage.ContentManagerParams;
import fedora.server.storage.ExternalContentManager;
import fedora.server.storage.lowlevel.ILowlevelStorage;
import fedora.server.validation.ValidationUtility;

/**
 * @author Chris Wilper
 * @version $Id$
 */
public class DatastreamManagedContent
        extends Datastream {
    
    /**
     * Internal scheme to indicating that a copy should made of the resource.
     */
    public static final String COPY_SCHEME = "copy://";
    
    public static final String TEMP_SCHEME = "temp://";
    
    public static final String UPLOADED_SCHEME = "uploaded://";
    
    private static ILowlevelStorage s_llstore;
    
    private static Management s_mgmt;
    
    private static ExternalContentManager s_ecm;

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
                Server server =
                        Server.getInstance(new File(Constants.FEDORA_HOME),
                                           false);
                s_llstore =
                        (ILowlevelStorage) server
                                .getModule("fedora.server.storage.lowlevel.ILowlevelStorage");
            } catch (InitializationException ie) {
                throw new Exception("Unable to get LLStore Module: "
                        + ie.getMessage(), ie);
            }
        }
        return s_llstore;
    }
    
    private Management getManagement() throws Exception {
        if (s_mgmt == null) {
            Server server;
            try {
                server = Server.getInstance(new File(Constants.FEDORA_HOME),
                                   false);
                s_mgmt = (Management) server.getModule("fedora.server.management.Management");
            } catch (InitializationException e) {
                throw new Exception("Unable to get Management Module: "
                                    + e.getMessage(), e);
            }
        }
        return s_mgmt;
    }
    
    private ExternalContentManager getExternalContentManager() throws Exception {
        if (s_ecm == null) {
            Server server;
            try {
                server = Server.getInstance(new File(Constants.FEDORA_HOME),
                                   false);
                s_ecm = (ExternalContentManager) server
                        .getModule("fedora.server.storage.ExternalContentManager");
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
                return getManagement().getTempStream(DSLocation);
            } else {
                try {
                    // validation precludes internal DSLocations, which
                    // have the form pid+dsid+dsvid, e.g. demo:foo+DS1+DS1.0
                    ValidationUtility.validateURL(DSLocation, this.DSControlGrp);
                    // If validation has succeeded, assume an external resource.
                    // Fetch it, store it locally, update DSLocation
                    Context ctx = ReadOnlyContext.EMPTY;
                    MIMETypedStream stream = getExternalContentManager()
                            .getExternalContent(
                                    new ContentManagerParams(DSLocation));
                    DSLocation = getManagement().putTempStream(ctx, stream.getStream());
                    return getManagement().getTempStream(DSLocation);
                } catch(ValidationException e) {
                    // At this point, assume it's an internal id 
                    // (e.g. demo:foo+DS1+DS1.0)
                    return getLLStore().retrieveDatastream(DSLocation);
                }
            }
        } catch (Throwable th) {
            throw new StreamIOException("[DatastreamManagedContent] returned "
                    + " the error: \"" + th.getClass().getName()
                    + "\". Reason: " + th.getMessage());
        }
    }
}
