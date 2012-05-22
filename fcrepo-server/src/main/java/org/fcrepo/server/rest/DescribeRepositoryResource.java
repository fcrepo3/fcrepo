package org.fcrepo.server.rest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.servlet.ServletException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.Server;
import org.fcrepo.server.access.RepositoryInfo;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implements the "describeRepository" functionality of the Fedora Access LITE
 * (API-A-LITE) interface using a java servlet front end. The syntax defined by
 * API-A-LITE has for getting a description of the repository has the following
 * binding:
 * <ol>
 * <li>describeRepository URL syntax:
 * protocol://hostname:port/fedora/describe[?xml=BOOLEAN] This syntax requests
 * information about the repository. The xml parameter determines the type of
 * output returned. If the parameter is omitted or has a value of "false", a
 * MIME-typed stream consisting of an html table is returned providing a
 * browser-savvy means of viewing the object profile. If the value specified is
 * "true", then a MIME-typed stream consisting of XML is returned.</li>
 * <ul>
 * <li>protocol - either http or https.</li>
 * <li>hostname - required hostname of the Fedora server.</li>
 * <li>port - required port number on which the Fedora server is running.</li>
 * <li>fedora - required name of the Fedora access service.</li>
 * <li>describe - required verb of the Fedora service.</li>
 * <li>xml - an optional parameter indicating the requested output format. A
 * value of "true" indicates a return type of text/xml; the absence of the xml
 * parameter or a value of "false" indicates format is to be text/html.</li>
 * </ul>
 *
 * @author Ross Wayland
 * @author Benjamin Armintor armintor@gmail.com
 * @version $Id$
 */

@Path("/")
@Component
public class DescribeRepositoryResource
        extends BaseRestResource
        implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(DescribeRepositoryResource.class);

    private static final long serialVersionUID = 1L;

    /** Content type for html. */
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    /** Content type for xml. */
    private static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";

    public DescribeRepositoryResource(Server server) {
        super(server);
    }

    /**
     * <p>
     * Process Fedora Access Request. Parse and validate the servlet input
     * parameters and then execute the specified request.
     * </p>
     *
     * @param request
     *        The servlet request.
     * @param response
     *        servlet The servlet response.
     * @throws ServletException
     *         If an error occurs that effects the servlet's basic operation.
     * @throws IOException
     *         If an error occurs with an input or output operation.
     */
    @Path("/")
    @GET
    public Response describe(@QueryParam("xml")
                          @DefaultValue("false")
                          boolean xml)
            throws ServletException, IOException {

        Context context = getContext();
        try {
            return describeRepository(context, xml);
        } catch (Exception ae) {
            return handleException(ae, false);
        }

    }

    @Path("/")
    @POST
    public Response postDescribe(@QueryParam("xml") @DefaultValue("false") boolean xml)
        throws ServletException, IOException {
        return describe(xml);
    }

    public Response describeRepository(Context context,
                                   boolean xml)
            throws ServerException {

        RepositoryInfo repositoryInfo = null;

        try {
            repositoryInfo = m_access.describeRepository(context);
            if (repositoryInfo != null) {
                // Repository info obtained.
                // Serialize the RepositoryInfo object into XML
                ReposInfoSerializer result = new ReposInfoSerializer(context, repositoryInfo);
                if (xml) {
                    // Return results as raw XML
                    return Response.ok(result,CONTENT_TYPE_XML).build();

                } else {
                    // Transform results into an html table
                    return Response.ok(new HtmlTransformation(context, result),CONTENT_TYPE_HTML).build();
                }
            } else {
                // Describe request returned nothing.
                logger.error("No repository info returned");
                return Response.noContent().build();
            }
        } catch (AuthzException ae) {
            throw ae;
        } catch (Throwable th) {
            String msg = "Error describing repository";
            logger.error(msg, th);
            throw new GeneralException(msg, th);
        }
    }

    /**
     * <p>
     * Serializes RepositoryInfo object into XML.
     * </p>
     */
    public class ReposInfoSerializer
            implements StreamingOutput {

        private RepositoryInfo repositoryInfo = null;

        /**
         * <p>
         * Constructor for ReposInfoSerializerThread.
         * </p>
         *
         * @param repositoryInfo
         *        A repository info data structure.
         * @param pw
         *        A PipedWriter to which the serialization info is written.
         */
        public ReposInfoSerializer(Context context,
                                   RepositoryInfo repositoryInfo) {
            this.repositoryInfo = repositoryInfo;
        }

        /**
         * <p>
         * This method executes the thread.
         * </p>
         */
        @Override
        public void write(OutputStream output)
                throws IOException {
            OutputStreamWriter pw = new OutputStreamWriter(output,"UTF-8");
            pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.write("<fedoraRepository"
                    + " xmlns=\"" + REPO_DESC1_0.namespace.uri + "\""
                    + " xmlns:xsd=\"" + XML_XSD.uri + "\""
                    + " xmlns:xsi=\"" + XSI.uri + "\""
                    + " xsi:schemaLocation=\"" + ACCESS.uri + " "
                    + REPO_DESC1_0.xsdLocation + "\">");

            // REPOSITORY INFO FIELDS SERIALIZATION
            pw.write("<repositoryName>" + repositoryInfo.repositoryName
                     + "</repositoryName>");
            pw.write("<repositoryBaseURL>"
                    + repositoryInfo.repositoryBaseURL
                    + "</repositoryBaseURL>");
            pw.write("<repositoryVersion>"
                    + repositoryInfo.repositoryVersion
                    + "</repositoryVersion>");
            pw.write("<repositoryPID>");
            pw.write("    <PID-namespaceIdentifier>"
                    + repositoryInfo.repositoryPIDNamespace
                    + "</PID-namespaceIdentifier>");
            pw.write("    <PID-delimiter>" + ":" + "</PID-delimiter>");
            pw.write("    <PID-sample>" + repositoryInfo.samplePID
                     + "</PID-sample>");
            String[] retainPIDs = repositoryInfo.retainPIDs;
            for (String element : retainPIDs) {
                pw.write("    <retainPID>" + element + "</retainPID>");
            }
            pw.write("</repositoryPID>");
            pw.write("<repositoryOAI-identifier>");
            pw.write("    <OAI-namespaceIdentifier>"
                    + repositoryInfo.OAINamespace
                    + "</OAI-namespaceIdentifier>");
            pw.write("    <OAI-delimiter>" + ":" + "</OAI-delimiter>");
            pw.write("    <OAI-sample>"
                    + repositoryInfo.sampleOAIIdentifer
                    + "</OAI-sample>");
            pw.write("</repositoryOAI-identifier>");
            pw.write("<sampleSearch-URL>"
                    + repositoryInfo.sampleSearchURL
                    + "</sampleSearch-URL>");
            pw.write("<sampleAccess-URL>"
                    + repositoryInfo.sampleAccessURL
                    + "</sampleAccess-URL>");
            pw.write("<sampleOAI-URL>" + repositoryInfo.sampleOAIURL
                     + "</sampleOAI-URL>");
            String[] emails = repositoryInfo.adminEmailList;
            for (String element : emails) {
                pw.write("<adminEmail>" + element + "</adminEmail>");
            }
            pw.write("</fedoraRepository>");
            pw.flush();
            pw.close();
        }
    }

    public class HtmlTransformation implements StreamingOutput {
        private final ReposInfoSerializer reposInfo;
        private final Context context;
        public HtmlTransformation(Context context, ReposInfoSerializer input) {
            this.reposInfo = input;
            this.context = context;
        }

        @Override
        public void write(OutputStream out) throws IOException {
            File xslFile =
                    new File(m_server.getHomeDir(),
                             "access/viewRepositoryInfo.xslt");
            PipedOutputStream po = new PipedOutputStream();
            PipedInputStream pr = new PipedInputStream(po);
            this.reposInfo.write(po);
            try {
                TransformerFactory factory =
                        XmlTransformUtility.getTransformerFactory();
                Templates template =
                        factory.newTemplates(new StreamSource(xslFile));
                Transformer transformer = template.newTransformer();
                transformer.setParameter("fedora", context
                                         .getEnvironmentValue(FEDORA_APP_CONTEXT_NAME));
                transformer.transform(new StreamSource(pr),
                                  new StreamResult(out));
            } catch (TransformerException te) {
                throw new IOException("Transform error" + te.toString(), te);
            }

            out.flush();
        }
    }

}

