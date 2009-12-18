/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Signals that the value of the resumptionToken argument is invalid or expired.
 * 
 * This may occur while fulfilling a ListIdentifiers, ListRecords, or ListSets
 * request.
 * 
 * @author Chris Wilper
 */
public class BadResumptionTokenException
        extends OAIException {

    private static final long serialVersionUID = 1L;

    public BadResumptionTokenException() {
        super("badResumptionToken", null);
    }

    public BadResumptionTokenException(String message) {
        super("badResumptionToken", message);
    }

}
