/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.FaultException;
import org.fcrepo.common.PID;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.ValidationException;
import org.fcrepo.server.security.PolicyParser;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamManagedContent;



/**
 * Misc validation-related functions.
 *
 * @author Chris Wilper
 * @author Edwin Shin
 * @version $Id$
 */
public abstract class ValidationUtility {

    private static final Logger logger =
            LoggerFactory.getLogger(ValidationUtility.class);

    private static PolicyParser policyParser;
    private static PolicyParser feslPolicyParser;
    // FIXME: this to maintain backward compatibility, validation should be enforced
    private static boolean validateFeslPolicy = false;

    /**
     * Validates the candidate URL. The result of the validation also depends on the
     * control group of the datastream in question. Managed datastreams may be ingested
     * using the file URI scheme, other datastreams may not.
     *
     * @param url
     *            The URL to validate.
     * @param controlGroup
     *            The control group of the datastream the URL belongs to.
     *
     * @throws ValidationException
     *             if the URL is malformed.
     */
    public static void validateURL(String url, String controlGroup)
            throws ValidationException {
        if (!(controlGroup.equalsIgnoreCase("M") || controlGroup.equalsIgnoreCase("E")) && url.startsWith("file:")) {
            throw new ValidationException(
                    "Malformed URL (file: not allowed for control group "
                            + controlGroup + ") " + url);
        }
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            if (url.startsWith(DatastreamManagedContent.UPLOADED_SCHEME)) {
                return;
            }
            throw new ValidationException("Malformed URL: " + url, e);
        }
    }

    /**
     * Sets the policy parser to be used to validate "POLICY" datastream.
     *
     * NOTE: This must be set before attempting to validate POLICY datastreams.
     * Otherwise, a runtime exception will be thrown.
     *
     * @param parser the parser to use.
     */
    public static void setPolicyParser(PolicyParser parser) {
        policyParser = parser;
    }

    public static void setFeslPolicyParser(PolicyParser parser) {
        feslPolicyParser = parser;
    }
    public static void setValidateFeslPolicy(boolean validate) {
        validateFeslPolicy = validate;
    }

    /**
     * Validates the latest version of all reserved datastreams in the given
     * object.
     */
    public static void validateReservedDatastreams(DOReader reader)
            throws ValidationException {
        try {
            for (Datastream ds: reader.GetDatastreams(null, null)) {
                if ("X".equals(ds.DSControlGrp) || "M".equals(ds.DSControlGrp)) {
                    validateReservedDatastream(PID.getInstance(reader.GetObjectPID()),
                                               ds.DatastreamID,
                                               ds);
                }
            }
        } catch (ValidationException e) {
            throw e;
        } catch (ServerException e) {
            throw new FaultException(e);
        }
    }

    /**
     * Validates the given datastream if it's a reserved datastream.
     *
     * The given stream is guaranteed to be closed when this method completes.
     */
    public static void validateReservedDatastream(PID pid,
                                                  String dsId,
                                                  Datastream ds)
            throws ValidationException {

        // NB: only want to generate inputstream from .getContentStream() once
        // we know that this is a datastream to validate
        // to prevent reading of non-reserved datstream content when it is not needed
        InputStream content = null;

        try {
        if ("POLICY".equals(dsId)) {
            content = ds.getContentStream();
            validatePOLICY(content);
        } else if ("FESLPOLICY".equals(dsId)) {
            content = ds.getContentStream();
            validateFESLPOLICY(content);
        } else if ("RELS-EXT".equals(dsId) || "RELS-INT".equals(dsId)) {
                content = ds.getContentStream();
            validateRELS(pid, dsId, content);
            }
        } catch (StreamIOException e) {
            throw new ValidationException("Failed to get content stream for " + pid + "/" + dsId + ": " + e.getMessage(), e);
        }

        if (content != null) {
            try {
                content.close();
            } catch (IOException e) {
                throw new ValidationException("Error closing content stream for " + pid + "/" + dsId + ": " + e.getMessage(), e);
            }
        }
    }

    private static void validatePOLICY(InputStream content)
            throws ValidationException {
        logger.debug("Validating POLICY datastream");
        policyParser.copy().parse(content, true);
        logger.debug("POLICY datastream is valid");
    }

    private static void validateFESLPOLICY(InputStream content)
    throws ValidationException {
        // if FeSL is not enabled, this won't be set
        if (feslPolicyParser != null) {
            logger.debug("Validating FESLPOLICY datastream");
            // FIXME: maintaining backwards compatibility; policy validation should really be enforced
            feslPolicyParser.copy().parse(content, validateFeslPolicy);
            logger.debug("FESLPOLICY datastream is valid");
        }
    }


    /**
     * validate relationships datastream
     * @param pid
     * @param dsId
     * @param content
     * @throws ValidationException
     */
    private static void validateRELS(PID pid, String dsId, InputStream content)
            throws ValidationException {
        logger.debug("Validating " + dsId + " datastream");
        new RelsValidator().validate(pid, dsId, content);
        logger.debug(dsId + " datastream is valid");
    }

}
