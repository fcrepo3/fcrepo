/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import java.io.BufferedInputStream;
import java.io.InputStream;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility class for REST operations.
 *
 * @author Bill Branan
 */
public abstract class RestUtil {

    protected static final Logger logger = LoggerFactory.getLogger(RestUtil.class);

    /**
     * Retrieves the contents of the HTTP Request.
     * @return InputStream from the request
     */
    public static RequestContent getRequestContent(HttpServletRequest request,
                                            HttpHeaders headers)
    throws Exception {
        RequestContent rContent = null;

        // See if the request is a multi-part file upload request
        if(ServletFileUpload.isMultipartContent(request)) {

        	logger.debug("processing multipart content...");
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload();

            // Parse the request, use the first available File item
            FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                if (!item.isFormField()) {
                    rContent = new RequestContent();
                    rContent.contentStream = item.openStream();
                    rContent.mimeType = item.getContentType();

                    FileItemHeaders itemHeaders = item.getHeaders();
                    if(itemHeaders != null) {
                        String contentLength = itemHeaders.getHeader("Content-Length");
                        if(contentLength != null) {
                            rContent.size = Long.parseLong(contentLength);
                        }
                    }

                    break;
                } else {
                	logger.trace("ignoring form field \"{}\" \"{}\"", item.getFieldName(), item.getName());
                }
            }
        } else {
            // If the content stream was not been found as a multipart,
            // try to use the stream from the request directly
            if(rContent == null) {
                String contentLength = request.getHeader("Content-Length");
                long size = 0;
                if(contentLength != null) {
                    size = Long.parseLong(contentLength);
                } else size = request.getContentLength();
                if (size > 0) {
                  rContent = new RequestContent();
                  rContent.contentStream = request.getInputStream();
                  rContent.size = size;
                } else {
                    String transferEncoding =
                            request.getHeader("Transfer-Encoding");
                    if (transferEncoding != null && transferEncoding.contains("chunked")) {
                        BufferedInputStream bis =
                            new BufferedInputStream(request.getInputStream());
                        bis.mark(2);
                        if (bis.read() > 0) {
                            bis.reset();
                            rContent = new RequestContent();
                            rContent.contentStream = bis;
                        }
                    } else {
                    	logger.warn(
                    			"Expected chunked data not found- " +
                    	        "Transfer-Encoding : {}, Content-Length: {}",
                    	        transferEncoding, size);
                    }
                }
            }
        }

        // Attempt to set the mime type and size if not already set
        if(rContent != null) {
            if(rContent.mimeType == null) {
                MediaType mediaType = headers.getMediaType();
                if(mediaType != null) {
                    rContent.mimeType = mediaType.toString();
                }
            }

            if(rContent.size == 0) {
                List<String> lengthHeaders =
                    headers.getRequestHeader("Content-Length");
                if(lengthHeaders != null && lengthHeaders.size() > 0) {
                    rContent.size = Long.parseLong(lengthHeaders.get(0));
                }
            }
        }

        return rContent;
    }

    static class RequestContent {
        private InputStream contentStream = null;
        private String mimeType = null;
        private long size = 0;

        /**
         * @return the contentStream
         */
        public InputStream getContentStream() {
            return contentStream;
        }

        /**
         * @return the mimeType
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * @return the size
         */
        public long getSize() {
            return size;
        }
    }

}
