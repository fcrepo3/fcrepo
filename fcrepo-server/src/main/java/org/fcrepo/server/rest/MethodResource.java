/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import java.io.Reader;
import java.util.Date;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.storage.types.Property;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.springframework.stereotype.Component;


/**
 * A rest controller to handle listing and invoking all Service Definition
 * methods in a digital object.
 *
 * @author Chris Wilper
 * @version $Id$
 */
@Path("/{pid : ([A-Za-z0-9]|-|\\.)+:(([A-Za-z0-9])|-|\\.|~|_|(%[0-9A-F]{2}))+}/methods")
@Component
public class MethodResource extends BaseRestResource {

    public MethodResource(Server server) {
        super(server);
    }

    /**
     * Lists all Service Definitions methods that can be invoked on a digital
     * object.
     *
     * GET /objects/{pid}/methods ? format asOfDateTime
     */
    @GET
    @Produces({ HTML, XML })
    public Response getAllObjectMethods(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dTime,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(HTML)
            String format,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        return getObjectMethodsForSDefImpl(pid, null, dTime, format, flash);
    }

    /**
     * Lists all Service Definitions methods that can be invoked on a digital
     * object, for the supplied Service Definition.
     *
     * GET /objects/{pid}/methods/{sDef} ? format asOfDateTime
     */
    @Path("/{sDef}")
    @GET
    @Produces({ HTML, XML })
    public Response getObjectMethodsForSDef(
            @PathParam(RestParam.PID)
            String pid,
            @PathParam(RestParam.SDEF)
            String sDef,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dTime,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue(HTML)
            String format,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        return getObjectMethodsForSDefImpl(pid, sDef, dTime, format, flash);
    }

    /**
     * Invokes a Service Definition method on an object, using GET.
     */
    @Path("/{sDef}/{method}")
    @GET
    public Response invokeSDefMethodUsingGET(
            @javax.ws.rs.core.Context
            UriInfo uriInfo,
            @PathParam(RestParam.PID)
            String pid,
            @PathParam(RestParam.SDEF)
            String sDef,
            @PathParam(RestParam.METHOD)
            String method,
            @QueryParam(RestParam.AS_OF_DATE_TIME)
            String dTime,
            @QueryParam(RestParam.FLASH)
            @DefaultValue("false")
            boolean flash) {
        try {
            Date asOfDateTime = DateUtility.parseDateOrNull(dTime);
            return buildResponse(m_access.getDissemination(
                    getContext(),
                    pid,
                    sDef,
                    method,
                    toProperties(uriInfo.getQueryParameters(),
                                 asOfDateTime != null),
                    asOfDateTime));
        } catch (Exception e) {
            return handleException(e, flash);
        }
    }

    private Response getObjectMethodsForSDefImpl(String pid, String sDef, String dTime, String format, boolean flash) {
        try {
            Date asOfDateTime = DateUtility.parseDateOrNull(dTime);
            Context context = getContext();
            ObjectMethodsDef[] methodDefs = m_access.listMethods(context, pid, asOfDateTime);
            ReadableCharArrayWriter out = new ReadableCharArrayWriter(1024);
            getSerializer(context)
                    .objectMethodsToXml(methodDefs, pid, sDef, asOfDateTime, out);

            out.close();
            MediaType mime = RestHelper.getContentType(format);

            if (TEXT_HTML.isCompatible(mime)) {
                Reader reader = out.toReader();
                out = new ReadableCharArrayWriter(1024);
                transform(reader, "access/listMethods.xslt", out);
                out.close();
            }

            return Response.ok(out.toReader(), mime).build();
        } catch (Exception e) {
            return handleException(e, flash);
        }
    }

    private static Property[] toProperties(MultivaluedMap<String, String> map,
                                           boolean omitDateTime) {
        Property[] props;
        if (omitDateTime) {
            props = new Property[map.size() - 1];
        } else {
            props = new Property[map.size()];
        }
        int i = 0;
        for (String key: map.keySet()) {
            if (!key.equals(RestParam.AS_OF_DATE_TIME)) {
                props[i++] = new Property(key, map.getFirst(key));
            }
        }
        return props;
    }

}
