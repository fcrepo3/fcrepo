package org.fcrepo.server.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fcrepo.server.Server;
import org.springframework.stereotype.Component;


@Path("/")
@Component
public class SchemaResource extends BaseRestResource {
    private final File schemaDir;
    public SchemaResource(Server server) {
        super(server);
        schemaDir = new File(server.getHomeDir(),"xsd");
    }

    @GET
    @Path("/{schema : \\w+\\.((dtd)|(xsd))}")
    public Response getSchema(@PathParam("schema") String schemaName) {
        File schema = new File(schemaDir, schemaName);
        if (schema.exists()){
            String mime = (schemaName.endsWith("xsd"))? MediaType.TEXT_XML : MediaType.TEXT_PLAIN;
            try{
                return Response.ok(new FileInputStream(schema), mime).build();
            } catch (IOException ioe) {
                return handleException(ioe, false);
            }
        } else {
            return Response.status(404).build();
        }
    }

}
