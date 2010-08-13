package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.ExternalContentManager;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.MIMETypedStream;
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
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
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
                  DOReader contentmodelReader, Date asOfDateTime, ExternalContentManager m_exExternalContentManager) throws ServerException {

        List<Extension> extensions = typeModel.getExtension();
        List<List<String>> schemaStreamsToProblemsMap = new ArrayList<List<String>>();
        for (Extension extension : extensions) {
            String name = extension.getName();


            Element reference = null;
            Source source = null;

            if (!"SCHEMA".equals(name)) {//ignore non schema extensions
                continue;
            }

            List<Element> contents = extension.getAny();
            for (Element content : contents) { //find the reference
                String tagname = content.getTagName();
                if (tagname.equals("reference")) {
                    reference = content;
                    break;
                }
            }

            if (reference == null){ //if no reference
                boolean found = false;
                for (Element content : contents) {
                    if ( content.getNodeType() == Element.ELEMENT_NODE){
                        source = new DOMSource(content);//parse the inline schema
                        found = true;
                        break;
                    }
                }
                if (!found){//empty tag
                    List<String> validationProblems = validation.getDatastreamProblems(objectDatastream.DatastreamID);
                    validationProblems.add(Errors.schemaNotFound(contentmodelReader.GetObjectPID()));
                    validation.setValid(false);
                }
            }
            else {
                String type = reference.getAttribute("type");
                String value = reference.getAttribute("value");

                if ("datastream".equalsIgnoreCase(type)) {
                    Datastream schemaDS = contentmodelReader.GetDatastream(value, asOfDateTime);
                    if (schemaDS == null) {//No schema datastream, ignore and continue
                        continue;
                    }
                    InputStream schemaStream;
                    schemaStream = schemaDS.getContentStream();
                    source = new StreamSource(schemaStream);

                } else if ("url".equalsIgnoreCase(type)){
                    InputStream schemaStream;
                    ContentManagerParams params = new ContentManagerParams(value);
                    MIMETypedStream externalContent = m_exExternalContentManager.getExternalContent(params);
                    schemaStream = externalContent.getStream();
                    source = new StreamSource(schemaStream);
                } else { //reference used, but type not recognized
                    List<String> validationProblems = validation.getDatastreamProblems(objectDatastream.DatastreamID);
                    validationProblems.add(Errors.schemaNotFound(contentmodelReader.GetObjectPID()));
                    validation.setValid(false);
                    continue;
                }

            }

            LSResourceResolver resourceResolver
                    = new ResourceResolver(context, doMgr, contentmodelReader, asOfDateTime);


            Schema schema;
            try {
                schema = parseAsSchema(source, resourceResolver);
            } catch (SAXException e) {
                List<String> validationProblems = validation.getDatastreamProblems(objectDatastream.DatastreamID);
                validationProblems.add(Errors.schemaCannotParse(contentmodelReader.GetObjectPID(),objectDatastream.DatastreamID,e));
                validation.setValid(false);
                continue;
            }

            List<String> problems = checkSchema( objectDatastream.getContentStream(),
                                                 schema,
                                                 contentmodelReader.GetObjectPID(),
                                                 objectDatastream.DatastreamID);
            schemaStreamsToProblemsMap.add(problems);
            if (problems.isEmpty()) {//if multiple SCHEMAS have been defined, only one is needed to be compliant. If one schema
                //produce no errors, do not bother validating against the others.
                break;

            }
        }


        boolean foundProblem = false;
        for (List<String> problems : schemaStreamsToProblemsMap) {
            if (!problems.isEmpty()){
                foundProblem = true;
                break;
            }
        }
        if (foundProblem) {
            validation.setValid(false);
            List<String> validationProblems = validation.getDatastreamProblems(objectDatastream.DatastreamID);
            for (List<String> problems : schemaStreamsToProblemsMap) {
                validationProblems.addAll(problems);
            }
        }
    }


    private Schema parseAsSchema(Source input, LSResourceResolver resolver) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        schemaFactory.setResourceResolver(resolver);

        schema = schemaFactory.newSchema(input);
        return schema;
    }

    public List<String> checkSchema(InputStream objectStream, Schema schema,
                                    String contentModel,
                                    String datastreamID) {

        List<String> problems = new ArrayList<String>();
        ErrorHandler errorhandler = new ReportingErrorHandler(problems, contentModel, datastreamID);

        try {
            Validator validator = schema.newValidator();
            validator.setErrorHandler(errorhandler);
            validator.validate(new StreamSource(objectStream));
        } catch (SAXException e) {
            problems.add(Errors.invalidContentInDatastream(datastreamID,contentModel,e));
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
            if (systemId.startsWith("$THIS$/")) {//other datastream in this object
                String[] tokens = systemId.split("/");
                //String is of the format $THIS$/DSID
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



        public ReportingErrorHandler(List<String> problems,
                                     String contentModel,
                                     String datastreamID) {
            this.problems = problems;
            this.contentModel = contentModel;
            this.datastreamID = datastreamID;

        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            //TODO should these be reported?
            problems.add(Errors.schemaValidationWarning(datastreamID,contentModel,exception));
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            problems.add(Errors.schemaValidationError(datastreamID,contentModel,exception));
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            problems.add(Errors.schemaValidationFatalError(datastreamID,contentModel,exception));
        }

    }
}
