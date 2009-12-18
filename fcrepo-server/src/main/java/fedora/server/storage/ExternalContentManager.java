/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import fedora.server.errors.ServerException;
import fedora.server.storage.types.MIMETypedStream;

/**
 * Interface that provides a mechanism for retrieving external content via HTTP.
 * 
 * @author Ross Wayland
 * @version $Id$
 */
public interface ExternalContentManager {

    
    /**
     * Reads the contents of the specified URL and returns the
     * result as a MIMETypedStream. Used as a wrapper with a default MIME type 
     * of "text/plain"
     * 
     * @param url
     *        The URL of the external content.
     * @param context
     *           The context map.       
     * @return A MIME-typed stream.
     * @throws ServerException
     *         If the URL connection could not be established.
     */
    public MIMETypedStream getExternalContent(ContentManagerParams params)
            throws ServerException;
    
}
