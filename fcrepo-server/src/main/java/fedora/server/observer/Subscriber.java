/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.observer;

/**
 *
 * @author Edwin Shin
 * @version $Id$
 */
public interface Subscriber {
    public void update(Publisher s, Object o);
}
