/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import fedora.server.errors.ObjectValidityException;
import fedora.server.errors.ServerException;

import fedora.utilities.XmlTransformUtility;

/**
 * Schematron validation for fedora objects encoded in schematron schema for
 * Fedora. The schematron schema (metsExtRules1-0.xml) expresses a set of rules
 * using XPATH that enable us to check for things that are either not expressed
 * in the METS XML schema, or that cannot be expressed with XML Schema language.
 * Generally we will look for things that are requirements of Fedora objects,
 * which are not requirements for METS objects in general.
 * 
 * @author Sandy Payette
 * @version $Id$
 */
public class DOValidatorSchematron {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DOValidatorSchematron.class.getName());

    private StreamSource rulesSource;

    private StreamSource preprocessorSource;

    private final StreamSource validatingStyleSheet;

    private static Map<String, ByteArrayOutputStream> generatedStyleSheets = 
        new HashMap<String, ByteArrayOutputStream>();

    /**
     * Constructs a DOValidatorSchematron instance with a Schematron
     * preprocessor that is provided by the calling class. This will allow the
     * DOValidator module to pass in the preprocessor that is configured with
     * the Fedora repository.
     * 
     * @param schemaPath
     *        the URL of the Schematron schema
     * @param preprocessorPath
     *        the location of the Schematron preprocessor
     * @param phase
     *        the phase in the fedora object lifecycle to which validation
     *        should pertain. (Currently options are "ingest" and "store"
     * @throws ObjectValidityException
     */
    public DOValidatorSchematron(String schemaPath,
                                 String preprocessorPath,
                                 String phase)
            throws ObjectValidityException {
        validatingStyleSheet = setUp(preprocessorPath, schemaPath, phase);
    }

    /**
     * Run the Schematron validation on a Fedora object.
     * 
     * @param objectAsFile
     *        the Fedora object as a File
     * @throws ServerException
     */
    public void validate(File objectAsFile) throws ServerException {
        validate(new StreamSource(objectAsFile));
    }

    /**
     * Run the Schematron validation on a Fedora object.
     * 
     * @param objectAsStream
     *        the Fedora object as an Inputstream
     * @throws ServerException
     */
    public void validate(InputStream objectAsStream) throws ServerException {
        validate(new StreamSource(objectAsStream));
    }

    /**
     * Run the Schematron validation on a Fedora object.
     * 
     * @param objectSource
     *        the Fedora object as an StreamSource
     * @throws ServerException
     */
    public void validate(StreamSource objectSource) throws ServerException {
        DOValidatorSchematronResult result = null;
        try {
            // Create a transformer that uses the validating stylesheet.
            // Run the Schematron validation of the Fedora object and
            // output results in DOM format.
            TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
            Transformer vtransformer =
                    tfactory.newTransformer(validatingStyleSheet);
            DOMResult validationResult = new DOMResult();
            vtransformer.transform(objectSource, validationResult);
            result = new DOValidatorSchematronResult(validationResult);
        } catch (Exception e) {
            LOG.error("Schematron validation failed", e);
            throw new ObjectValidityException(e.getMessage());
        }

        if (!result.isValid()) {
            String msg = null;
            try {
                msg = result.getXMLResult();
            } catch (Exception e) {
                LOG
                        .warn("Error getting XML result of schematron validation failure",
                              e);
            }
            throw new ObjectValidityException(msg);
        }
    }

    /**
     * Run setup to prepare for Schematron validation. This entails dynamically
     * creating the validating stylesheet using the preprocessor and the schema.
     * 
     * @param preprocessorPath
     *        the location of the Schematron preprocessor
     * @param fedoraschemaPath
     *        the URL of the Schematron schema
     * @param phase
     *        the phase in the fedora object lifecycle to which validation
     *        should pertain. (Currently options are "ingest" and "store")
     * @return StreamSource
     * @throws ObjectValidityException
     */
    private StreamSource setUp(String preprocessorPath,
                               String fedoraschemaPath,
                               String phase) throws ObjectValidityException {
        String key = fedoraschemaPath + "#" + phase;
        ByteArrayOutputStream out =
                (ByteArrayOutputStream) generatedStyleSheets.get(key);
        if (out == null) {
            rulesSource = fileToStreamSource(fedoraschemaPath);
            preprocessorSource = fileToStreamSource(preprocessorPath);
            out =
                    createValidatingStyleSheet(rulesSource,
                                               preprocessorSource,
                                               phase);
            generatedStyleSheets.put(key, out);
        }
        return new StreamSource(new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Create the validating stylesheet which will be used to perform the actual
     * Schematron validation. The validating stylesheet is created dynamically
     * using the preprocessor stylesheet and the Schematron schema for Fedora.
     * The phase is key. The stylesheet is created for the appropriate phase as
     * specified in the Schematron rules schema. Valid work flow phases are
     * currently "ingest" and "store." Different schematron rules apply to
     * different phases of the object lifecycle. Some rules are applied when an
     * object is first being ingested into the repository. Other rules apply
     * before the object is put into permanent storage.
     * 
     * @param rulesSource
     *        the location of the rules
     * @param preprocessorSource
     *        the location of the Schematron preprocessor
     * @param phase
     *        the phase in the fedora object lifecycle to which validation
     *        should pertain. (Currently options are "ingest" and "store"
     * @return A ByteArrayOutputStream containing the stylesheet
     * @throws ObjectValidityException
     */
    private ByteArrayOutputStream createValidatingStyleSheet(StreamSource rulesSource,
                                                             StreamSource preprocessorSource,
                                                             String phase)
            throws ObjectValidityException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // Create a transformer for that uses the Schematron preprocessor stylesheet.
            // Transform the Schematron schema (rules) into a validating stylesheet.
            TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
            Transformer ptransformer =
                    tfactory.newTransformer(preprocessorSource);
            ptransformer.setParameter("phase", phase);
            ptransformer.transform(rulesSource, new StreamResult(out));
        } catch (TransformerException e) {
            LOG.error("Schematron validation failed", e);
            throw new ObjectValidityException(e.getMessage());
        }
        return out;
    }

    /** Code based on com.jclark.xsl.sax.Driver: * */
    /**
     * Generates a StreamSource from a file name.
     */
    static public StreamSource fileToStreamSource(String str) {
        return fileToStreamSource(new File(str));
    }

    static public StreamSource fileToStreamSource(File file) {
        String path = file.getAbsolutePath();
        String fSep = System.getProperty("file.separator");
        if (fSep != null && fSep.length() == 1) {
            path = path.replace(fSep.charAt(0), '/');
        }
        if (path.length() > 0 && path.charAt(0) != '/') {
            path = '/' + path;
        }
        try {
            String url = new URL("file", null, path).toString();
            return new StreamSource(url);
        } catch (java.net.MalformedURLException e) {
            /*
             * According to the spec this could only happen if the file protocol
             * were not recognized.
             */
            throw new Error("unexpected MalformedURLException");
        }
    }
}
