/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.types;

import java.io.IOException;
import java.io.InputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.utilities.StringUtility;


/**
 * A Fedora datastream.
 *
 * @author Sandy Payette
 */
public class Datastream {

    private static final Logger logger =
            LoggerFactory.getLogger(Datastream.class);

    public final static String CHECKSUMTYPE_DISABLED = "DISABLED";

    public final static String CHECKSUM_NONE = "none";

    public final static String CHECKSUM_IOEXCEPTION = "ExceptionReadingStream";

    public boolean isNew = false;

    public String DatastreamID;

    public String[] DatastreamAltIDs = new String[0];

    public String DSFormatURI;

    public String DSMIME;

    /**
     * Datastream Control Group: This indicates the nature of the repository's
     * control over the datastream content. Values are:
     * <p>
     * R = Redirected. The datastream resides on an external server and is
     * referenced by a URL. When a dissemination request for the *datastream*
     * comes through, Fedora sends an HTTP redirect to the client, thereby
     * causing the client to directly access the datastream from its external
     * location. This is useful in cases where the datastream is really some
     * sort of streaming media that cannot be piped through Fedora, or the
     * datastream is an HTML document with relative hyperlinks to the server on
     * which is is normally hosted. E = External Referenced. The datastream
     * content is external to the repository and referenced by URL. The content
     * is not under the direct custodianship of the repository. The URL is
     * considered public so the repository does not worry about whether it
     * exposes the datastream location to collaborating services. M = Managed
     * Content. The datastream content is stored and managed by the repository.
     * The content is considered under the direct custodianship of the
     * repository. The repository does not expose the underlying storage
     * location to collaborating services and it mediates all access to the
     * content by collaborating services. X = Inline XML Metadata. The
     * datastream content is user-defined XML metadata that is stored within the
     * digital object XML file itself. As such, it is intrinsically bound to the
     * digital object, and by implication, it is stored and managed by the
     * repository. The content considered under the custodianship of the
     * repository.
     */
    public String DSControlGrp;

    /** Info Type: DATA or one of the METS MDType values */
    /** Used to maintain backwards compatibility with METS-Fedora */
    public String DSInfoType;

    public String DSState;

    public boolean DSVersionable;

    // Version-level attributes:
    public String DSVersionID;

    public String DSLabel;

    public Date DSCreateDT;

    public long DSSize;

    public String DSLocation;

    public String DSLocationType;

    public String DSChecksumType;

    public String DSChecksum;

    public static boolean autoChecksum = false;

    public static String defaultChecksumType = "DISABLED";

    public Datastream() {
    }

    public InputStream getContentStream() throws StreamIOException {
        return null;
    }

    public InputStream getContentStreamForChecksum() throws StreamIOException {
        return getContentStream();
    }

    public static String getDefaultChecksumType() {
        return defaultChecksumType;
    }

    public String getChecksumType() {
        if (DSChecksumType == null || DSChecksumType.equals("")
                || DSChecksumType.equals(CHECKSUM_NONE)) {
            DSChecksumType = getDefaultChecksumType();
            if (DSChecksumType == null) {
                logger.warn("checksumType is null");
            }
        }
        return DSChecksumType;
    }

    public String getChecksum() {
        if (DSChecksum == null || DSChecksum.equals(CHECKSUM_NONE)) {
            DSChecksum = computeChecksum(getChecksumType());
        }
        logger.debug("Checksum = " + DSChecksum);
        return DSChecksum;
    }

    public String setChecksum(String csType) {
        if (csType != null) {
            DSChecksumType = csType;
        }
        logger.debug("setting checksum using type: " + DSChecksumType);
        DSChecksum = computeChecksum(DSChecksumType);
        return DSChecksum;
    }

    public boolean compareChecksum() {
        if (DSChecksumType == null || DSChecksumType.equals("")
                || DSChecksumType.equals(CHECKSUM_NONE)) {
            return false;
        }
        if (DSChecksum == null) {
            return false;
        }
        if (DSChecksumType.equals(CHECKSUMTYPE_DISABLED)) {
            return true;
        }
        String curChecksum = computeChecksum(DSChecksumType);
        if (curChecksum.equals(DSChecksum)) {
            return true;
        }
        return false;
    }

    private String computeChecksum(String csType) {
        logger.debug("checksumType is " + csType);
        String checksum = CHECKSUM_NONE;
        if (csType == null) {
            logger.warn("checksumType is null");
        }
        if (csType.equals(CHECKSUMTYPE_DISABLED)) {
            checksum = CHECKSUM_NONE;
            return checksum;
        }
        InputStream is = null;
        try {
            MessageDigest md = MessageDigest.getInstance(csType);
            logger.debug("Classname = " + this.getClass().getName());
            logger.debug("location = " + DSLocation);
            is = getContentStreamForChecksum();
            if (is != null) {
                byte buffer[] = new byte[5000];
                int numread;
                logger.debug("Reading content...");
                while ((numread = is.read(buffer, 0, 5000)) > 0) {
                    md.update(buffer, 0, numread);
                }
                is.close();
                logger.debug("...Done reading content");
                checksum = StringUtility.byteArraytoHexString(md.digest());
            }
        } catch (NoSuchAlgorithmException e) {
            checksum = CHECKSUM_NONE;
        } catch (StreamIOException e) {
            // TODO Auto-generated catch block
            checksum = CHECKSUM_IOEXCEPTION;
            logger.warn("IOException reading datastream to generate checksum");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            checksum = CHECKSUM_IOEXCEPTION;
            logger.warn("IOException reading datastream to generate checksum");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.warn("IOException closing stream (computeChecksum) in finally");
                }
            }
        }
        return checksum;
    }

    public static String validateChecksumType(String checksumType)
            throws GeneralException {
        String csType = null;
        if (checksumType == null || checksumType.equalsIgnoreCase("DEFAULT")) {
            return Datastream.getDefaultChecksumType();
        }
        if (checksumType.equalsIgnoreCase("DISABLED")) {
            return "DISABLED";
        }
        if (checksumType.equalsIgnoreCase("MD5")) {
            csType = "MD5";
        }
        if (checksumType.equalsIgnoreCase("SHA-1")) {
            csType = "SHA-1";
        }
        if (checksumType.equalsIgnoreCase("SHA-256")) {
            csType = "SHA-256";
        }
        if (checksumType.equalsIgnoreCase("SHA-384")) {
            csType = "SHA-384";
        }
        if (checksumType.equalsIgnoreCase("SHA-512")) {
            csType = "SHA-512";
        }
        if (checksumType.equalsIgnoreCase("HAVAL")) {
            csType = "HAVAL";
        }
        if (checksumType.equalsIgnoreCase("TIGER")) {
            csType = "TIGER";
        }
        if (checksumType.equalsIgnoreCase("WHIRLPOOL")) {
            csType = "WHIRLPOOL";
        }
        if (csType == null) {
            throw new GeneralException("Unknown checksum algorithm specified: "
                    + checksumType);
        }
        try {
            MessageDigest.getInstance(csType);
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralException("Checksum algorithm not yet implemented: "
                    + csType);
        }
        return csType;
    }

    // Get a complete copy of this datastream
    public Datastream copy() {
        Datastream ds = new Datastream();
        copy(ds);
        return ds;
    }

    // Copy this instance into target
    public void copy(Datastream target) {

        target.isNew = isNew;
        target.DatastreamID = DatastreamID;
        if (DatastreamAltIDs != null) {
            target.DatastreamAltIDs = new String[DatastreamAltIDs.length];
            for (int i = 0; i < DatastreamAltIDs.length; i++) {
                target.DatastreamAltIDs[i] = DatastreamAltIDs[i];
            }
        }
        target.DSFormatURI = DSFormatURI;
        target.DSMIME = DSMIME;
        target.DSControlGrp = DSControlGrp;
        target.DSInfoType = DSInfoType;
        target.DSState = DSState;
        target.DSVersionable = DSVersionable;
        target.DSVersionID = DSVersionID;
        target.DSLabel = DSLabel;
        target.DSCreateDT = DSCreateDT;
        target.DSSize = DSSize;
        target.DSLocation = DSLocation;
        target.DSLocationType = DSLocationType;
        target.DSChecksumType = DSChecksumType;
        target.DSChecksum = DSChecksum;
    }

}
