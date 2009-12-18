/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.utilities;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.Triple;

import org.trippi.TripleIterator;
import org.trippi.TrippiException;

public class FilteredTripleIterator
        extends TripleIterator {

    TripleIterator m_baseIter;

    boolean m_add;

    Triple m_filter;

    Triple m_next;

    boolean m_changeMade = false;

    public FilteredTripleIterator(TripleIterator baseIter,
                                  Triple filter,
                                  boolean add)
            throws TrippiException {
        m_baseIter = baseIter;
        m_filter = filter;
        m_add = add;

        // FIXME restore these later - eddie
        // Map<String, String> map = m_baseIter.getAliasMap();
        // setAliasMap(map);
        m_next = getNext();
    }

    public FilteredTripleIterator(Map<String, String> aliasMap,
                                  Triple filter,
                                  boolean add)
            throws TrippiException {
        m_baseIter = null;
        m_filter = filter;
        m_add = add;
        // FIXME restore this later - eddie
        // setAliasMap(aliasMap);
        m_next = getNext();
    }

    @Override
    public boolean hasNext() throws TrippiException {
        return m_next != null;
    }

    @Override
    public Triple next() throws TrippiException {
        Triple last = m_next;
        m_next = getNext();
        return last;
    }

    private Triple getNext() throws TrippiException {
        Triple next;
        if (!m_add) // purging entries
        {
            next = m_baseIter.next();
            while (next != null && matches(next, m_filter)) {
                m_changeMade = true;
                next = m_baseIter.next();
            }
            return next;
        } else if (m_baseIter == null) { // adding entry to empty set
            m_changeMade = true;
            next = m_filter;
            m_filter = null;
            return next;
        } else { // adding entry to existing set
            next = m_baseIter.next();
            if (matches(next, m_filter)) {
                m_filter = null; // Triple to be added already present
            }
            if (next == null) {
                if (m_filter != null) {
                    m_changeMade = true;
                }
                next = m_filter;
                m_filter = null;
            }
            return next;
        }
    }

    private boolean matches(Triple next, Triple filter) {
        if (filter == null || next == null) {
            return false;
        }
        return partMatches(next.getSubject().toString(), filter.getSubject()
                .toString())
                && partMatches(next.getPredicate().toString(), filter
                        .getPredicate().toString())
                && partMatches(next.getObject().toString(), filter.getObject()
                        .toString());

    }

    private boolean partMatches(String next, String filter) {
        if (next.equals(filter)) {
            return true;
        }
        Map<String, String> map = getAliasMap();
        Set<String> keys = map.keySet();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            if (next.startsWith(key + ":")) {
                next = next.replaceFirst(key + ":", map.get(key));
            }
            if (filter.startsWith(key + ":")) {
                filter = filter.replaceFirst(key + ":", map.get(key));
            }
            if (next.equals(filter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() throws TrippiException {
        m_filter = null;
        if (m_baseIter != null) {
            m_baseIter.close();
        }
    }

    public boolean wasChangeMade() {
        return m_changeMade;
    }
}
