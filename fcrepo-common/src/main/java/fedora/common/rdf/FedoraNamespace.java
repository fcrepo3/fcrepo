/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

/**
 * The Fedora RDF namespace.
 * 
 * <pre>
 * Namespace URI    : info:fedora/
 * Preferred Prefix : fedora
 * </pre>
 * 
 * @see <a
 *      href="http://info-uri.info/registry/OAIHandler?verb=GetRecord&metadataPrefix=reg&identifier=info:fedora/">
 *      "info" URI Scheme Registry page</a>
 * @author Chris Wilper
 */
public class FedoraNamespace
        extends RDFNamespace {

    private static final long serialVersionUID = 1L;

    public FedoraNamespace() {

        uri = "info:fedora/";
        prefix = "fedora";
    }

}
