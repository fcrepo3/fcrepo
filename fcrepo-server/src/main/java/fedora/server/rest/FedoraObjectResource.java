/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.rest;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;

import java.net.URI;
import java.net.URLEncoder;

import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.Context;
import fedora.server.access.ObjectProfile;
import fedora.server.rest.RestUtil.RequestContent;
import fedora.server.utilities.StreamUtility;

/**
 * Implement /objects/pid/* REST API
 *
 * @author cuong.tran@yourmediashelf.com
 * @version $Id$
 */
@Path("/{pid}")
public class FedoraObjectResource extends BaseRestResource {
    private final String FOXML1_1 = "info:fedora/fedora-system:FOXML-1.1";

    private static Logger LOG =
            Logger.getLogger(FedoraObjectResource.class.getName());

    /**
     * Exports the entire digital object in the specified XML format
     * ("info:fedora/fedora-system:FOXML-1.1" or
     * "info:fedora/fedora-system:METSFedoraExt-1.1"), and encoded appropriately
     * for the specified export context ("public", "migrate", or "archive").
     *
     * GET /objects/{pid}/export ? format context encoding
     */
    @Path("/export")
    @GET
    @Produces(XML)
    public Response getObjectExport(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(FOXML1_1)
            String format,
            @QueryParam(RestParam.EXPORT_CONTEXT)
            String exportContext,
            @QueryParam(RestParam.ENCODING)
            @DefaultValue(DEFAULT_ENC)
            String encoding) {

        try {
            Context context = getContext();
            InputStream is = apiMService.export(context, pid, format, exportContext, encoding);
            return Response.ok(is, TEXT_XML).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Gets a list of timestamps indicating when components changed in an
     * object. This is a set of timestamps indicating when a datastream or
     * disseminator was created or modified in the object. These timestamps can
     * be used to request a timestamped dissemination request to view the object
     * as it appeared at a specific point in time.
     *
     * GET /objects/{pid}/versions ? format
     */
    @Path("/versions")
    @GET
    public Response getObjectHistory(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(HTML)
            String format) {

        try {
            Context context = getContext();
            String[] objectHistory = apiAService.getObjectHistory(context, pid);
            String xml = getSerializer(context).objectHistoryToXml(objectHistory, pid);
            MediaType mime = RestHelper.getContentType(format);

            if (TEXT_HTML.isCompatible(mime)) {
                CharArrayWriter writer = new CharArrayWriter();
                transform(xml, "access/viewObjectHistory.xslt", writer);
                xml = writer.toString();
            }

            return Response.ok(xml, mime).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Gets a profile of the object which includes key metadata fields and URLs
     * for the object Dissemination Index and the object Item Index. Can be
     * thought of as a default view of the object.
     *
     * GET /objects/{pid}/objectXML
     */
    @Path("/objectXML")
    @GET
    @Produces(XML)
    public Response getObjectXML(
            @PathParam(RestParam.PID)
            String pid) {

        try {
            Context context = getContext();
            InputStream is = apiMService.getObjectXML(context, pid, DEFAULT_ENC);

            return Response.ok(is, TEXT_XML).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Gets a profile of the object which includes key metadata fields and URLs
     * for the object Dissemination Index and the object Item Index. Can be
     * thought of as a default view of the object.
     *
     * GET /objects/{pid} ? format asOfDateTime
     */
    @GET
    @Produces( { HTML, XML })
    public Response getObjectProfile(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dateTime,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(HTML)
            String format) {

        try {
            Date asOfDateTime = parseDate(dateTime);
            Context context = getContext();
            ObjectProfile objProfile = apiAService.getObjectProfile(context, pid, asOfDateTime);
            String xml = getSerializer(context).objectProfileToXML(objProfile, asOfDateTime);

            MediaType mime = RestHelper.getContentType(format);

            if (TEXT_HTML.isCompatible(mime)) {
                CharArrayWriter writer = new CharArrayWriter();
                transform(xml, "access/viewObjectProfile.xslt", writer);
                xml = writer.toString();
            }

            return Response.ok(xml, mime).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Permanently removes an object from the repository.
     *
     * DELETE /objects/{pid} ? logMessage force
     */
    @DELETE
    public Response deleteObject(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam("logMessage")
            String logMessage,
            @QueryParam("force")
            @DefaultValue("false")
            boolean force) {
        try {
            Context context = getContext();
            apiMService.purgeObject(context, pid, logMessage, force);
            return Response.noContent().build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Create/Update a new digital object. If no xml given in the body, will
     * create an empty object.
     *
     * POST /objects/{pid} ? label logMessage format encoding namespace ownerId state
     */
    @POST
    @Consumes({ XML, FORM })
    public Response createObject(
            @javax.ws.rs.core.Context
            HttpHeaders headers,
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.LABEL)
            String label,
            @QueryParam(RestParam.LOG_MESSAGE)
            String logMessage,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(FOXML1_1)
            String format,
            @QueryParam(RestParam.ENCODING)
            @DefaultValue(DEFAULT_ENC)
            String encoding,
            @QueryParam(RestParam.NAMESPACE)
            String namespace,
            @QueryParam(RestParam.OWNER_ID)
            String ownerID,
            @QueryParam(RestParam.STATE)
            @DefaultValue("A")
            String state,
            @QueryParam(RestParam.IGNORE_MIME)
            @DefaultValue("false")
            boolean ignoreMime) {
        try {
            Context context = getContext();

            InputStream is = null;
            boolean newPID = false;

            // Determine if content is provided
            RestUtil restUtil = new RestUtil();
            RequestContent content =
                restUtil.getRequestContent(servletRequest, headers);
            if(content != null && content.getContentStream() != null) {
                if(ignoreMime) {
                    is = content.getContentStream();
                } else {
                    // Make sure content is XML
                    String contentMime = content.getMimeType();
                    if(contentMime != null &&
                       TEXT_XML.isCompatible(MediaType.valueOf(contentMime))) {
                        is = content.getContentStream();
                    }
                }
            }

            // If no content is provided, use a FOXML template
            if (is == null) {
                if (pid == null || pid.equals("new")) {
                    pid = apiMService.getNextPID(context, 1, namespace)[0];
                }

                ownerID = context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri);
                is = new ByteArrayInputStream(getFOXMLTemplate(pid, label, ownerID, encoding).getBytes());
            } else {
                // content provided, but no pid
                if (pid == null || pid.equals("new")) {
                    newPID = true;
                }

                if(namespace != null && !namespace.equals("")) {
                    LOG.warn("The namespace parameter is only applicable when object " +
                             "content is not provided, thus the namespace provided '" +
                             namespace + "' has been ignored.");
                }
            }

            pid = apiMService.ingest(context, is, logMessage, format, encoding, newPID);

            URI createdLocation = uriInfo.getRequestUri().resolve(URLEncoder.encode(pid, DEFAULT_ENC));
            return Response.created(createdLocation).entity(pid).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Update digital object
     *
     * PUT /objects/{pid} ? label logMessage ownerId state
     *
     * @see API-M.modifyObject
     */
    @PUT
    public Response updateObject(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.LABEL)
            String label,
            @QueryParam(RestParam.LOG_MESSAGE)
            String logMessage,
            @QueryParam(RestParam.OWNER_ID)
            String ownerID,
            @QueryParam(RestParam.STATE)
            @DefaultValue("A")
            String state) {
        try {
            Context context = getContext();
            apiMService.modifyObject(context, pid, state, label, ownerID, logMessage);
            return Response.ok().build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    private static String getFOXMLTemplate(
            String pid,
            String label,
            String ownerId,
            String encoding) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n");
        xml.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("    xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        xml.append("           xsi:schemaLocation=\"" + Constants.FOXML.uri + " " + Constants.FOXML1_1.xsdLocation + "\"");
        if (pid != null && pid.length() > 0) {
            xml.append("\n           PID=\"" + StreamUtility.enc(pid) + "\">\n");
        } else {
            xml.append(">\n");
        }
        xml.append("  <foxml:objectProperties>\n");
        xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>\n");
        xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""
                + StreamUtility.enc(label) + "\"/>\n");
        xml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\""
                + ownerId + "\"/>\n");
        xml.append("  </foxml:objectProperties>\n");
        xml.append("</foxml:digitalObject>");

        return xml.toString();
    }
}
