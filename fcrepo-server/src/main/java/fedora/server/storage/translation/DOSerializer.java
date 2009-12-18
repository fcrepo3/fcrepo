/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DigitalObject;

/**
 * Writes a Fedora object to some format.
 * <p>
 * Implementations of this interface <strong>MUST</strong> implement a public,
 * no-arg constructor.
 * </p>
 * 
 * @author Chris Wilper
 */
public interface DOSerializer {

    /**
     * Creates a new serializer that writes the same format as this one.
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