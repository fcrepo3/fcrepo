/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
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

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.ObjectProfile;
import org.fcrepo.server.rest.RestUtil.RequestContent;
import org.fcrepo.server.rest.param.DateTimeParam;
import org.fcrepo.server.search.Condition;
import org.fcrepo.server.search.FieldSearchQuery;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Implement /objects/pid/* REST API
 *
 * @author cuong.tran@yourmediashelf.com
 * @version $Id$
 */
@Path("/")
@Component
public class FedoraObjectsResource extends BaseRestResource {
    private final String FOXML1_1 = "info:fedora/fedora-system:FOXML-1.1";
    private final String ATOMZIP1_1 = "info:fedora/fedora-system:ATOMZip-1.1";

    static final String[] SEARCHABLE_FIELDS = { "pid", "label", "state", "ownerId",
        "cDate", "mDate", "dcmDate", "title", "creator", "subject", "description",
        "publisher", "contributor", "date", "type", "format", "identifier",
        "source", "language", "relation", "coverage", "rights" };

    private static final Logger logger =
            LoggerFactory.getLogger(FedoraObjectsResource.class);

    public FedoraObjectsResource(Server server) {
        super(server);
    }

    @GET
    @Path("/")
    @Produces( { HTML, XML })
    public Response searchObjects(
            @QueryParam(RestParam.TERMS)
            String terms,
            @QueryParam(RestParam.QUERY)
            String query,
            @QueryParam(RestParam.MAX_RESULTS)
            @DefaultValue("25")
            int maxResults,
            @QueryParam(RestParam.SESSION_TOKEN)
            String sessionToken,
            @QueryParam(RestParam.RESULT_FORMAT)
            @DefaultValue(HTML)
            String format,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {

        try {
            Context context = getContext();
            String[] wantedFields = getWantedFields(m_servletRequest);
            MediaType mime = RestHelper.getContentType(format);

            FieldSearchResult result = null;

            if (wantedFields.length > 0 || sessionToken != null) {
                if (sessionToken != null) {
                    result = m_access.resumeFindObjects(context, sessionToken);
                } else {
                    if ((terms != null) && (terms.length() != 0)) {
                        result = m_access.findObjects(context, wantedFields, maxResults, new FieldSearchQuery(terms));
                    } else {
                        result = m_access.findObjects(context, wantedFields, maxResults, new FieldSearchQuery(Condition.getConditions(query)));
                    }
                }
            }

            String output;
            if (TEXT_HTML.isCompatible(mime)) {
                output = getSerializer(context).searchResultToHtml(query, terms, SEARCHABLE_FIELDS, wantedFields, maxResults, result);
            } else {
                output = getSerializer(context).searchResultToXml(result);

            }

            return Response.ok(output, mime).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    /**
     * Implements the "getNextPID" functionality of the Fedora Management LITE
     * (API-M-LITE) interface using a java servlet front end. The syntax defined
     * by API-M-LITE for getting a list of the next available PIDs has the
     * following binding:
     * <ol>
     * <li>getNextPID URL syntax:
     * protocol://hostname:port/fedora/objects/nextPID[?numPIDs=NUMPIDS&namespace=NAMESPACE&format=html,xml]
     * This syntax requests a list of next available PIDS. The parameter numPIDs
     * determines the number of requested PIDS to generate. If omitted, numPIDs
     * defaults to 1. The namespace parameter determines the namespace to be
     * used in generating the PIDs. If omitted, namespace defaults to the
     * namespace defined in the fedora.fcfg configuration file for the parameter
     * pidNamespace. The xml parameter determines the type of output returned.
     * If the parameter is omitted or has a value of "false", a MIME-typed
     * stream consisting of an html table is returned providing a browser-savvy
     * means of viewing the object profile. If the value specified is "true",
     * then a MIME-typed stream consisting of XML is returned.</li>
     */
    @Path("/nextPID")
    @POST
    public Response getNextPID(
            @QueryParam("numPIDs")
            @DefaultValue("1")
            int numPIDS,
            @QueryParam(RestParam.NAMESPACE)
            String namespace,
            @QueryParam("format")
            @DefaultValue(HTML)
            String format,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) throws Exception {

        try {
            Context context = getContext();
            String[] pidList = m_management.getNextPID(context, numPIDS, namespace);
            MediaType mime = RestHelper.getContentType(format);

            if (pidList.length > 0) {
                String output = getSerializer(context).pidsToXml(pidList);

                if (TEXT_HTML.isCompatible(mime)) {
                    CharArrayWriter writer = new CharArrayWriter();
                    transform(output, "management/getNextPIDInfo.xslt", writer);
                    output = writer.toString();
                }

                return Response.ok(output, mime).build();
            } else {
                return Response.noContent().build();
            }
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    private static String[] getWantedFields(
            HttpServletRequest request) {
        List<String> fields = new ArrayList<String>();

        for (String f : SEARCHABLE_FIELDS) {
            if ("true".equals(request.getParameter(f))) {
                fields.add(f);
            }
        }

        return fields.toArray(new String[fields.size()]);
    }

    @Path(VALID_PID_PART +"/validate")
    @GET
    @Produces({XML})
    public Response doObjectValidation(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dateTime,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        try {
            Context context = getContext();
            Date asOfDateTime = DateUtility.parseDateOrNull(dateTime);
            MediaType mediaType = TEXT_XML;

            Validation validation = m_management.validate(context, pid, asOfDateTime);

            String xml = getSerializer(context).objectValidationToXml(validation);
            return Response.ok(xml, mediaType).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }


    /**
     * Exports the entire digital object in the specified XML format
     * ("info:fedora/fedora-system:FOXML-1.1" or
     * "info:fedora/fedora-system:METSFedoraExt-1.1"), and encoded appropriately
     * for the specified export context ("public", "migrate", or "archive").
     * <p/>
     * GET /objects/{pid}/export ? format context encoding
     */
    @Path(VALID_PID_PART + "/export")
    @GET
    @Produces({XML, ZIP})
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
            String encoding,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {

        try {
            Context context = getContext();
            InputStream is = m_management.export(context, pid, format, exportContext, encoding);
            MediaType mediaType = TEXT_XML;
            if (format.equals(ATOMZIP1_1)) {
                mediaType = MediaType.valueOf(ZIP);
            }
            return Response.ok(is, mediaType).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    /**
     * Gets a list of timestamps indicating when components changed in an
     * object. This is a set of timestamps indicating when a datastream or
     * disseminator was created or modified in the object. These timestamps can
     * be used to request a timestamped dissemination request to view the object
     * as it appeared at a specific point in time.
     * <p/>
     * GET /objects/{pid}/versions ? format
     */
    @Path(VALID_PID_PART + "/versions")
    @GET
    public Response getObjectHistory(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(HTML)
            String format,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {

        try {
            Context context = getContext();
            String[] objectHistory = m_access.getObjectHistory(context, pid);
            String xml = getSerializer(context).objectHistoryToXml(objectHistory, pid);
            MediaType mime = RestHelper.getContentType(format);

            if (TEXT_HTML.isCompatible(mime)) {
                CharArrayWriter writer = new CharArrayWriter();
                transform(xml, "access/viewObjectHistory.xslt", writer);
                xml = writer.toString();
            }

            return Response.ok(xml, mime).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    /**
     * Gets a profile of the object which includes key metadata fields and URLs
     * for the object Dissemination Index and the object Item Index. Can be
     * thought of as a default view of the object.
     * <p/>
     * GET /objects/{pid}/objectXML
     */
    @Path(VALID_PID_PART + "/objectXML")
    @GET
    @Produces(XML)
    public Response getObjectXML(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {

        try {
            Context context = getContext();
            InputStream is = m_management.getObjectXML(context, pid, DEFAULT_ENC);

            return Response.ok(is, TEXT_XML).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    /**
     * Gets a profile of the object which includes key metadata fields and URLs
     * for the object Dissemination Index and the object Item Index. Can be
     * thought of as a default view of the object.
     * <p/>
     * GET /objects/{pid} ? format asOfDateTime
     */
    @GET
    @Produces({HTML, XML})
    @Path(VALID_PID_PART)
    public Response getObjectProfile(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dateTime,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(HTML)
            String format,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {

        try {
            Date asOfDateTime = DateUtility.parseDateOrNull(dateTime);
            Context context = getContext();
            ObjectProfile objProfile = m_access.getObjectProfile(context, pid, asOfDateTime);
            String xml = getSerializer(context).objectProfileToXML(objProfile, asOfDateTime);

            MediaType mime = RestHelper.getContentType(format);

            if (TEXT_HTML.isCompatible(mime)) {
                CharArrayWriter writer = new CharArrayWriter();
                transform(xml, "access/viewObjectProfile.xslt", writer);
                xml = writer.toString();
            }

            return Response.ok(xml, mime).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    /**
     * Permanently removes an object from the repository.
     * <p/>
     * DELETE /objects/{pid} ? logMessage
     */
    @DELETE
    @Path(VALID_PID_PART)
    public Response deleteObject(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam("logMessage")
            String logMessage,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        try {
            Context context = getContext();
            Date d = m_management.purgeObject(context, pid, logMessage);
            return Response.ok(DateUtility.convertDateToXSDString(d), MediaType.TEXT_PLAIN_TYPE).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    @POST
    @Path("/new")
    @Consumes({XML, FORM})
    public Response newObject(
                              @javax.ws.rs.core.Context
                              HttpHeaders headers,
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
                              boolean ignoreMime,
                              @QueryParam(RestParam.FLASH)
                              @DefaultValue("false")
                              boolean flash) {
        return createObject(headers, "new", label, logMessage, format, encoding, namespace, ownerID, state, ignoreMime, flash);
    }
    /**
     * Create/Update a new digital object. If no xml given in the body, will
     * create an empty object.
     * <p/>
     * POST /objects/{pid} ? label logMessage format encoding namespace ownerId state
     */
    @POST
    @Path(VALID_PID_PART)
    @Consumes({XML, FORM, ZIP})
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
            boolean ignoreMime,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        try {
            Context context = getContext();

            InputStream is = null;

            // Determine if content is provided
            RequestContent content =
                    RestUtil.getRequestContent(m_servletRequest, headers);
            if (content != null && content.getContentStream() != null) {
                if (ignoreMime) {
                    is = content.getContentStream();
                } else {
                    // Make sure content is XML or ZIP
                    String contentMime = content.getMimeType();
                    if (contentMime != null) {
                        MediaType t = MediaType.valueOf(contentMime);
                        if (TEXT_XML.isCompatible(t) || APP_ZIP.isCompatible(t)) {
                            is = content.getContentStream();
                        }
                    }
                }
            }

            // If no content is provided, use a FOXML template
            if (is == null) {
                if (pid == null || pid.equals("new")) {
                    pid = m_management.getNextPID(context, 1, namespace)[0];
                }

                if (ownerID == null || "".equals(ownerID.trim())) {
                    ownerID = context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri);
                }
                is = new ByteArrayInputStream(getFOXMLTemplate(pid, label, ownerID, encoding).getBytes());
            } else {

                if (namespace != null && !namespace.equals("")) {
                    logger.warn("The namespace parameter is only applicable whene object " +
                                "content is not provided, thus the namespace provided '" +
                                namespace + "' has been ignored.");
                }
            }

            pid = m_management.ingest(context, is, logMessage, format, encoding, pid);

            URI createdLocation = m_uriInfo.getRequestUri().resolve(URLEncoder.encode(pid, DEFAULT_ENC));
            return Response.created(createdLocation).entity(pid).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
        }
    }

    /**
     * Update (modify) digital object.
     * <p>PUT /objects/{pid} ? label logMessage ownerId state lastModifiedDate</p>
     *
     * @param pid              the persistent identifier
     * @param label
     * @param logMessage
     * @param ownerID
     * @param state
     * @param lastModifiedDate Optional XSD dateTime to guard against concurrent
     *                         modification. If provided (i.e. not null), the request will fail with an
     *                         HTTP 409 Conflict if lastModifiedDate is earlier than the object's
     *                         lastModifiedDate.
     * @return The timestamp for this modification (as an XSD dateTime string)
     * @see org.fcrepo.server.management.Management#modifyObject(org.fcrepo.server.Context, String, String, String, String, String)
     */
    @PUT
    @Path(VALID_PID_PART)
    @Produces(MediaType.TEXT_PLAIN)
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
            String state,
            @QueryParam(RestParam.LAST_MODIFIED_DATE)
            DateTimeParam lastModifiedDate,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        try {
            Context context = getContext();
            Date requestModDate = null;
            if (lastModifiedDate != null) {
                requestModDate = lastModifiedDate.getValue();
            }
            Date lastModDate =
                    m_management.modifyObject(context, pid, state, label, ownerID, logMessage, requestModDate);
            return Response.ok().entity(DateUtility.convertDateToXSDString(lastModDate)).build();
        } catch (Exception ex) {
            return handleException(ex, flash);
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
        xml.append(
                "           xsi:schemaLocation=\"" + Constants.FOXML.uri + " " + Constants.FOXML1_1.xsdLocation + "\"");
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
