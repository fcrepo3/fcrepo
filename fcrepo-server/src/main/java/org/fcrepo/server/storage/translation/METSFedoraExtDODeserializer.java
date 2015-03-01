/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.xml.sax.SAXException;
import org.fcrepo.common.Constants;
import org.fcrepo.common.xml.format.XMLFormat;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.translation.handlers.METSContentHandler;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Deserializes objects in the constructor-provided version of the METS Fedora
 * Extension format.
 *
 * @author Sandy Payette
 * @author Chris Wilper
 */
public class METSFedoraExtDODeserializer
        implements Constants, DODeserializer {

    /**
     * The format this deserializer will read if unspecified at construction.
     * This defaults to the latest FOXML format.
     */
    public static final XMLFormat DEFAULT_FORMAT = METS_EXT1_1;

    private static final Logger logger =
            LoggerFactory.getLogger(METSFedoraExtDODeserializer.class);

    /** The format this deserializer reads. */
    private final XMLFormat m_format;

    /** The translation utility is use */
    private DOTranslationUtility m_translator;

    /**
     * Creates a deserializer that reads the default Fedora METS Extension
     * format.
     */
    public METSFedoraExtDODeserializer() {
        this(DEFAULT_FORMAT);
    }

    /**
     * Creates a deserializer that reads the given Fedora METS Extension format.
     *
     * @param format
     *        the version-specific Fedora METS Extension format.
     * @throws IllegalArgumentException
     *         if format is not a known Fedora METS Extension format.
     */
    public METSFedoraExtDODeserializer(XMLFormat format) {
        this(format, null);
    }

    /**
     * Creates a deserializer that reads the given Fedora METS Extension format.
     *
     * @param format
     *        the version-specific Fedora METS Extension format.
     * @throws IllegalArgumentException
     *         if format is not a known Fedora METS Extension format.
     */
    public METSFedoraExtDODeserializer(XMLFormat format, DOTranslationUtility translator) {
        if (!format.equals(METS_EXT1_0) && !format.equals(METS_EXT1_1)) {
            throw new IllegalArgumentException("Not a METSFedoraExt format: "
                    + format.uri);
        }
        m_format = format;
        m_translator = (translator == null) ? DOTranslationUtility.defaultInstance() : translator;
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
        logger.debug("Deserializing " + m_format.uri + " for transContext: "
                + transContext);

        try {
            XmlTransformUtility.parseWithoutValidating(in, new METSContentHandler(m_format, m_translator, transContext, encoding, obj));
        } catch (IOException ioe) {
            throw new StreamIOException("Low-level stream IO problem occurred "
                    + "while SAX parsing this object.");
        } catch (SAXException se) {
            throw new ObjectIntegrityException("METS stream was bad : "
                    + se.getMessage());
        }
        try {
            m_translator.normalizeDatastreams(obj,
                                                      transContext,
                                                      encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ObjectIntegrityException(e.getMessage(),e);
        }

    }

}
