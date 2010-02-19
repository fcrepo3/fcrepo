/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.management;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Accepts and HTTP Multipart POST of a file from an authorized user, and if
 * successful, returns a status of "201 Created" and a text/plain response with
 * a single line containing an opaque identifier that can be used to later
 * submit to the appropriate API-M method.
 * <p>
 * If it fails it will return a non-201 status code with a text/plain
 * explanation. The submitted file must be named "file".
 *
 * @author Chris Wilper
 */
public class UploadServlet
        extends HttpServlet {

    private static final Logger logger =
            LoggerFactory.getLogger(UploadServlet.class);

    private static final long serialVersionUID = 1L;

    /** Instance of Management subsystem (for storing uploaded files). */
    private static Management s_management = null;

    /**
     * The servlet entry point. http://host:port/fedora/management/upload
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Context context =
                ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                           request);
        try {
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload();

            // Parse the request, looking for "file"
            InputStream in = null;
            FileItemIterator iter = upload.getItemIterator(request);
            while (in == null && iter.hasNext()) {
                FileItemStream item = iter.next();
                logger.info("Got next item: isFormField=" + item.isFormField() + " fieldName=" + item.getFieldName());
                if (!item.isFormField() && item.getFieldName().equals("file")) {
                    in = item.openStream();
                }
            }
            if (in == null) {
                sendResponse(HttpServletResponse.SC_BAD_REQUEST,
                             "No data sent.",
                             response);
            } else {
                sendResponse(HttpServletResponse.SC_CREATED,
                             s_management.putTempStream(context, in),
                             response);
            }
        } catch (AuthzException ae) {
            throw RootException.getServletException(ae,
                                                    request,
                                                    "Upload",
                                                    new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
                    .getClass().getName()
                    + ": " + e.getMessage(), response);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        sendResponse(HttpServletResponse.SC_OK,
                     "Client must use HTTP Multipart POST",
                     response);
    }

    public void sendResponse(int status,
                             String message,
                             HttpServletResponse response) {
        try {
            if (status == HttpServletResponse.SC_CREATED) {
                logger.info("Successful upload, id=" + message);
            } else {
                logger.error("Failed upload: " + message);
            }
            response.setStatus(status);
            response.setContentType("text/plain");
            PrintWriter w = response.getWriter();
            w.println(message);
        } catch (Exception e) {
            logger.error("Unable to send response", e);
        }
    }

    /**
     * Initialize servlet. Gets a reference to the fedora Server object.
     *
     * @throws ServletException
     *         If the servet cannot be initialized.
     */
    @Override
    public void init() throws ServletException {
        try {
            Server server =
                    Server.getInstance(new File(Constants.FEDORA_HOME), false);
            s_management = (Management)
                    server.getModule("org.fcrepo.server.management.Management");
            if (s_management == null) {
                throw new ServletException("Unable to get Management module from server.");
            }
        } catch (InitializationException ie) {
            throw new ServletException("Unable to get Fedora Server instance."
                    + ie.getMessage());
        }
    }

}
