/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.io.File;
import java.io.InputStream;



import fedora.common.Constants;
import fedora.server.Server;
import fedora.server.errors.InitializationException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.ContentManagerParams;
import fedora.server.storage.ExternalContentManager;

/**
 * Referenced Content.
 * 
 * @author Chris Wilper
 * @version $Id$
 */
public class DatastreamReferencedContent
        extends Datastream {

    private static ExternalContentManager s_ecm;


    public DatastreamReferencedContent() {
    }

    @Override
    public Datastream copy() {
        DatastreamReferencedContent ds = new DatastreamReferencedContent();
        copy(ds);
        return ds;
    }

    /**
     * Gets the external content manager which is used for the retrieval of
     * content.
     * 
     * @return an instance of <code>ExternalContentManager</code> 
     * @throws Exception is thrown in case the server is not able to find the module.
     */
    private ExternalContentManager getExternalContentManager()
            throws Exception {
        if (s_ecm == null) {
            Server server;
            try {
                server = Server.getInstance(new File(Constants.FEDORA_HOME),
                        false);
                s_ecm = (ExternalContentManager) server
                        .getModule("fedora.server.storage.ExternalContentManager");
            } catch (InitializationException e) {
                throw new Exception(
                        "Unable to get ExternalContentManager Module: "
                                + e.getMessage(), e);
            }
        }
        return s_ecm;
    }

    /**
     * Gets an InputStream to the content of this externally-referenced
     * datastream.
     * 
     * <p>The DSLocation of this datastream must be non-null before invoking 
     * this method.
     * 
     * <p>If successful, the DSMIME type is automatically set based on the web
     * server's response header. If the web server doesn't send a valid
     * Content-type: header, as a last resort, the content-type is guessed by
     * using a map of common extensions to mime-types.
     * 
     * <p>If the content-length header is present in the response, DSSize will 
     * be set accordingly.
     *
     * @see fedora.server.storage.types.Datastream#getContentStream()
     */
    @Override
    public InputStream getContentStream() throws StreamIOException {
        try {
            MIMETypedStream stream = getExternalContentManager()
                    .getExternalContent(new ContentManagerParams(DSLocation));
            DSSize = getContentLength(stream);
            return stream.getStream();
        } catch (Exception ex) {
            throw new StreamIOException("Error getting content stream", ex);
        }
    }
    
    /**
     * Returns the length of the content of this stream.
     * @param stream the MIMETypedStream 
     * @return length the length of the content
     */
    public long getContentLength(MIMETypedStream stream) {
        long length = 0;
        if (stream.header != null) {
            for (int i = 0; i < stream.header.length; i++) {
                if (stream.header[i].name != null
                        && !stream.header[i].name.equalsIgnoreCase("")
                        && stream.header[i].name.equalsIgnoreCase("content-length")) {
                    length = Long.parseLong(stream.header[i].value);
                    break;
                }
            }
        }
        return length;
    }    
}
