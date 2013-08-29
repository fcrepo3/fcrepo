/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.fcrepo.common.Constants;
import org.fcrepo.common.xml.format.XMLFormat;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.translation.handlers.FOXMLContentHandler;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;




/**
 * Deserializes objects in the constructor-provided version of FOXML.
 *
 * @author Sandy Payette
 * @author Chris Wilper
 */
public class FOXMLDODeserializer
        implements DODeserializer, Constants {

    /**
     * The format this deserializer will read if unspecified at construction.
     * This defaults to the latest FOXML format.
     */
    public static final XMLFormat DEFAULT_FORMAT = FOXML1_1;

    private static final Logger logger =
            LoggerFactory.getLogger(FOXMLDODeserializer.class);

    /** The format this deserializer reads. */
    private final XMLFormat m_format;

    /**
     * Creates a deserializer that reads the default FOXML format.
     */
    public FOXMLDODeserializer() {
        this(DEFAULT_FORMAT);
    }

    /**
     * Creates a deserializer that reads the given FOXML format.
     *
     * @param format
     *        the version-specific FOXML format.
     * @throws IllegalArgumentException
     *         if format is not a known FOXML format.
     */
    public FOXMLDODeserializer(XMLFormat format) {
        if (format.equals(FOXML1_0) || format.equals(FOXML1_1)) {
            m_format = format;
        } else {
            throw new IllegalArgumentException("Not a FOXML format: "
                    + format.uri);
        }

    }

    //---
    // DODeserializer implementation
    //---

    /**
     * {@inheritDoc}
     */
    public DODeserializer getInstance() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void deserialize(InputStream in,
                            DigitalObject obj,
                            String encoding,
                            int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException {
        logger.debug("Deserializing {} for transContext: {}",
                m_format.uri, transContext);


        // make resettable and pool these handlers?
        FOXMLContentHandler handler =
                new FOXMLContentHandler(obj, m_format, encoding, transContext);
        try {
            XmlTransformUtility.parseWithoutValidating(in, handler);
        } catch (IOException ioe) {
            throw new StreamIOException("low-level stream io problem occurred "
                    + "while sax was parsing this object.");
        } catch (SAXException se) {
            throw new ObjectIntegrityException("FOXML IO stream was bad : "
                    + se.getMessage(), se);
        }
        logger.debug("Just finished parse.");

        if (!handler.rootElementFound()) {
            throw new ObjectIntegrityException("FOXMLDODeserializer: Input stream is not valid FOXML."
                    + " The digitalObject root element was not detected.");
        }

        DOTranslationUtility.normalizeDatastreams(obj, transContext, encoding);
    }

}
