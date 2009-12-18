/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

/**
 * Signals that an object has one or more related objects that depend on it.
 * 
 * <p>For example, a service definition or deployment
 * object can be shared by multiple objects. Any data objects that use a
 * specific service definition or deployment object "depend" on those
 * objects. To remove a dependent object, you must first remove all related
 * objects.
 * 
 * @author Ross Wayland
 */
public class ObjectDependencyException
        extends StorageException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an ObjectDependencyException.
     * 
     * @param message
     *        An informative message explaining what happened and (possibly) how
     *        to fix it.
     */
    public ObjectDependencyException(String message) {
        super(message);
    }

}
