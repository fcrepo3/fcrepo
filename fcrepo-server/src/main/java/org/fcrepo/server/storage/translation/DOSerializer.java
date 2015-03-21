/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.translation;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.types.DigitalObject;


/**
 * Writes a Fedora object to some format.
 * <p>
 * Implementations of this interface <strong>MUST</strong> implement a public
 * constructor with a DOTranslationUtility argument.
 * </p>
 * 
 * @author Chris Wilper
 */
public interface DOSerializer {

    static final char [] DS_INDENT = "              ".toCharArray();

    /**
     * Get a new serializer with the same format as this one, safe
     * for use in the current thread. Thread-safe implementations may
     * return the same object. 
     */
    public DOSerializer getInstance();

    /**
     * Serializes the given object.
     * 
     * @param obj
     *        the object to serialize.
     * @param out
     *        where to send the output to (auto-closed when finished).
     * @param encoding
     *        the character encoding if the format is text-based.
     * @param transContext
     *        the translation context.
     * @throws ObjectIntegrityException
     *         if the given object is in such a state that serialization can't
     *         be performed.
     * @throws StreamIOException
     *         if there is an error writing to the stream.
     * @throws UnsupportedEncodingException
     *         if the encoding is not supported by the JVM.
     * @see DOTranslationUtility#SERIALIZE_EXPORT_ARCHIVE
     * @see DOTranslationUtility#SERIALIZE_EXPORT_PUBLIC
     * @see DOTranslationUtility#SERIALIZE_EXPORT_MIGRATE
     * @see DOTranslationUtility#SERIALIZE_STORAGE_INTERNAL
     */
    public void serialize(DigitalObject obj,
                          OutputStream out,
                          String encoding,
                          int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException;

}