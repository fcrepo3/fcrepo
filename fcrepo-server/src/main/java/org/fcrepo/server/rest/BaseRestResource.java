/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import java.io.File;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.FeatureKeys;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.Access;
import org.fcrepo.server.errors.DatastreamLockedException;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.ObjectLockedException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.fcrepo.server.storage.types.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * A barebone RESTFUL resource implementation.
 *
 * @author cuong.tran@yourmediashelf.com
 * @version $Id$
 */
public class BaseRestResource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final String[] EMPTY_STRING_ARRAY = new String[0];
    static final String DEFAULT_ENC = "UTF-8";

    public static final String FORM = "multipart/form-data";
    public static final String HTML = "text/html";
    public static final String XML = "text/xml";
    public static final String ZIP = "application/zip";

    public static final MediaType TEXT_HTML = new MediaType("text", "html");
    public static final MediaType TEXT_XML = new MediaType("text", "xml");

    protected Server fedoraServer;
    protected Management apiMService;
    protected Access apiAService;
    protected String fedoraServerHost;
    protected ObjectMapper mapper;

    protected DatastreamFilenameHelper datastreamFilenameHelper;

    @javax.ws.rs.core.Context
    protected HttpServletRequest servletRequest;

    @javax.ws.rs.core.Context
    protected UriInfo uriInfo;

    @javax.ws.rs.core.Context
    protected HttpHeaders headers;

    public BaseRestResource() {
        try {
            this.fedoraServer = Server.getInstance(new File(Constants.FEDORA_HOME), false);
            this.apiMService = (Management) fedoraServer.getModule("org.fcrepo.server.management.Management");
            this.apiAService = (Access) fedoraServer.getModule("org.fcrepo.server.access.Access");
            this.fedoraServerHost = fedoraServer.getParameter("fedoraServerHost");
            datastreamFilenameHelper = new DatastreamFilenameHelper(fedoraServer, apiMService, apiAService );
            mapper = new ObjectMapper();
        } catch (Exception ex) {
            throw new RestException("Unable to locate Fedora server instance", ex);
        }
    }

    protected Context getContext() {
        return ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                          servletRequest);
    }

    protected DefaultSerializer getSerializer(Context context) {
        return new DefaultSerializer(fedoraServerHost, context);
    }

    protected void transform(String xml, String xslt, Writer out)
    throws TransformerFactoryConfigurationError,
           TransformerConfigurationException,
           TransformerException {
        File xslFile = new File(fedoraServer.getHomeDir(), xslt);
        TransformerFactory factory = TransformerFactory.newInstance();
        if (factory.getClass().getName().equals("net.sf.saxon.TransformerFactoryImpl")) {
            factory.setAttribute(FeatureKeys.VERSION_WARNING, Boolean.FALSE);
        }
        Templates template = factory.newTemplates(new StreamSource(xslFile));
        Transformer transformer = template.newTransformer();
        String appContext = getContext().getEnvironmentValue(Constants.FEDORA_APP_CONTEXT_NAME);
        transformer.setParameter("fedora", appContext);
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(out));
    }

    protected Response buildResponse(MIMETypedStream result) throws Exception {
        if (result.MIMEType.equalsIgnoreCase("application/fedora-redirect")) {
            URI location = URI.create(IOUtils.toString(result.getStream()));
            return Response.temporaryRedirect(location).build();
        } else {
            ResponseBuilder builder = Response.ok();

            if (result.header != null) {
                for (Property header : result.header) {
                    if (header.name != null
                            && !(header.name.equalsIgnoreCase("transfer-encoding"))
                            && !(header.name.equalsIgnoreCase("content-type"))) {
                        builder.header(header.name, header.value);
                    }
                }
            }

            if (!result.MIMEType.equals("")){
                builder.type(result.MIMEType);
            }
            builder.entity(result.getStream());
            return builder.build();
        }
    }

    protected Response handleException(Exception ex) {
        if (ex instanceof ObjectNotInLowlevelStorageException ||
            ex instanceof DatastreamNotFoundException) {
            logger.warn("Resource not found: " + ex.getMessage() + "; unable to fulfill REST API request", ex);
            return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).type("text/plain").build();
        } else if (ex instanceof AuthzException) {
            logger.warn("Authorization failed; unable to fulfill REST API request", ex);
            return Response.status(Status.UNAUTHORIZED).entity(ex.getMessage()).type("text/plain").build();
        } else if (ex instanceof IllegalArgumentException) {
            logger.warn("Bad request; unable to fulfill REST API request", ex);
            return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).type("text/plain").build();
        } else if (ex instanceof ObjectLockedException ||
                   ex instanceof DatastreamLockedException) {
            logger.warn("Lock exception; unable to fulfill REST API request", ex);
            return Response.status(Status.CONFLICT).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } else {
            logger.error("Unexpected error fulfilling REST API request", ex);
            throw new WebApplicationException(ex);
        }
    }
}
