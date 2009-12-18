/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DOReaderCache
        extends Thread {

    private final int m_maxReaders;

    private final int m_maxCachedSeconds;

    private final Map m_readers;

    private final List m_pidList;

    private boolean m_stopRequested;

    public DOReaderCache(int maxReaders, int maxCachedSeconds) {

        m_maxReaders = maxReaders;
        m_maxCachedSeconds = maxCachedSeconds;

        m_readers = new HashMap();
        m_pidList = new ArrayList();

        m_stopRequested = false;
        start();
    }

    /**
     * Until closed, check for and remove any expired entries every second.
     */
    @Override
    public void run() {
        while (!m_stopRequested) {
            removeExpired();
            if (!m_stopRequested) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
    }

    private void removeExpired() {
        long cutoffTime =
                System.currentTimeMillis() - 1000 * m_maxCachedSeconds;
        synchronized (m_readers) {
            if (m_pidList.size() > 0) {
                boolean done = false;
                List expiredList = new ArrayList();
                Iterator pids = m_pidList.iterator();
                while (pids.hasNext() && !done) {
                    String pid = (String) pids.next();
                    List l = (List) m_readers.get(pid);
                    long cachedTime = ((Long) l.get(1)).longValue();
                    if (cachedTime < cutoffTime) {
                        expiredList.add(pid);
                    } else {
                        done = true;
                    }
                }
                pids = expiredList.iterator();
                while (pids.hasNext()) {
                    remove((String) pids.next());
                }
            }
        }
    }

    /**
     * Remove a DOReader from the cache. If it doesn't exist in the cache, do
     * nothing.
     */
    public void remove(String pid) {
        synchronized (m_readers) {
            if (m_readers.remove(pid) != null) {
                m_pidList.remove(pid);
            }
        }
    }

    /**
     * Add a DOReader to the cache. If it already exists in the cache, refresh
     * the DOReader in the cache.
     */
    public void put(DOReader reader) {
        String pid = null;
        try {
            pid = reader.GetObjectPID();
        } catch (Exception e) {
        }
        Long time = new Long(System.currentTimeMillis());
        List l = new ArrayList();
        l.add(reader);
        l.add(time);
        synchronized (m_readers) {
            m_readers.put(pid, l);
            if (m_pidList.contains(pid)) {
                // ensure it only appears in the list once, at the end
                m_pidList.remove(pid);
            }
            m_pidList.add(pid);
            if (m_readers.size() > m_maxReaders) {
                Object overflowPid = m_pidList.remove(0);
                m_readers.remove(overflowPid);
            }
        }
    }

    /**
     * Get a DOReader from the cache. If it doesn't exist in the cache, return
     * null. If it does exist, set its time to the current time and return it.
     */
    public DOReader get(String pid) {
        DOReader reader = null;
        synchronized (m_readers) {
            List l = (List) m_readers.get(pid);
            if (l != null) {
                reader = (DOReader) l.get(0);
                l.remove(1);
                l.add(new Long(System.currentTimeMillis()));
                // move it to the end of the list so the list stays sorted
                m_pidList.remove(pid);
                m_pidList.add(pid);
            }
        }
        return reader;
    }

    public void close() {
        // make sure the thread finishes
        m_stopRequested = true;
    }

}