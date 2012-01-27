/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.fcrepo.server.errors.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is a rewrite of the original DOReaderCache using a java.util.concurrent.ConcurrentHashMap

/**
 * DOReader Cache to be used by DOManager to make object retrieval more efficient
 * @author frank asseg
 *
 */
public class DOReaderCache extends TimerTask{
	
	private static final Logger LOG=LoggerFactory.getLogger(DOReaderCache.class);
	
	private int maxSeconds;
	private int maxEntries;
	private final Map<String,CacheEntry> cacheMap = new ConcurrentHashMap<String, DOReaderCache.CacheEntry>(); 
	
	/**
	 * create a new {@link DOReaderCache} instance
	 */
	public DOReaderCache() {
		super();
		LOG.debug(DOReaderCache.class.getName() + " initialized");
	}
	
	/**
	 * set the maximal time in seconds an object should live in the cache
	 * @param maxSeconds the seconds objects will live in the cache before expiring
	 */
	public void setMaxSeconds(int maxSeconds) {
		this.maxSeconds = maxSeconds;
	}
	
	/**
	 * set the max number of entries the cache can hold
	 * @param maxEntries the number of entries
	 */
	public void setMaxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}
	
	/**
	 * add a new entry to the cache
	 * @param reader the {@link DOReader} to be cached
	 */
	public final void put(final DOReader reader){
		try{
			String pid=reader.GetObjectPID();
			LOG.debug("adding " + pid + " to cache");
			synchronized (cacheMap) {
				cacheMap.put(pid, new CacheEntry(System.currentTimeMillis(), reader));
				if (cacheMap.size() > maxEntries){
					removeOldest();
				}
			}
		}catch(ServerException e){
			throw new RuntimeException("Unable to retrieve PID from reader for caching");
		}
	}
	
	private void removeOldest() {
		String oldestEntryPid=null;
		long oldestTimestamp=Long.MAX_VALUE;
		LOG.debug("evicting oldest entry");
		synchronized (cacheMap) {
			for (Map.Entry<String, CacheEntry> e:cacheMap.entrySet()){
				if (e.getValue().timeStamp <= oldestTimestamp){
					oldestTimestamp=e.getValue().timeStamp;
					oldestEntryPid=e.getKey();
				}
			}
			cacheMap.remove(oldestEntryPid);
		}
	}
	
	/**
	 * remove an entry from the cache
	 * @param pid the entry's pid
	 */
	public final void remove(final String pid){
			cacheMap.remove(pid);
	}
	
	/**
	 * get an {@link DOReader} from the cache
	 * @param pid the pid of the {@link DOReader}
	 * @return th correpsondung {@link DOReader} or null if there is no applicable cache content
	 */
	public final DOReader get(final String pid){
		if (cacheMap.containsKey(pid)){
			CacheEntry e=cacheMap.get(pid).copy(System.currentTimeMillis());
			cacheMap.put(pid, e);
			LOG.debug("cache hit for " + pid);
			return e.reader;
		}
		LOG.debug("cache miss for " + pid);
		return null;
	}
	
	/**
	 * {@link TimerTask} implementation to be used a managed Thread by the spring framework
	 */
	@Override
	public void run() {
		this.removeExpired();
	}
	
	/**
	 * remove expired entries from the cache
	 */
	public final void removeExpired(){
		synchronized (cacheMap) {
			for (Iterator<String> it=cacheMap.keySet().iterator();it.hasNext();){
				String pid=it.next();
				CacheEntry e=cacheMap.get(pid);
				long timeStamp=e.timeStamp;
				long age=System.currentTimeMillis() - timeStamp;
				if (age > (maxSeconds * 1000)){
					it.remove();
					LOG.debug("removing entry " + pid + " after " + ((double) age/1000d) + " seconds");
				}
	
			}
		}
	}
	
	private class CacheEntry{
		private final long timeStamp;
		private final DOReader reader;
		
		private CacheEntry(final long timeStamp, final DOReader reader) {
			super();
			this.timeStamp = timeStamp;
			this.reader = reader;
		}
		
		private CacheEntry copy(final long timeStamp){
			return new CacheEntry(timeStamp, this.reader);
		}
	
	}
}