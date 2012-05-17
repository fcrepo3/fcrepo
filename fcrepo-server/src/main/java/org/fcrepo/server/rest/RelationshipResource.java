/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.rest;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fcrepo.common.PID;
import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.server.storage.types.TupleArrayTripleIterator;
import org.springframework.stereotype.Component;
import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TrippiException;


/**
 * A rest controller to handle CRUD operations for the Fedora relationships API.
 *
 * @author Edwin Shin
 * @version $Id$
 * @since 3.4.0
 */
@Path("/{pid : ([A-Za-z0-9]|-|\\.)+:(([A-Za-z0-9])|-|\\.|~|_|(%[0-9A-F]{2}))+}/relationships")
@Component
public class RelationshipResource extends BaseRestResource {

    public RelationshipResource(Server server) {
        super(server);
    }

    /**
     * Get relationships asserted by the object denoted by <i>pid</i>.
     *
     * @param pid The pid of the Fedora object, e.g. demo:1.
     * @param subject The subject uri. If null, defaults to the URI form of pid,
     *                e.g. info:fedora/demo:1.
     * @param predicate The predicate uri or null to match any predicate.
     * @param format one of "rdf/xml", "n-triples", "turtle", or "sparql".
     *               If null, defaults to rdf/xml.
     * @return the relationships in the specified format.
     */
    @GET
    @Produces({"application/rdf+xml", "text/plain", "application/x-turtle",
        "application/sparql-results+xml"})
    public Response getRelationships(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.SUBJECT)
            String subject,
            @QueryParam(RestParam.PREDICATE)
            String predicate,
            @QueryParam(RestParam.FORMAT)
            @DefaultValue("rdf/xml")
            String format) {
        Context context = getContext();
        if (subject == null) {
            // assume the subject is the object as denoted by the pid
            subject = PID.toURI(pid);
        }
        try {
            RelationshipTuple[] tuples = m_management.getRelationships(context, subject, predicate);
            TripleIterator it = new TupleArrayTripleIterator(new ArrayList<RelationshipTuple>(Arrays.asList(tuples)));
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            format = format.toLowerCase();
            RDFFormat outputFormat;
            MediaType mediaType;
            if (format.equalsIgnoreCase("xml") || format.equals("rdf/xml")) {
                outputFormat = RDFFormat.RDF_XML;
                mediaType = new MediaType("application", "rdf+xml");
            } else if (format.equals("n-triples") || format.equals("ntriples")) {
                outputFormat = RDFFormat.N_TRIPLES;
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            } else if (format.equals("turtle")) {
                outputFormat = RDFFormat.TURTLE;
                mediaType = new MediaType("application", "x-turtle");
            } else if (format.equals("sparql")) {
                outputFormat = RDFFormat.SPARQL;
                mediaType = new MediaType("application", "sparql-results+xml");
            } else {
                throw new IllegalArgumentException("unknown format: " + format);
            }
            it.toStream(out, outputFormat);
            return Response.ok(out.toString("UTF-8"), mediaType).build();
        } catch (ServerException e) {
            return handleException(e);
        } catch (TrippiException e) {
            return handleException(e);
        } catch (UnsupportedEncodingException e) {
            return handleException(e);
        }
    }

    /**
     * Add a relationship.
     * <p/>
     * POST /objects/{pid}/relationships/new ? subject predicate object isLiteral datatype
     * <p/>
     * Successful Response:
     * Status: 200 OK
     */
    @Path("/new")
    @POST
    public Response addRelationship(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.SUBJECT)
            String subject,
            @QueryParam(RestParam.PREDICATE)
            String predicate,
            @QueryParam(RestParam.OBJECT)
            String object,
            @QueryParam(RestParam.IS_LITERAL)
            boolean isLiteral,
            @QueryParam(RestParam.DATATYPE)
            String datatype) {
        Context context = getContext();
        try {
            if (subject == null) {
                // assume the subject is the object as denoted by the pid
                subject = PID.toURI(pid);
            }
            boolean result = m_management.addRelationship(context, subject, predicate, object, isLiteral, datatype);
            return Response.ok(Boolean.toString(result)).build(); // needs an entity to not be overridden with a 204
        } catch (ServerException e) {
            return handleException(e);
        }
    }

    /**
     * Delete a relationship.
     * <p/>
     * DELETE /objects/{pid}/relationships ? subject predicate object isLiteral datatype
     * <p/>
     * Successful Response:
     * Status: 200 OK
     */
    @DELETE
    public Response purgeRelationship(
            @PathParam(RestParam.PID)
            String pid,
            @QueryParam(RestParam.SUBJECT)
            String subject,
            @QueryParam(RestParam.PREDICATE)
            String predicate,
            @QueryParam(RestParam.OBJECT)
            String object,
            @QueryParam(RestParam.IS_LITERAL)
            boolean isLiteral,
            @QueryParam(RestParam.DATATYPE)
            String datatype) {
        Context context = getContext();
        try {
            if (subject == null) {
                // assume the subject is the object as denoted by the pid
                subject = PID.toURI(pid);
            }
            boolean result = m_management.purgeRelationship(context, subject, predicate, object, isLiteral, datatype);
            return Response.ok(Boolean.toString(result), MediaType.TEXT_PLAIN_TYPE).build();
        } catch (ServerException e) {
            return handleException(e);
        }
    }
}
