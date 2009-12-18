/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.UnsupportedTranslationException;
import fedora.server.storage.types.DigitalObject;

/**
 * Interface for serializing and deserializing Fedora objects to/from various
 * formats.
 * 
 * @author Chris Wilper
 */
public interface DOTranslator {

    /**
     * Deserializes the given stream.
     * 
     * @param in
     *        the stream to read from (closed when finished).
     * @param obj
     *        the object to deserialize into.
     * @param format
     *        the format of the stream (typically a format URI).
     * @param encoding
     *        the character encoding if the format is text-based.
     * @param transContext
     *        the translation context.
     * @throws ObjectIntegrityException
     *         if the stream does not properly encode an object.
     * @throws StreamIOException
     *         if there is an error reading from the stream.
     * @throws ServerException
     *         if the translator is unable to deserialize for any other reason.
     * @throws UnsupportedEncodingException
     *         if the encoding is not supported by the JVM.
     * @see DOTranslationUtility#DESERIALIZE_INSTANCE
     */
    void deserialize(InputStream in,
                     DigitalObject obj,
                     String format,
                     String encoding,
                     int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedTranslationException, ServerException;

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
     * @throws ServerException
     *         if the translator is unable to serialize for any other reason.
     * @throws UnsupportedEncodingException
     *         if the encoding is not supported by the JVM.
     * @see DOTranslationUtility#SERIALIZE_EXPORT_ARCHIVE
     * @see DOTranslationUtility#SERIALIZE_EXPORT_PUBLIC
     * @see DOTranslationUtility#SERIALIZE_EXPORT_MIGRATE
     * @see DOTranslationUtility#SERIALIZE_STORAGE_INTERNAL
     */
    void serialize(DigitalObject obj,
                   OutputStream out,
                   String format,
                   String encoding,
                   int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedTranslationException, ServerException;

}