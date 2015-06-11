package org.fcrepo.test.api;

import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.fcrepo.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


public class ValidatorHelper {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ValidatorHelper.class);
    
    /**
     * Get all local filenames that correspond to declared schemas. Validation
     * does not work with a "union of all schemata" schema. This was an attempt
     * create the most minimal set of schema by traversing schemaLocation and
     * &lt;include&gt; within the xsd files. Ultimately, this was a dead end, because
     * of a bug in Xerces: https://issues.apache.org/jira/browse/XERCESJ-1130
     * schemaLocation typically points to some http resource. For offline tests,
     * we want to use the local copy of that resource (since we know we have
     * them). Thus, for all declared and included schemas, produce a list of
     * local filenames.
     *
     * @param xml
     *        blob of xml that may contain schemaLocation
     * @return List of all local files corresponding to declared schemas
     * @throws Exception
     */
    public List<File> getSchemaFiles(String xml, List<File> state)
            throws Exception {

        File schemaDir =
                new File(Constants.FEDORA_HOME, "server" + File.separator
                        + "xsd");

        /* Get local copies of any declared schema */
        ArrayList<File> result = new ArrayList<File>();
        Pattern p = Pattern.compile("schemaLocation=\"(.+?)\"");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            String[] content = m.group(1).split("\\s+");
            for (String frag : content) {
                if (frag.contains(".xsd")) {
                    String[] paths = frag.split("/");
                    File newSchema =
                            new File(schemaDir, paths[paths.length - 1]);
                    if (state == null || !state.contains(newSchema)) {
                        result.add(newSchema);
                    }
                }
            }
        }

        /* For each declared schema, and get any <include> schemas from them */
        ArrayList<File> included = new ArrayList<File>();
        for (File f : result) {
            xml = IOUtils.toString(new FileInputStream(f));
            included.addAll(getSchemaFiles(xml, result));
        }

        result.addAll(included);

        return result;
    }

    /**
     * Validate XML document supplied as a string.
     * <p>
     * Validates against the local copy of the XML schema specified as
     * schemaLocation in document
     *</p>
     *
     * @param xml
     * @throws Exception
     */
    public void offlineValidate(String url, String xml, List<File> schemas)
            throws Exception {

        SchemaFactory sf =
                SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        LOGGER.info(sf.getClass().getName());

        ArrayList<Source> schemata = new ArrayList<Source>();

        for (File schemaFile : schemas) {
            schemata.add(new StreamSource(schemaFile));
        }

        Schema schema;

        try {
            schema = sf.newSchema(schemata.toArray(new Source[0]));
        } catch (SAXException e) {

            throw new RuntimeException("Could not parse schema "
                    + schemas.toString(), e);
        }
        Validator v = schema.newValidator();

        StringBuilder errors = new StringBuilder();

        v.setErrorHandler(new ValidatorErrorHandler(errors));

        v.validate(new StreamSource(new StringReader(xml)));

        assertTrue("Offline validation failed for " + url + ". Errors: "
                + errors.toString() + "\n xml:\n" + xml, 0 == errors.length());

    }
    
    /**
     * Validate XML document supplied as a string. Validates against XML schema
     * specified as schemaLocation in document
     *
     * @param xml
     * @throws Exception
     */
    public void onlineValidate(String url, String xml) throws Exception {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);

        SAXParser parser;
        parser = factory.newSAXParser();
        parser
                .setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                             "http://www.w3.org/2001/XMLSchema");

        StringBuilder errors = new StringBuilder();

        XMLReader reader = parser.getXMLReader();
        reader.setEntityResolver(new ValidatorEntityResolver());
        reader.setErrorHandler(new ValidatorErrorHandler(errors));
        reader.parse(new InputSource(new StringReader(xml)));
        if (errors.length() > 0) LOGGER.warn(xml);
        assertTrue("Online Validation failed for " + url + ". Errors: "
                + errors.toString(), 0 == errors.length());

    }

}
