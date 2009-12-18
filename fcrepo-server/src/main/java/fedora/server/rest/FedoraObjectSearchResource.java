/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.rest;

import java.io.CharArrayWriter;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fedora.server.Context;
import fedora.server.search.Condition;
import fedora.server.search.FieldSearchQuery;
import fedora.server.search.FieldSearchResult;

/**
 * Implement /objects REST API (search)
 *
 * GET /objects ? terms query sessionToken maxResults format
 *
 * @author cuong.tran@yourmediashelf.com
 * @version $Id$
 */
@Path("/objects")
public class FedoraObjectSearchResource extends BaseRestResource {
    static final String[] SEARCHABLE_FIELDS = { "pid", "label", "state", "ownerId",
            "cDate", "mDate", "dcmDate", "title", "creator", "subject", "description",
            "publisher", "contributor", "date", "type", "format", "identifier",
            "source", "language", "relation", "coverage", "rights" };

    @GET
    @Produces( { HTML, XML })
    public Response searchObjects(
            @QueryParam("terms")
            String terms,
            @QueryParam("query")
            String query,
            @QueryParam("maxResults")
            @DefaultValue("25")
            int maxResults,
            @QueryParam("sessionToken")
            String sessionToken,
            @QueryParam("resultFormat")
            @DefaultValue(HTML)
            String format) {

        try {
            Context context = getContext();
            String[] wantedFields = getWantedFields(servletRequest);
            MediaType mime = RestHelper.getContentType(format);

            FieldSearchResult result = null;

            if (wantedFields.length > 0 || sessionToken != null) {
                if (sessionToken != null) {
                    result = apiAService.resumeFindObjects(context, sessionToken);
                } else {
                    if ((terms != null) && (terms.length() != 0)) {
                        result = apiAService.findObjects(context, wantedFields, maxResults, new FieldSearchQuery(terms));
                    } else {
                        result = apiAService.findObjects(context, wantedFields, maxResults, new FieldSearchQuery(Condition.getConditions(query)));
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
            return handleException(ex);
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
    @Path("nextPID")
    @POST
    public Response getNextPID(
            @QueryParam("numPIDs")
            @DefaultValue("1")
            int numPIDS,
            @QueryParam(RestParam.NAMESPACE)
            String namespace,
            @QueryParam("format")
            @DefaultValue(HTML)
            String format) throws Exception {

        try {
            Context context = getContext();
            String[] pidList = apiMService.getNextPID(context, numPIDS, namespace);
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
            return handleException(ex);
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
}
