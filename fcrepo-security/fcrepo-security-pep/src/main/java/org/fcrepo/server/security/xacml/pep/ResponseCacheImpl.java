/*
 * File: ResponseCache.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.server.security.xacml.pep;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.util.AttributeComparator;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.SubjectComparator;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class ResponseCacheImpl
        implements ResponseCache {

    private static final Logger logger =
            LoggerFactory.getLogger(ResponseCacheImpl.class);

    private final ContextUtil contextUtil = new ContextUtil();

    private static final int DEFAULT_CACHE_SIZE = 1000;

    private static final long DEFAULT_TTL = 10 * 60 * 1000; // 10 minutes

    private final int CACHE_SIZE;

    private long TTL;

    private Map<String, String> requestCache = null;

    private Map<String, Long> requestCacheTimeTracker = null;

    private List<String> requestCacheUsageTracker = null;

    private MessageDigest digest = null;

    /**
     * The default constructor that initialises the cache with default values.
     *
     * @throws PEPException
     */
    public ResponseCacheImpl()
            throws PEPException {
        this(new Integer(DEFAULT_CACHE_SIZE), new Long(DEFAULT_TTL));
    }

    /**
     * Constructor that initialises the cache with the size and time to live
     * values.
     *
     * @param size
     *        size of the cache
     * @param ttl
     *        maximum time for a cache item to be valid in milliseconds
     * @throws PEPException
     */
    public ResponseCacheImpl(Integer size, Long ttl)
            throws PEPException {
        String noCache = System.getenv("PEP_NOCACHE");
        if (noCache != null && noCache.toLowerCase().startsWith("t")) {
            TTL = 0;
            logger.info("PEP_NOCACHE: TTL on responseCache set to 0");
        } else {
            TTL = ttl.longValue();
        }

        CACHE_SIZE = size.intValue();

        // Note - HashMap, ArrayList are not thread-safe
        requestCache = new HashMap<String, String>(CACHE_SIZE);
        requestCacheTimeTracker = new HashMap<String, Long>(CACHE_SIZE);
        requestCacheUsageTracker = new ArrayList<String>(CACHE_SIZE);

        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new PEPException("Could not initialize the ResponseCache", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ResponseCache#addCacheItem(java.lang.String,
     * java.lang.String)
     */
    public void addCacheItem(String request, String response) {
        String hash = null;

        try {
            hash = makeHash(request);

            // thread-safety on cache operations
            synchronized (requestCache) {

                // if we have a maxxed cache, remove least used item
                if (requestCache.size() >= CACHE_SIZE) {
                    String key = requestCacheUsageTracker.remove(0);
                    requestCache.remove(key);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Purging cache element");
                    }
                }

                requestCache.put(hash, response);
                requestCacheUsageTracker.add(hash);
                requestCacheTimeTracker.put(hash, new Long(System
                                                           .currentTimeMillis()));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Adding Cache Item (" + requestCache.size() + "/"
                             + requestCacheUsageTracker.size() + "/"
                             + requestCacheTimeTracker.size() + "): " + hash);
            }
        } catch (Exception e) {
            logger.warn("Error adding cache item: " + e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ResponseCache#getCacheItem(java.lang.String)
     */
    public String getCacheItem(String request) {
        String hash = null;
        String response = null;

        try {
            hash = makeHash(request);

            // thread-safety on cache operations
            synchronized (requestCache) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Getting Cache Item (" + requestCache.size() + "/"
                                 + requestCacheUsageTracker.size() + "/"
                                 + requestCacheTimeTracker.size() + "): " + hash);
                }

                response = requestCache.get(hash);

                if (response == null) {
                    return null;
                }

                // if this item is older than CACHE_ITEM_TTL then we can't use it
                long usedLast =
                    System.currentTimeMillis()
                    - requestCacheTimeTracker.get(hash).longValue();
                if (usedLast > TTL) {
                    requestCache.remove(hash);
                    requestCacheUsageTracker.remove(hash);
                    requestCacheTimeTracker.remove(hash);

                    if (logger.isDebugEnabled()) {
                        logger.debug("CACHE_ITEM_TTL exceeded: " + hash);
                    }

                    return null;
                }

                // we just used this item, move it to the end of the list (items at
                // beginning get removed...)
                requestCacheUsageTracker.add(requestCacheUsageTracker
                                             .remove(requestCacheUsageTracker.indexOf(hash)));
            }
        } catch (Exception e) {
            logger.warn("Error getting cache item: " + e.getMessage(), e);
            response = null;
        }

        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.fcrepo.server.security.xacml.pep.ResponseCache#invalidate()
     */
    public void invalidate() {
        // thread-safety on cache operations
        synchronized (requestCache) {
            requestCache = new HashMap<String, String>(CACHE_SIZE);
            requestCacheTimeTracker = new HashMap<String, Long>(CACHE_SIZE);
            requestCacheUsageTracker = new ArrayList<String>(CACHE_SIZE);
        }
    }

    /**
     * Given a request, this method generates a hash.
     *
     * @param request
     *        the request to hash
     * @return the hash
     * @throws CacheException
     */
    @SuppressWarnings("unchecked")
    private String makeHash(String request) throws CacheException {
        RequestCtx reqCtx = null;
        try {
            reqCtx = contextUtil.makeRequestCtx(request);
        } catch (MelcoeXacmlException pe) {
            throw new CacheException("Error converting request", pe);
        }
        byte[] hash = null;
        // ensure thread safety, don't want concurrent invocations of this method all modifying digest at once
        // (alternative is to construct a new digest for each(
        synchronized(digest) {
            digest.reset();

            Set<Attribute> attributes = null;

            Set<Subject> subjects = new TreeSet(new SubjectComparator());
            subjects.addAll(reqCtx.getSubjects());
            for (Subject s : subjects) {
                attributes = new TreeSet(new AttributeComparator());
                attributes.addAll(s.getAttributes());
                for (Attribute a : attributes) {
                    hashAttribute(a, digest);
                }
            }

            attributes = new TreeSet(new AttributeComparator());
            attributes.addAll(reqCtx.getResource());
            for (Attribute a : attributes) {
                hashAttribute(a, digest);
            }

            attributes = new TreeSet(new AttributeComparator());
            attributes.addAll(reqCtx.getAction());
            for (Attribute a : attributes) {
                hashAttribute(a, digest);
            }

            attributes = new TreeSet(new AttributeComparator());
            attributes.addAll(reqCtx.getEnvironmentAttributes());
            for (Attribute a : attributes) {
                hashAttribute(a, digest);
            }

            hash = digest.digest();
        }

        return byte2hex(hash);
    }

    /**
     * Utility function to add an attribute to the hash digest.
     *
     * @param a
     *        the attribute to hash
     */
    private static void hashAttribute(Attribute a, MessageDigest dig) {
        dig.update(a.getId().toString().getBytes());
        dig.update(a.getType().toString().getBytes());
        dig.update(a.getValue().encode().getBytes());
        if (a.getIssuer() != null) {
            dig.update(a.getIssuer().getBytes());
        }
        if (a.getIssueInstant() != null) {
            dig.update(a.getIssueInstant().encode().getBytes());
        }
    }

    /**
     * Converts a hash into its hexadecimal string representation.
     *
     * @param bytes
     *        the byte array to convert
     * @return the hexadecimal string representation
     */
    private String byte2hex(byte[] bytes) {
        char[] hexChars =
                {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
                        'c', 'd', 'e', 'f'};

        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(hexChars[b >> 4 & 0xf]);
            sb.append(hexChars[b & 0xf]);
        }

        return new String(sb);
    }
}
