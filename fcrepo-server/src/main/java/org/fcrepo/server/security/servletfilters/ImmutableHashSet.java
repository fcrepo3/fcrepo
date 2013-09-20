/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Bill Niebel
 * @param <T>
 */
public class ImmutableHashSet<T>
        extends HashSet<T> {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean add(T o) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

}
