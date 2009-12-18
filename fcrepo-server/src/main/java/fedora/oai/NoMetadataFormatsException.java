/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Signals that there are no metadata formats available for the specified item.
 * 
 * This may occur while fulfilling a ListMetadataFormats request.
 * 
 * @author Chris Wilper
 */
public class NoMetadataFormatsException
        extends OAIException {

    private static final long serialVersionUID = 1L;

    public NoMetadataFormatsException() {
        super("noMetadataFormats", null);
    }

    public NoMetadataFormatsException(String message) {
        super("noMetadataFormats", message);
    }

}
