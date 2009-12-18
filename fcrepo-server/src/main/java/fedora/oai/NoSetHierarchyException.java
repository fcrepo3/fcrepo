/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.oai;

/**
 * Signals that the repository does not support sets.
 * 
 * This may occur while fulfilling a ListSets, ListIdentifiers, or ListRecords
 * request.
 * 
 * @author Chris Wilper
 */
public class NoSetHierarchyException
        extends OAIException {

    private static final long serialVersionUID = 1L;

    public NoSetHierarchyException() {
        super("noSetHierarchy", null);
    }

    public NoSetHierarchyException(String message) {
        super("noSetHierarchy", message);
    }

}
