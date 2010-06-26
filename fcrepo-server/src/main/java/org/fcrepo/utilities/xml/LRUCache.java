package org.fcrepo.utilities.xml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Package private helper for caching compiled XSLTs and XPath expressions.
 */
class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private int sizeLimit;

    /**
     * Create a new cache with its size restricted to {@code sizeLimit}.
     *
     * @param sizeLimit maximal number of items allowed in the cache
     */
    public LRUCache(int sizeLimit) {
        // Limit the map to a size of 5 without ever growing it and set
        // the sorting to be "access order"
        super(sizeLimit, 1F, true);
        this.sizeLimit = sizeLimit;
    }

    @Override
    public boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() >= sizeLimit;
    }

}
