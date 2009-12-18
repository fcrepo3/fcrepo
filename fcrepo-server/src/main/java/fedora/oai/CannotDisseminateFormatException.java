/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Signals that the metadata format identified by the value given for the 
 * metadataPrefix argument is not supported by the item or by the repository.
 * 
 * This may occur while fulfilling a GetRecord, ListIdentifiers, or ListRecords
 * request.
 * 
 * @author Chris Wilper
 */
public class CannotDisseminateFormatException
        extends OAIException {

    private static final long serialVersionUID = 1L;

    public CannotDisseminateFormatException() {
        super("cannotDisseminateFormat", null);
    }

    public CannotDisseminateFormatException(String message) {
        super("cannotDisseminateFormat", message);
    }

}
