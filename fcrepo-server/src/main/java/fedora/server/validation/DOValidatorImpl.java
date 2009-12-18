/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import fedora.common.Constants;
import fedora.server.errors.GeneralException;
import fedora.server.errors.ObjectValidityException;
import fedora.server.errors.ServerException;
import fedora.utilities.FileUtils;

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

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(DOValidatorImpl.class.getName());

    protected static boolean debug = true;

    public static final int VALIDATE_ALL = 0;

    public static final int VALIDATE_XML_SCHEMA = 1;

    public static final int VALIDATE_SCHEMATRON = 2;

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
    private final Map<String, String> m_xmlSchemaMap;

    /**
     * Map of Schematron rule schemas configured with the Fedora Repository. key =
     * format uri value = schema file path
     */
    private final Map<String, String> m_ruleSchemaMap;

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
        LOG.debug("VALIDATE: Initializing object validation...");
        m_xmlSchemaMap = xmlSchemaMap;
        m_ruleSchemaMap = ruleSchemaMap;
        if (tempDir == null) {
            throw new ObjectValidityException("[DOValidatorImpl] ERROR in constructor: "
                    + "tempDir is null.");
        }
        if (schematronPreprocessorPath == null) {
            throw new ObjectValidityException("[DOValidatorImpl] ERROR in constructor. "
                    + "schematronPreprocessorPath is null.");
        }
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
                         String phase) throws ObjectValidityException {
        checkFormat(format);
        // FIXME We need to use the object Inputstream twice, once for XML
        // Schema validation and once for Schematron validation.
        // We may want to consider implementing some form of a rewindable
        // InputStream. For now, I will just write the object InputStream to
        // disk so I can read it multiple times.
        try {
            File objectAsFile = streamtoFile(tempDir, objectAsStream);
            validate(objectAsFile, format, validationType, phase);
        } catch (ObjectValidityException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectValidityException("[DOValidatorImpl]: "
                    + "ERROR in validate objectAsStream. " + e.getMessage());
        }
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
        LOG.debug("Validation phase=" + phase + " format=" + format);
        LOG.debug("VALIDATE: Initiating validation: " + " phase=" + phase
                + " format=" + format);
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
                        manifest = streamtoFile(tempDir, zip);
                        break;
                    }
                }
                zip.close();
                objectAsFile = manifest;
            } catch(IOException e) {
                throw new GeneralException(e.getMessage(), e);
            }
        }
        
        if (validationType == VALIDATE_ALL) {
            validateByRules(objectAsFile,
                            m_ruleSchemaMap.get(format),
                            schematronPreprocessorPath,
                            phase);
            validateXMLSchema(objectAsFile, m_xmlSchemaMap.get(format));
        } else if (validationType == VALIDATE_XML_SCHEMA) {
            validateXMLSchema(objectAsFile, m_xmlSchemaMap.get(format));
        } else if (validationType == VALIDATE_SCHEMATRON) {
            validateByRules(objectAsFile,
                            m_ruleSchemaMap.get(format),
                            schematronPreprocessorPath,
                            phase);
        } else {
            String msg = "VALIDATE: ERROR - missing or invalid validationType";
            LOG.error(msg);
            cleanUp(objectAsFile);
            throw new GeneralException("[DOValidatorImpl] " + msg + ":"
                    + validationType);
        }
        cleanUp(objectAsFile);
    }

    private void checkFormat(String format) throws ObjectValidityException {
        if (!m_xmlSchemaMap.containsKey(format)) {
            throw new ObjectValidityException("Unsupported format: " + format);
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
    private void validateXMLSchema(File objectAsFile, String xmlSchemaPath)
            throws ObjectValidityException, GeneralException {

        try {
            DOValidatorXMLSchema xsv = new DOValidatorXMLSchema(xmlSchemaPath);
            xsv.validate(objectAsFile);
        } catch (ObjectValidityException e) {
            LOG.error("VALIDATE: ERROR - failed XML Schema validation.", e);
            cleanUp(objectAsFile);
            throw e;
        } catch (Exception e) {
            LOG.error("VALIDATE: ERROR - failed XML Schema validation.", e);
            cleanUp(objectAsFile);
            throw new ObjectValidityException("[DOValidatorImpl]: validateXMLSchema. "
                    + e.getMessage());
        }
        LOG.debug("VALIDATE: SUCCESS - passed XML Schema validation.");
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
    private void validateByRules(File objectAsFile,
                                 String ruleSchemaPath,
                                 String preprocessorPath,
                                 String phase) throws ObjectValidityException,
            GeneralException {

        try {
            DOValidatorSchematron schtron =
                    new DOValidatorSchematron(ruleSchemaPath,
                                              preprocessorPath,
                                              phase);
            schtron.validate(objectAsFile);
        } catch (ObjectValidityException e) {
            LOG.error("VALIDATE: ERROR - failed Schematron rules validation.",
                      e);
            cleanUp(objectAsFile);
            throw e;
        } catch (Exception e) {
            LOG.error("VALIDATE: ERROR - failed Schematron rules validation.",
                      e);
            cleanUp(objectAsFile);
            throw new ObjectValidityException("[DOValidatorImpl]: "
                    + "failed Schematron rules validation. " + e.getMessage());
        }
        LOG.debug("VALIDATE: SUCCESS - passed Schematron rules validation.");
    }

    private File streamtoFile(String dirname, InputStream objectAsStream)
            throws IOException {

        File objectAsFile = null;
        try {
            File tempDir = new File(dirname);
            File fileLocation = null;
            if (tempDir.exists() || tempDir.mkdirs()) {
                fileLocation = File.createTempFile("validation", "tmp", tempDir);

                FileOutputStream fos = new FileOutputStream(fileLocation);
                if (FileUtils.copy(objectAsStream, fos)) {
                    objectAsFile = fileLocation;
                }
            }
        } catch (IOException e) {
            if (objectAsFile.exists()) {
                objectAsFile.delete();
            }
            throw e;
        }
        return objectAsFile;
    }

    // Distinguish temporary object files from real object files 
    // that were passed in for validation.  This is a bit ugly as it stands, 
    // but it should only blow away files in the temp directory.
    private void cleanUp(File f) {
        if (f.getParentFile() != null) {
            if ((new File(tempDir)).getAbsolutePath().equalsIgnoreCase(f
                    .getParentFile().getAbsolutePath())) {
                f.delete();
            }
        }
    }
}
