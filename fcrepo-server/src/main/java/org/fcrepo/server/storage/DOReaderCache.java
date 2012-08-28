/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage;

import org.fcrepo.server.errors.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

// This class is a rewrite of the original DOReaderCache using a java.util.concurrent.ConcurrentHashMap

/**
 * Cache implementation for DOReaders using Guava's CacheBuilder.
 * 
 * @author Edwin Shin
 * 
 */
public class DOReaderCache {

    private static final Logger LOG = LoggerFactory
            .getLogger(DOReaderCache.class);

    private final Cache<String, DOReader> cacheMap;

    /**
     * create a new {@link DOReaderCache} instance
     */
    public DOReaderCache(CacheBuilder<String, DOReader> builder) {
        cacheMap = builder.build();
        LOG.debug("{} initialized", DOReaderCache.class.getName());
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
            cacheMap.put(pid, reader);
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
        cacheMap.invalidate(pid);
    }

    /**
     * get an {@link DOReader} from the cache
     * 
     * @param pid
     *            the pid of the {@link DOReader}
     * @return th correpsondung {@link DOReader} or null if there is no
     *         applicable cache content
     */
    public final DOReader get(final String pid) {
        return cacheMap.getIfPresent(pid);
    }
}