/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

import java.util.Set;

/**
 * Metadata expressed in a single format with a header and optional "about"
 * data which is descriptive of the metadata (such as rights or provenance 
 * statements).
 * 
 * @author Chris Wilper
 * @see <a
 *      href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Record">
 *      http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#Record</a>
 */
public interface Record {

    /**
     * Get the header portion of the record.
     */
    public abstract Header getHeader();

    /**
     * Get the metadata portion of the record. This must be an xml chunk in
     * which the W3C schema is identified by the root element's
     * xsi:schemaLocation attribute. If getHeader().isAvailable() is false, this
     * may be null.
     */
    public abstract String getMetadata();

    /**
     * Get the 'about' portions of the record. There will be zero or more items
     * in the resulting Set. These are descriptors of the metadata. These must
     * be xml chunks in which the W3C schema is identified by the root element's
     * xsi:schemaLocation attribute. If getHeader().isAvailable() is false, this
     * may be null.
     */
    public abstract Set getAbouts();

}
