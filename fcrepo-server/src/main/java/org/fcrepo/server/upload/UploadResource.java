/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.upload;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.UploadServlet;
import org.fcrepo.server.rest.BaseRestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Enables the upload of temp files for the REST API. Takes a POST request with
 * a message having media type multipart/form-data and consisting of exactly one
 * entity called "file".
 *
 * @version $Id$
 */
@Path("/")
@Component
public class UploadResource extends BaseRestResource {

    private static final Logger logger = LoggerFactory
            .getLogger(UploadServlet.class);

    public UploadResource(Server server) {
        super(server);
    }

    /**
     * Uploads a file encoded in a multipart/form message.
     *
     * @param multiPart
     *            the multiPart object containing the file to be uploaded
     * @return a URI with the (custom) uploaded:// scheme and a unique
     *         identifier to be used future ingests. Returns 202 if request succeeds
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@Multipart("file") InputStream fileStream){
        if (fileStream == null) {
            return Response.status(400).entity("No file part uploaded").type(MediaType.TEXT_PLAIN).build();
        }
        String uploaded;
        try {
            uploaded = m_management.putTempStream(getContext(), fileStream);
            logger.debug("File uploaded: ", uploaded);
        } catch (ServerException e) {
            logger.error(e.toString());
            return handleException(e, false);
        }
        return Response.status(Response.Status.ACCEPTED).entity(uploaded).type(
                MediaType.TEXT_PLAIN).build();
    }
}
