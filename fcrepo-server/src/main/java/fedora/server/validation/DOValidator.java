/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.io.File;
import java.io.InputStream;

import fedora.server.errors.ServerException;

/**
 * Validates a digital object.
 * 
 * @author Sandy Payette
 */
public interface DOValidator {

    /**
     * Validates a digital object.
     * 
     * @param in
     *        The digital object provided as a bytestream.
     * @param validationLevel
     *        The level of validation to perform on the digital object. This is
     *        an integer from 0-2 with the following meanings: 0 = VALIDATE_ALL
     *        (do all validation levels) 1 = VALIDATE_XML_SCHEMA (perform only
     *        XML Schema validation) 2 = VALIDATE_SCHEMATRON (perform only
     *        Schematron Rules validation)
     * @param phase
     *        The stage in the work flow for which the validation should be
     *        contextualized. "ingest" = the object is in the submission format
     *        for the ingest stage phase "store" = the object is in the
     *        authoritative format for the final storage phase
     * @throws ServerException
     *         If validation fails for any reason.
     */
    public void validate(InputStream in,
                         String format,
                         int validationLevel,
                         String phase) throws ServerException;

    /**
     * Validates a digital object.
     * 
     * @param in
     *        The digital object provided as a file.
     * @param validationLevel
     *        The level of validation to perform on the digital object. This is
     *        an integer from 0-2 with the following meanings: 0 = VALIDATE_ALL
     *        (do all validation levels) 1 = VALIDATE_XML_SCHEMA (perform only
     *        XML Schema validation) 2 = VALIDATE_SCHEMATRON (perform only
     *        Schematron Rules validation)
     * @param phase
     *        The stage in the work flow for which the validation should be
     *        contextualized. "ingest" = the object is in the submission format
     *        for the ingest stage phase "store" = the object is in the
     *        authoritative format for the final storage phase
     * @throws ServerException
     *         If validation fails for any reason.
     */
    public void validate(File in,
                         String format,
                         int validationLevel,
                         String phase) throws ServerException;

}
