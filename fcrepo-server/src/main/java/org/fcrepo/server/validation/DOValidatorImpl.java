/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectValidityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.utilities.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


/**
 * The implementation of the digital object validation module (see
 * DOValidator.class and DOValidatorModule.class). The validator operates on
 * digital object XML files encoded in one of the Fedora-supported encoding
 * formats (i.e., FOXML, Fedora METS, and possibly others in the future). The
 * following types of validation can be run:
 *
 * <pre>
 *   0=VALDIATE_ALL : All validation will be done.
 *   1=VALIDATE_XML_SCHEMA : the digital object will be validated against
 *                 the the appropriate XML Schema. An ObjectValidityException
 *                 will be thrown if the object fails the schema test.
 *   2=VALIDATE_SCHEMATRON : the digital object will be validated
 *                 against a set of rules expressed by a Schematron schema.
 *                 These rules are beyond what can be expressed in XML Schema.
 *                 The Schematron schema expresses rules for different phases
 *                 of the object. There are rules appropriate to a digital
 *                 object when it is first ingested into the repository
 *                 (ingest phase). There are additional rules that must be met
 *                 before a digital object is considered valid for permanent
 *                 storage in the repository (completed phase). These rules
 *                 pertain to aspects of the object that are system assigned,
 *                 such as created dates and state codes.
 *                 An ObjectValidityException will be thrown if the object fails
 *                 the Fedora rules test.
 * </pre>
 *
 * @author Sandy Payette
 * @version $Id$
 */
public class DOValidatorImpl
        implements DOValidator {

    private static final Logger logger =
            LoggerFactory.getLogger(DOValidatorImpl.class);

    protected static boolean debug = true;

    /** Configuration variable: tempdir is a working area for validation */
    protected static String tempDir = null;

    /**
     * Configuration variable: xmlSchemaPath is the location of the XML Schema.
     */
    protected static String xmlSchemaPath = null;

    /**
     * Configuration variable: schematronPreprocessorPath is the Schematron
     * stylesheet that is used to transform a Schematron schema into a
     * validating stylesheet based on the rules in the schema.
     */
    protected static String schematronPreprocessorPath = null;

    /**
     * Configuration variable: schematronSchemaPath is the Schematron schema
     * that expresses Fedora-specific validation rules. It is transformed into a
     * validating stylesheet by the Schematron preprocessing stylesheet.
     */
    protected static String schematronSchemaPath = null;

    /**
     * Map of XML Schemas configured with the Fedora Repository. key = format
     * uri value = schema file path
     */
    private final Map<String, DOValidatorXMLSchema> m_xmlSchemaMap;

    /**
     * Map of Schematron rule schemas configured with the Fedora Repository. key =
     * format uri value = schema file path
     */
    private final Map<String, String> m_ruleSchemaMap;
    
    private final File m_tempDir;
    
    private final String m_absoluteTempPath;

    /**
     * <p>
     * Constructs a new DOValidatorImpl to support all forms of digital object
     * validation, using specified values for configuration values.
     * </p>
     * <p>
     * Any parameter may be given as null, in which case the default value is
     * assumed.
     * </p>
     *
     * @param tempDir
     *        Working area for validation, default is <i>temp/</i>
     * @param xmlSchemaMap
     *        Location of XML Schemas (W3 Schema) configured with Fedora (see
     *        Fedora.fcfg). Current options are <i>xsd/foxml1-1.xsd</i> for
     *        FOXML or <i>xsd/mets-fedora-ext1-1.xsd</i> for METS (Fedora
     *        extension)
     * @param schematronPreprocessorPath
     *        Location of the Schematron pre-processing stylesheet configured
     *        with Fedora.</i>
     * @param ruleSchemaMap
     *        Location of rule schemas (Schematron), configured with Fedora (see
     *        Fedora.fcfg). Current options are <i>schematron/foxmlRules1-0.xml</i>
     *        for FOXML or <i>schematron/metsExtRules1-0.xml</i> for METS
     * @throws ServerException
     *         If construction fails for any reason.
     */
    public DOValidatorImpl(String tempDir,
                           Map<String, String> xmlSchemaMap,
                           String schematronPreprocessorPath,
                           Map<String, String> ruleSchemaMap)
            throws ServerException {
        logger.debug("VALIDATE: Initializing object validation...");
        m_xmlSchemaMap = new HashMap<String, DOValidatorXMLSchema>(xmlSchemaMap.size());
        SchemaFactory schemaFactory =
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        for (Entry<String,String> entry: xmlSchemaMap.entrySet()) {
            try {
                m_xmlSchemaMap.put(
                    entry.getKey(),
                    new DOValidatorXMLSchema(
                        schemaFactory.newSchema(new File(entry.getValue()))));
            } catch (SAXException e) {
                throw new GeneralException("Cannot read or create schema at " +
                        entry.getValue(),e);
            }
        }
        m_ruleSchemaMap = ruleSchemaMap;
        if (tempDir == null) {
            throw new ObjectValidityException("[DOValidatorImpl] ERROR in constructor: "
                    + "tempDir is null.");
        }
        if (schematronPreprocessorPath == null) {
            throw new ObjectValidityException("[DOValidatorImpl] ERROR in constructor. "
                    + "schematronPreprocessorPath is null.");
        }
        m_tempDir = new File(tempDir);
        if (!m_tempDir.exists() && !m_tempDir.mkdirs()) {
            throw new GeneralException("Cannot read or create tempDir at " +
                tempDir);
        }
        m_absoluteTempPath = m_tempDir.getAbsolutePath();
        DOValidatorImpl.tempDir = tempDir;
        DOValidatorImpl.schematronPreprocessorPath = schematronPreprocessorPath;
    }

    /**
     * <p>
     * Validates a digital object.
     * </p>
     *
     * @param objectAsStream
     *        The digital object provided as a stream.
     * @param format
     *           The format URI of the object serialization.
     * @param validationType
     *        The level of validation to perform on the digital object. This is
     *        an integer from 0-2 with the following meanings: 0 = VALIDATE_ALL
     *        (do all validation levels) 1 = VALIDATE_XML_SCHEMA (perform only
     *        XML Schema validation) 2 = VALIDATE_SCHEMATRON (perform only
     *        Schematron Rules validation)
     * @param phase
     *        The stage in the workflow for which the validation should be
     *        contextualized. "ingest" = the object is encoded for ingest into
     *        the repository "store" = the object is encoded with all final
     *        assignments so that it is appropriate for storage as the
     *        authoritative serialization of the object.
     * @throws ObjectValidityException
     *         If validation fails for any reason.
     * @throws GeneralException
     *         If validation fails for any reason.
     */
    public void validate(InputStream objectAsStream,
                         String format,
                         int validationType,
                         String phase) throws ObjectValidityException,
                         GeneralException {

        if (validationType == VALIDATE_NONE) return;
        checkFormat(format);
        switch (validationType) {
            case VALIDATE_NONE:
                break;
            case VALIDATE_ALL:
                try {
                    // FIXME We need to use the object Inputstream twice, once for XML
                    // Schema validation and once for Schematron validation.
                    // We may want to consider implementing some form of a rewindable
                    // InputStream. For now, I will just write the object InputStream to
                    // disk so I can read it multiple times.
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "Validating streams against schema and schematron" +
                                        " requires caching tempfiles to disk; consider" +
                                        "calling validations seperately with a buffered" +
                                        "InputStream"
                                );
                    }
                    File objectAsFile = streamtoFile(objectAsStream);
                    validate(objectAsFile, format, validationType, phase);
                } catch (IOException ioe) {
                    throw new ObjectValidityException("[DOValidatorImpl]: "
                            + "ERROR in validate(InputStream objectAsStream...). " + ioe.getMessage());
                }
                break;
            case VALIDATE_XML_SCHEMA:
                validateXMLSchema(objectAsStream, m_xmlSchemaMap.get(format));
                break;
            case VALIDATE_SCHEMATRON:
                validateByRules(objectAsStream,
                        m_ruleSchemaMap.get(format),
                        schematronPreprocessorPath,
                        phase);
                break;
            default:
                String msg = "VALIDATE: ERROR - missing or invalid validationType";
                logger.error(msg);
                throw new GeneralException("[DOValidatorImpl] " + msg + ":"
                    + validationType);
        }
        return;
    }

    /**
     * <p>
     * Validates a digital object.
     * </p>
     *
     * @param objectAsFile
     *        The digital object provided as a file.
     * @param validationType
     *        The level of validation to perform on the digital object. This is
     *        an integer from 0-2 with the following meanings: 0 = VALIDATE_ALL
     *        (do all validation levels) 1 = VALIDATE_XML_SCHEMA (perform only
     *        XML Schema validation) 2 = VALIDATE_SCHEMATRON (perform only
     *        Schematron Rules validation)
     * @param phase
     *        The stage in the work flow for which the validation should be
     *        contextualized. "ingest" = the object is in the submission format
     *        for the ingest phase "store" = the object is in the authoritative
     *        format for the final storage phase
     * @throws ObjectValidityException
     *         If validation fails for any reason.
     * @throws GeneralException
     *         If validation fails for any reason.
     */
    public void validate(File objectAsFile,
                         String format,
                         int validationType,
                         String phase) throws ObjectValidityException,
            GeneralException {
        logger.debug("VALIDATE: Initiating validation: phase={} format={}",
                phase, format);
        if (validationType == VALIDATE_NONE) return;
        checkFormat(format);

        if (format.equals(Constants.ATOM_ZIP1_1.uri)) {
            // If the object serialization is a Zip file with an atom
            // manifest, extract the manifest for validation.
            try {
                File manifest = null;
                ZipInputStream zip = new ZipInputStream(new FileInputStream(objectAsFile));
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().equals("atommanifest.xml")) {
                        manifest = streamtoFile(zip);
                        break;
                    }
                }
                zip.close();
                objectAsFile = manifest;
            } catch(IOException e) {
                throw new GeneralException(e.getMessage(), e);
            }
        }
        
        try {

            FileInputStream objectAsStream = new FileInputStream(objectAsFile);
            if (validationType == VALIDATE_ALL) {
                validateByRules(objectAsStream,
                        m_ruleSchemaMap.get(format),
                        schematronPreprocessorPath,
                        phase);
                validateXMLSchema(new FileInputStream(objectAsFile),
                    m_xmlSchemaMap.get(format));
            } else if (validationType == VALIDATE_XML_SCHEMA) {
                validateXMLSchema(objectAsStream, m_xmlSchemaMap.get(format));
            } else if (validationType == VALIDATE_SCHEMATRON) {
                validateByRules(objectAsStream,
                        m_ruleSchemaMap.get(format),
                        schematronPreprocessorPath,
                        phase);
            } else {
                String msg = "VALIDATE: ERROR - missing or invalid validationType";
                logger.error(msg);
                throw new GeneralException("[DOValidatorImpl] " + msg + ":"
                        + validationType);
            }
        } catch (IOException ioe) {
            logger.error("VALIDATE: ERROR - failed validations.", ioe);
            throw new ObjectValidityException("[DOValidatorImpl]: validate(File input...). "
                    + ioe.getMessage());
        } finally {
            cleanUp(objectAsFile);
        }
    }

    private void checkFormat(String format) throws ObjectValidityException {
        if (!m_xmlSchemaMap.containsKey(format)) {
        	Validation validation = new Validation("unknown");
        	String problem = "Unsupported format: ".concat(format);
        	validation.setObjectProblems(
        	        Collections.singletonList(problem));
            throw new ObjectValidityException(problem, validation);
        }
    }

    /**
     * Do XML Schema validation on the Fedora object.
     *
     * @param objectAsFile
     *        The digital object provided as a file.
     * @throws ObjectValidityException
     *         If validation fails for any reason.
     * @throws GeneralException
     *         If validation fails for any reason.
     */
    private void validateXMLSchema(InputStream objectAsStream, DOValidatorXMLSchema xsv)
            throws ObjectValidityException, GeneralException {

        try {
            xsv.validate(objectAsStream);
        } catch (ObjectValidityException e) {
            logger.error("VALIDATE: ERROR - failed XML Schema validation.", e);
            throw e;
        } catch (Exception e) {
            logger.error("VALIDATE: ERROR - failed XML Schema validation.", e);
            throw new ObjectValidityException("[DOValidatorImpl]: validateXMLSchema. "
                    + e.getMessage());
        }
        logger.debug("VALIDATE: SUCCESS - passed XML Schema validation.");
    }

    /**
     * Do Schematron rules validation on the Fedora object. Schematron
     * validation tests the object against a set of rules expressed using XPATH
     * in a Schematron schema. These test for things that are beyond what can be
     * expressed using XML Schema.
     *
     * @param objectAsFile
     *        The digital object provided as a file.
     * @param schemaPath
     *        Location of the Schematron rules file.
     * @param preprocessorPath
     *        Location of Schematron preprocessing stylesheet
     * @param phase
     *        The workflow phase (ingest, store) for the object.
     * @throws ObjectValidityException
     *         If validation fails for any reason.
     * @throws GeneralException
     *         If validation fails for any reason.
     */
    private void validateByRules(InputStream objectAsStream,
                                 String ruleSchemaPath,
                                 String preprocessorPath,
                                 String phase) throws ObjectValidityException,
            GeneralException {

        try {
            DOValidatorSchematron schtron =
                    new DOValidatorSchematron(ruleSchemaPath,
                                              preprocessorPath,
                                              phase);
            schtron.validate(objectAsStream);
        } catch (ObjectValidityException e) {
            logger.error("VALIDATE: ERROR - failed Schematron rules validation.",
                      e);
            throw e;
        } catch (Exception e) {
            logger.error("VALIDATE: ERROR - failed Schematron rules validation.",
                      e);
            throw new ObjectValidityException("[DOValidatorImpl]: "
                    + "failed Schematron rules validation. " + e.getMessage());
        }
        logger.debug("VALIDATE: SUCCESS - passed Schematron rules validation.");
    }

    private File streamtoFile(InputStream objectAsStream)
            throws IOException {

        File objectAsFile = null;
        try {
            objectAsFile = File.createTempFile("validation", "tmp", m_tempDir);

            FileOutputStream fos = new FileOutputStream(objectAsFile);
            FileUtils.copy(objectAsStream, fos);
            return objectAsFile;
        } catch (IOException e) {
            if (objectAsFile != null && objectAsFile.exists()) {
                objectAsFile.delete();
            }
            throw e;
        }
    }

    // Distinguish temporary object files from real object files
    // that were passed in for validation.  This is a bit ugly as it stands,
    // but it should only blow away files in the temp directory.
    private void cleanUp(File f) {
        if (f != null && f.getParentFile() != null) {
            if (m_absoluteTempPath.equalsIgnoreCase(f
                    .getParentFile().getAbsolutePath())) {
                f.delete();
            }
        }
    }
    
}
