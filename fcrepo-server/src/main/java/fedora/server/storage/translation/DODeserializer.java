/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.translation;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.StreamIOException;
import fedora.server.storage.types.DigitalObject;

/**
 * Reads a Fedora object in some format.
 * <p>
 * Implementations of this interface <strong>MUST</strong> implement a public,
 * no-arg constructor.
 * </p>
 * 
 * @author Chris Wilper
 */
public interface DODeserializer {

    /**
     * Creates a new deserializer that the same format as this one.
     */
    public DODeserializer getInstance();

    /**
     * Deserializes the given stream.
     * 
     * @param in
     *        the stream to read from (closed when finished).
     * @param obj
     *        the object to deserialize into.
     * @param encoding
     *        the character encoding if the format is text-based.
     * @param transContext
     *        the translation context.
     * @throws ObjectIntegrityException
     *         if the stream does not properly encode an object.
     * @throws StreamIOException
     *         if there is an error reading from the stream.
     * @throws UnsupportedEncodingException
     *         if the encoding is not supported by the JVM.
     * @see DOTranslationUtility#DESERIALIZE_INSTANCE
     */
    public void deserialize(InputStream in,
                            DigitalObject obj,
                            String encoding,
                            int transContext) throws ObjectIntegrityException,
            StreamIOException, UnsupportedEncodingException;

}