package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.ecm.jaxb.DsTypeModel;
import org.fcrepo.server.validation.ecm.jaxb.Extension;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 24, 2010
 * Time: 10:11:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class SchemaValidator {
    private RepositoryReader doMgr;

    public SchemaValidator(RepositoryReader doMgr) {

        //To change body of created methods use File | Settings | File Templates.
        this.doMgr = doMgr;
    }

    void validate(Context context, DsTypeModel typeModel, Datastream objectDatastream, Validation validation,
                  DOReader contentmodelReader, Date asOfDateTime) throws ServerException {

        List<Extension> extensions = typeModel.getExtension();
        Map<String, List<String>> schemaStreamsToProblemsMap = new HashMap<String, List<String>>();
        for (Extension extension : extensions) {
            String name = extension.getName();
            if ("SCHEMA".equals(name)) {
                List<Element> contents = extension.getAny();
                Element reference = null;
                for (Element content : contents) {
                    String tagname = content.getTagName();
                    if (tagname.equals("reference")) {
                        reference = content;
                        break;
                    }
                }
                if (reference != null) {
                    String type = reference.getAttribute("type");
                    if (!"xsd".equals(type)) {
                        continue;
                    }
                    String datastream = reference.getAttribute("datastream");
                    Datastream schemaDS = contentmodelReader.GetDatastream(datastream, asOfDateTime);
                    if (schemaDS == null) {//No schema datastream, ignore and continue
                        continue;
                    }

                    LSResourceResolver resourceResolver
                            = new ResourceResolver(context, doMgr, contentmodelReader, asOfDateTime);

                    List<String> problems = checkSchema(resourceResolver, objectDatastream.getContentStream(),
                                                        schemaDS.getContentStream(),
                                                        contentmodelReader.GetObjectPID(),
                                                        objectDatastream.DatastreamID, schemaDS.DatastreamID);
                    schemaStreamsToProblemsMap.put(datastream, problems);
                    if (problems
                            .isEmpty()) {//if multiple SCHEMAS have been defined, only one is needed to be compliant. If one schema
                        //produce no errors, do not bother validating against the others.
                        break;
                    }
                }
            }

        }
        boolean foundProblem = false;
        for (String schemaStream : schemaStreamsToProblemsMap.keySet()) {
            if (!schemaStreamsToProblemsMap.get(schemaStream).isEmpty()) {
                foundProblem = true;
                break;
            }
        }
        if (foundProblem) {
            validation.setValid(false);
            List<String> validationProblems = validation.getDatastreamProblems(objectDatastream.DatastreamID);
            for (String schemaStream : schemaStreamsToProblemsMap.keySet()) {
                validationProblems.addAll(schemaStreamsToProblemsMap.get(schemaStream));
            }
        }

    }


    public List<String> checkSchema(LSResourceResolver resolver, InputStream objectStream, InputStream schemaStream,
                                    String contentModel,
                                    String datastreamID, String schemaID) {
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        schemaFactory.setResourceResolver(resolver);
        List<String> problems = new ArrayList<String>();
        ErrorHandler errorhandler = new ReportingErrorHandler(problems, contentModel, datastreamID, schemaID);


        try {
            schema = schemaFactory.newSchema(new StreamSource(schemaStream));
        } catch (SAXException e) {
            problems.add(Errors.unableToParseSchema(schemaID,datastreamID,contentModel,e));
            return problems;
        }
        try {
            Validator validator = schema.newValidator();
            validator.setErrorHandler(errorhandler);
            validator.validate(new StreamSource(objectStream));
        } catch (SAXException e) {
            problems.add(Errors.invalidContentInDatastream(datastreamID,schemaID,contentModel,e));
        } catch (IOException e) {
            problems.add(Errors.unableToReadDatastream(datastreamID,e));
        }
        return problems;
    }


    public static class ResourceResolver implements LSResourceResolver {
        private Context context;
        private RepositoryReader doMgr;
        private DOReader contentmodelReader;
        private Date asOfDateTime;

        public ResourceResolver(Context context,
                                RepositoryReader doMgr,
                                DOReader contentmodelReader, Date asOfDateTime) {
            //To change body of created methods use File | Settings | File Templates.
            this.context = context;
            this.doMgr = doMgr;
            this.contentmodelReader = contentmodelReader;
            this.asOfDateTime = asOfDateTime;
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
                                       String baseURI) {
            //The key here is SystemID.
            if (systemId == null) {
                return null;
            }
            if (systemId.startsWith("../../")) { //other object
                String[] tokens = systemId.split("/");
                //String is of the format ../../pid/DSID
                try {
                    DOReader otherobject = doMgr.getReader(false, context, tokens[2]);
                    Datastream schemastream = otherobject.GetDatastream(tokens[3], asOfDateTime);
                    LSInput input = new MyLSInput(schemastream);
                    input.setBaseURI(baseURI);
                    input.setPublicId(publicId);
                    input.setBaseURI(baseURI);
                    return input;
                } catch (ServerException e) {
                    return null;
                }

            } else if (systemId.startsWith("../")) {//other datastream in this object
                String[] tokens = systemId.split("/");
                //String is of the format ../DSID
                try {
                    final Datastream schemastream = contentmodelReader.GetDatastream(tokens[1], asOfDateTime);
                    LSInput input = new MyLSInput(schemastream);
                    input.setBaseURI(baseURI);
                    input.setPublicId(publicId);
                    input.setBaseURI(baseURI);
                    return input;
                } catch (ServerException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        public static class MyLSInput implements LSInput {

            Datastream stream;

            public MyLSInput(Datastream stream) {
                this.stream = stream;
            }

            private String systemId, publicId, baseURI;

            @Override
            public Reader getCharacterStream() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void setCharacterStream(Reader characterStream) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public InputStream getByteStream() {
                try {
                    return stream.getContentStream();
                } catch (StreamIOException e) {
                    return null;
                }
            }

            @Override
            public void setByteStream(InputStream byteStream) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getStringData() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void setStringData(String stringData) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getSystemId() {
                return systemId;
            }

            @Override
            public void setSystemId(String systemId) {
                this.systemId = systemId;
            }

            @Override
            public String getPublicId() {
                return publicId;
            }

            @Override
            public void setPublicId(String publicId) {
                this.publicId = publicId;
            }

            @Override
            public String getBaseURI() {
                return baseURI;
            }

            @Override
            public void setBaseURI(String baseURI) {
                this.baseURI = baseURI;
            }

            @Override
            public String getEncoding() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void setEncoding(String encoding) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean getCertifiedText() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void setCertifiedText(boolean certifiedText) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        }

        ;
    }

    /**
     * Errorhandler used by ECM to get errors reported correctly.
     */
    public static class ReportingErrorHandler implements ErrorHandler {

        private List<String> problems;
        private String contentModel;
        private String datastreamID;
        private String schemaID;


        public ReportingErrorHandler(List<String> problems,
                                     String contentModel,
                                     String datastreamID, String schemaID) {
            this.problems = problems;
            this.contentModel = contentModel;
            this.datastreamID = datastreamID;
            this.schemaID = schemaID;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            //TODO should these be reported?
            problems.add(Errors.schemaValidationWarning(datastreamID,schemaID,contentModel,exception));
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            problems.add(Errors.schemaValidationError(datastreamID,schemaID,contentModel,exception));
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            problems.add(Errors.schemaValidationFatalError(datastreamID,schemaID,contentModel,exception));
        }

    }
}
