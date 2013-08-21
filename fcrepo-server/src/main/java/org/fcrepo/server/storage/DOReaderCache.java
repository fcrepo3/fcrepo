/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.utilities.TimestampedCacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DOReader Cache to be used by DOManager to make object retrieval more
 * efficient
 * 
 * @author Frank Asseg
 * @author Benjamin Armintor
 * 
 */
public class DOReaderCache extends TimerTask {

	private static final Logger LOG = LoggerFactory
			.getLogger(DOReaderCache.class);

	private int maxSeconds;
	// default the max entries to default initial size of the map
	private int maxEntries = 16;
	
	// since we need to synchronize access, we might as well
	// gain the utility of a linked map
	private final Map<String, TimestampedCacheEntry<DOReader>> cacheMap =
	    new FiniteLinkedMap<String, TimestampedCacheEntry<DOReader>>();

	private final ReentrantLock mapLock = new ReentrantLock();
	/**
	 * create a new {@link DOReaderCache} instance
	 */
	public DOReaderCache() {
		super();
		LOG.debug("{} initialized",DOReaderCache.class.getName());
	}

	/**
	 * set the maximal time in seconds an object should live in the cache
	 * 
	 * @param maxSeconds
	 *            the seconds objects will live in the cache before expiring
	 */
	public void setMaxSeconds(int maxSeconds) {
		this.maxSeconds = maxSeconds;
	}

	/**
	 * set the max number of entries the cache can hold
	 * 
	 * @param maxEntries
	 *            the number of entries
	 */
	public void setMaxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	/**
	 * add a new entry to the cache
	 * 
	 * @param reader
	 *            the {@link DOReader} to be cached
	 */
	public final void put(final DOReader reader) {
		try {
			String pid = reader.GetObjectPID();
			LOG.debug("adding {} to cache", pid);
			mapLock.lock();
			cacheMap.put(pid, new TimestampedCacheEntry<DOReader>(System.currentTimeMillis(),
			        reader));
			mapLock.unlock();
		} catch (ServerException e) {
			throw new RuntimeException(
					"Unable to retrieve PID from reader for caching");
		}
	}

	/**
	 * remove an entry from the cache
	 * 
	 * @param pid
	 *            the entry's pid
	 */
	public final void remove(final String pid) {
        mapLock.lock();
		cacheMap.remove(pid);
        mapLock.unlock();
	}

	/**
	 * get an {@link DOReader} from the cache
	 * 
	 * @param pid
	 *            the pid of the {@link DOReader}
	 * @return the corresponding {@link DOReader} or null if there is no
	 *         applicable cache content
	 */
	public final DOReader get(final String pid) {
	    DOReader result = null;
	    mapLock.lock();
		if (cacheMap.containsKey(pid)) {
			TimestampedCacheEntry<DOReader> e = cacheMap.get(pid);
			e.refresh();
			LOG.debug("cache hit for {}", pid);
			result = e.value();
		} else {
		    LOG.debug("cache miss for {}", pid);
		}
		mapLock.unlock();
		return result;
	}

	/**
	 * {@link TimerTask} implementation to be used a managed Thread by the
	 * spring framework
	 */
	@Override
	public void run() {
		this.removeExpired();
	}

	/**
	 * remove expired entries from the cache
	 */
	public final void removeExpired() {
		mapLock.lock();
		Iterator<Entry<String, TimestampedCacheEntry<DOReader>>> entries = cacheMap.entrySet().iterator();
		while (entries.hasNext()) {
		    Entry<String, TimestampedCacheEntry<DOReader>> entry = entries.next();
		    TimestampedCacheEntry<DOReader> e = entry.getValue();
		    long age = e.age();
		    if (age > (maxSeconds * 1000)) {
		        entries.remove();
	            String pid = entry.getKey();
		        LOG.debug("removing entry {} after {} seconds", pid,
		                ((double) age / 1000d));
		    }

		}
		mapLock.unlock();
	}

	@SuppressWarnings("serial")
    private class FiniteLinkedMap<K, V> extends LinkedHashMap<K, V> {
	    @Override
	    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
	        return this.size() > maxEntries;
	    }
	}
}