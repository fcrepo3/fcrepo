/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * A simple implementation of MetadataFormat that provides getters on the 
 * values passed in the constructor.
 * 
 * @author Chris Wilper
 */
public class SimpleMetadataFormat
        implements MetadataFormat {

    private final String m_prefix;

    private final String m_schemaLocation;

    private final String m_namespaceURI;

    public SimpleMetadataFormat(String prefix,
                                String schemaLocation,
                                String namespaceURI) {
        m_prefix = prefix;
        m_schemaLocation = schemaLocation;
        m_namespaceURI = namespaceURI;
    }

    public String getPrefix() {
        return m_prefix;
    }

    public String getSchemaLocation() {
        return m_schemaLocation;
    }

    public String getNamespaceURI() {
        return m_namespaceURI;
    }
}
