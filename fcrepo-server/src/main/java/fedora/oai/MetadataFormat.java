/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Describes a metadata format.
 * 
 * @author Chris Wilper
 * @see <a
 *      href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ListMetadataFormats">
 *      http://www.openarchives.org/OAI/openarchivesprotocol.html#ListMetadataFormats</a>
 */
public interface MetadataFormat {

    /**
     * Get the prefix of the format.
     */
    public abstract String getPrefix();

    /**
     * Get the URL of the schema.
     */
    public abstract String getSchemaLocation();

    /**
     * Get the URI of the namespace.
     */
    public abstract String getNamespaceURI();

}
