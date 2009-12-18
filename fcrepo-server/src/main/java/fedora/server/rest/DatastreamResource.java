/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.rest;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;

import java.net.URLEncoder;

import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import fedora.common.http.WebClient;

import fedora.server.Context;
import fedora.server.rest.RestUtil.RequestContent;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamDef;
import fedora.server.storage.types.MIMETypedStream;
import fedora.server.utilities.DateUtility;

/**
 * A rest controller to handle CRUD operations for the Fedora datasream API
 * (API-M) Request syntax:
 *
 * GET,PUT,POST,DELETE
 * prototol://hostname:port/fedora/objects/PID/datastreams/DSID/versions ? [dateTime][parmArray]
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required path name for the Fedora access service.</li>
 * <li>objects - required path name for the Fedora service.</li>
 * <li>PID - required persistent idenitifer of the digital object.</li>
 * <li>DSID - required datastream identifier for the datastream.</li>
 * <li>dateTime - optional dateTime value indicating dissemination of a version
 * of the digital object at the specified point in time.
 * <li>parmArray - optional array of method parameters consisting of name/value
 * pairs in the form parm1=value1&parm2=value2...</li>
 *
 * @author cuong.tran@yourmediashelf.com
 * @version $Id$
 */
@Path("/{pid}/datastreams")
public class DatastreamResource extends BaseRestResource {

    /**
     * Inquires upon all object Datastreams to obtain datastreams contained by a
     * digital object. This returns a set of datastream locations that represent
     * all possible datastreams available in the object.
     *
     * GET /objects/{pid}/datastreams ? asOfDateTime format
     *
     * @param pid
     * @param dateTime
     * @param format
     */
    @GET
    public Response listDatastreams(
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
            MediaType mime = RestHelper.getContentType(format);
            DatastreamDef[] dsDefs = apiAService.listDatastreams(context, pid, asOfDateTime);

            String output = getSerializer(context).dataStreamsToXML(pid, asOfDateTime, dsDefs);

            if (TEXT_HTML.isCompatible(mime)) {
                CharArrayWriter writer = new CharArrayWriter();
                transform(output, "access/listDatastreams.xslt", writer);
                output = writer.toString();
            }

            return Response.ok(output, mime).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Invoke API-M.getDatastream(context, pid, dsID, asOfDateTime)
     *
     * GET /objects/{pid}/datastreams/{dsID} ? asOfDateTime & validateChecksum=true|false
     */
    @Path("/{dsID}")
    @GET
    public Response getDatastreamProfile(
                     @PathParam(RestParam.PID)
                     String pid,
                     @PathParam(RestParam.DSID)
                     String dsID,
                     @QueryParam(RestParam.AS_OF_DATE_TIME)
                     String dateTime,
                     @QueryParam(RestParam.FORMAT)
                     @DefaultValue(HTML)
                     String format,
                     @QueryParam(RestParam.VALIDATE_CHECKSUM)
                     @DefaultValue("false")
                     boolean validateChecksum) {
        try {
            Date asOfDateTime = parseDate(dateTime);
            Context context = getContext();
            Datastream dsProfile = apiMService.getDatastream(context, pid, dsID, asOfDateTime);

            if(dsProfile == null) {
                return Response.status(Status.NOT_FOUND).type("text/plain").entity(
                  "No datastream could be found. Either there is no datastream for " +
                  "the digital object \""+pid+"\" with datastream ID of \""+dsID+
                  "\"  OR  there are no datastreams that match the specified " +
                  "date/time value of \""+dateTime+"\".").build();
            }

            String xml = getSerializer(context).
                datastreamProfileToXML(pid, dsID, dsProfile, asOfDateTime, validateChecksum);

            MediaType mime = RestHelper.getContentType(format);

            if (TEXT_HTML.isCompatible(mime)) {
                CharArrayWriter writer = new CharArrayWriter();
                transform(xml, "management/viewDatastreamProfile.xslt", writer);
                xml = writer.toString();
            }

            return Response.ok(xml, mime).build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Invoke API-A.getDatastreamDissemination(context, pid, dsID, asOfDateTime)
     *
     * GET /objects/{pid}/datastreams/{dsID}/content ? asOfDateTime
     */
    @Path("/{dsID}/content")
    @GET
    public Response getDatastream(
            @PathParam(RestParam.PID)
            String pid,
            @PathParam(RestParam.DSID)
            String dsID,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dateTime,
            @QueryParam(RestParam.DOWNLOAD)
            String download) {


        Context context = getContext();
        try {
            MIMETypedStream stream = apiAService.getDatastreamDissemination(
                                                                        context,
                                                                        pid,
                                                                        dsID,
                                                                        parseDate(dateTime));
            if (datastreamFilenameHelper != null) {
                datastreamFilenameHelper.addContentDispositionHeader(context, pid, dsID, download, parseDate(dateTime), stream);

            }

            return buildResponse(stream);
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Invoke API-M.purgeDatastream
     *
     * DELETE /objects/{pid}/datastreams/{dsID} ? startDT endDT logMessage force
     */
    @Path("/{dsID}")
    @DELETE
    public Response deleteDatastream(
            @PathParam(RestParam.PID)
            String pid,
            @PathParam(RestParam.DSID)
            String dsID,
            @QueryParam(RestParam.START_DT)
            String startDT,
            @QueryParam(RestParam.END_DT)
            String endDT,
            @QueryParam(RestParam.LOG_MESSAGE)
            String logMessage,
            @QueryParam(RestParam.FORCE)
            @DefaultValue("false")
            boolean force) {

        try {
            Context context = getContext();
            Date startDate = DateUtility.convertStringToDate(startDT);
            Date endDate = DateUtility.convertStringToDate(endDT);
            apiMService.purgeDatastream(context, pid, dsID, startDate,
                                        endDate, logMessage, force);
            return Response.noContent().build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Modify an existing datastream.
     *
     * PUT /objects/{pid}/datastreams/{dsID} ? dsLocation altIDs dsLabel versionable
     *                                         dsState formatURI checksumType checksum
     *                                         logMessage force
     *
     * Successful Response:
     *   Status: 200 OK
     *   Content-Type: text/xml
     *   Body: XML datastream profile
     */
    @Path("/{dsID}")
    @PUT
    public Response modifyDatastream(
            @PathParam(RestParam.PID)
            String pid,
            @PathParam(RestParam.DSID)
            String dsID,
            @QueryParam(RestParam.DS_LOCATION)
            String dsLocation,
            @QueryParam(RestParam.ALT_IDS)
            List<String> altIDs,
            @QueryParam(RestParam.DS_LABEL)
            String dsLabel,
            @QueryParam(RestParam.VERSIONABLE)
            @DefaultValue("true")
            boolean versionable,
            @QueryParam(RestParam.DS_STATE)
            String dsState,
            @QueryParam(RestParam.FORMAT_URI)
            String formatURI,
            @QueryParam(RestParam.CHECKSUM_TYPE)
            String checksumType,
            @QueryParam(RestParam.CHECKSUM)
            String checksum,
            @QueryParam(RestParam.MIME_TYPE)
            String mimeType,
            @QueryParam(RestParam.LOG_MESSAGE)
            String logMessage,
            @QueryParam(RestParam.FORCE)
            @DefaultValue("false")
            boolean force,
            @QueryParam(RestParam.IGNORE_CONTENT)
            @DefaultValue("false")
            boolean ignoreContent) {
        return addOrUpdateDatastream(false, pid, dsID, headers.getMediaType(), mimeType,
                                     null, dsLocation, altIDs, dsLabel, versionable,
                                     dsState, formatURI, checksumType, checksum,
                                     logMessage, force, ignoreContent);
    }

    /**
     * Add or modify a datastream.
     *
     * POST /objects/{pid}/datastreams/{dsID} ? controlGroup dsLocation altIDs dsLabel
     *                                          versionable dsState formatURI
     *                                          checksumType checksum logMessage
     *
     * Successful Response:
     *   Status: 201 Created
     *   Location: Datastream profile URL
     *   Content-Type: text/xml
     *   Body: XML datastream profile
     */
    @Path("/{dsID}")
    @POST
    public Response addDatastream(
            @PathParam(RestParam.PID)
            String pid,
            @PathParam(RestParam.DSID)
            String dsID,
            @QueryParam(RestParam.CONTROL_GROUP)
            @DefaultValue("X")
            String controlGroup,
            @QueryParam(RestParam.DS_LOCATION)
            String dsLocation,
            @QueryParam(RestParam.ALT_IDS)
            List<String> altIDs,
            @QueryParam(RestParam.DS_LABEL)
            String dsLabel,
            @QueryParam(RestParam.VERSIONABLE)
            @DefaultValue("true")
            boolean versionable,
            @QueryParam(RestParam.DS_STATE)
            @DefaultValue("A")
            String dsState,
            @QueryParam(RestParam.FORMAT_URI)
            String formatURI,
            @QueryParam(RestParam.CHECKSUM_TYPE)
            String checksumType,
            @QueryParam(RestParam.CHECKSUM)
            String checksum,
            @QueryParam(RestParam.MIME_TYPE)
            String mimeType,
            @QueryParam(RestParam.LOG_MESSAGE)
            String logMessage) {
        return addOrUpdateDatastream(true, pid, dsID, headers.getMediaType(), mimeType,
                                     controlGroup, dsLocation, altIDs, dsLabel,
                                     versionable, dsState, formatURI, checksumType,
                                     checksum, logMessage, false, false);
    }

    protected Response addOrUpdateDatastream(
            boolean posted,
            String pid,
            String dsID,
            MediaType mediaType,
            String mimeType,
            String controlGroup,
            String dsLocation,
            List<String> altIDList,
            String dsLabel,
            boolean versionable,
            String dsState,
            String formatURI,
            String checksumType,
            String checksum,
            String logMessage,
            boolean force,
            boolean ignoreContent) {

        try {
            String[] altIDs = {};

            if (altIDList != null && altIDList.size() > 0) {
                altIDs = altIDList.toArray(new String[altIDList.size()]);
            }

            Context context = getContext();

            Datastream existingDS = apiMService.getDatastream(context, pid, dsID, null);

            // If a datastream is set to Deleted state, it must be set to
            // another state before any other changes can be made
            if(existingDS != null && existingDS.DSState.equals("D") && dsState != null) {
                if(dsState.equals("A") || dsState.equals("I")) {
                    apiMService.setDatastreamState(context, pid, dsID,
                                                   dsState, logMessage);
                    existingDS.DSState = dsState;
                }
            }

            InputStream is = null;

            // Determine if datastream content is included in the request
            if(!ignoreContent) {
                RestUtil restUtil = new RestUtil();
                RequestContent content =
                    restUtil.getRequestContent(servletRequest, headers);

                if(content != null && content.getContentStream() != null) {
                    is = content.getContentStream();
                    // Give preference to the passed in mimeType
                    if(mimeType == null && content.getMimeType() != null) {
                        mimeType = content.getMimeType();
                    }
                }
            }

            // Make sure that there is a mime type value
            if(mimeType == null && mediaType != null) {
                mimeType = mediaType.toString();
            } else if(mimeType == null && mediaType == null) {
                mimeType = existingDS.DSMIME;
            }

            // set default control group based on mimeType
            if (dsLocation == null &&
                TEXT_XML.isCompatible(MediaType.valueOf(mimeType)) &&
                controlGroup == null) {
                controlGroup = "X";
            }

            if (existingDS == null) {
                if (posted) {
                    if ((dsLocation == null || dsLocation.equals(""))
                            && ("X".equals(controlGroup) || "M".equals(controlGroup))) {
                        dsLocation = apiMService.putTempStream(context, is);
                    }
                    dsID = apiMService.addDatastream(context, pid, dsID, altIDs, dsLabel,
                                                     versionable, mimeType, formatURI,
                                                     dsLocation, controlGroup, dsState,
                                                     checksumType, checksum, logMessage);
                } else {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                if ("X".equals(existingDS.DSControlGrp)) {
                    // Inline XML can only be modified by value. If there is no stream,
                    // but there is a dsLocation attempt to retrieve the content.
                    if(is == null && dsLocation != null && !dsLocation.equals("")) {
                        try {
                            WebClient webClient = new WebClient();
                            is = webClient.get(dsLocation, true);
                        } catch(IOException ioe) {
                            throw new Exception("Could not retrive content from " +
                                                dsLocation + " due to error: " +
                                                ioe.getMessage());
                        }
                    }
                    apiMService.modifyDatastreamByValue(context, pid, dsID, altIDs,
                                                        dsLabel, mimeType, formatURI,
                                                        is, checksumType, checksum,
                                                        logMessage, force);
                } else {
                    // Managed content can only be modified by reference.
                    // If there is no dsLocation, but there is a content stream,
                    // store the stream in a temporary location.
                    if (dsLocation == null && ("M".equals(existingDS.DSControlGrp))) {
                        if(is != null) {
                            dsLocation = apiMService.putTempStream(context, is);
                        } else {
                            dsLocation = null;
                        }
                    }

                    apiMService.modifyDatastreamByReference(context, pid, dsID, altIDs,
                                                            dsLabel, mimeType, formatURI,
                                                            dsLocation, checksumType,
                                                            checksum, logMessage, force);
                }

                if(dsState != null) {
                    if(dsState.equals("A") ||
                       dsState.equals("D") ||
                       dsState.equals("I")) {
                        if(!dsState.equals(existingDS.DSState)) {
                            apiMService.setDatastreamState(context, pid, dsID,
                                                           dsState, logMessage);
                        }
                    }
                }

                if(versionable != existingDS.DSVersionable) {
                    apiMService.setDatastreamVersionable(context, pid, dsID,
                                                         versionable, logMessage);
                }
            }

            ResponseBuilder builder;
            if (posted) {
                builder = Response.created(uriInfo.getRequestUri().resolve(
                        URLEncoder.encode(dsID, DEFAULT_ENC)));
            } else { // put
                builder = Response.ok();
            }
            builder.header("Content-Type", MediaType.TEXT_XML);
            Datastream dsProfile = apiMService
                    .getDatastream(context, pid, dsID, null);
            String xml = getSerializer(context)
                    .datastreamProfileToXML(pid, dsID, dsProfile, null, false);
            builder.entity(xml);
            return builder.build();
        } catch (Exception ex) {
            return handleException(ex);
        }
    }
}
