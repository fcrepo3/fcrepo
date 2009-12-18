/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Niebel
 */
public class Cache {

    private static final Log LOG = LogFactory.getLog(Cache.class);

    static boolean firstCall = true;

    private final String cacheId;

    private final String cacheAbbrev;

    private final String CACHE_KEY_SEPARATOR;

    private final String AUTH_SUCCESS_TIMEOUT_UNIT;

    private final int AUTH_SUCCESS_TIMEOUT_DURATION;

    private final String AUTH_FAILURE_TIMEOUT_UNIT;

    private final int AUTH_FAILURE_TIMEOUT_DURATION;

    private final String AUTH_EXCEPTION_TIMEOUT_UNIT;

    private final int AUTH_EXCEPTION_TIMEOUT_DURATION;

    private final CacheElementPopulator cacheElementPopulator;

    public final String getCacheId() {
        return cacheId;
    }

    public final String getCacheAbbrev() {
        return cacheAbbrev;
    }

    public final String getCacheKeySeparator() {
        return CACHE_KEY_SEPARATOR;
    }

    public final String getAuthSuccessTimeoutUnit() {
        return AUTH_SUCCESS_TIMEOUT_UNIT;
    }

    public final int getAuthSuccessTimeoutDuration() {
        return AUTH_SUCCESS_TIMEOUT_DURATION;
    }

    public final String getAuthFailureTimeoutUnit() {
        return AUTH_FAILURE_TIMEOUT_UNIT;
    }

    public final int getAuthFailureTimeoutDuration() {
        return AUTH_FAILURE_TIMEOUT_DURATION;
    }

    public final String getAuthExceptionTimeoutUnit() {
        return AUTH_EXCEPTION_TIMEOUT_UNIT;
    }

    public final int getAuthExceptionTimeoutDuration() {
        return AUTH_EXCEPTION_TIMEOUT_DURATION;
    }

    public final CacheElementPopulator getCacheElementPopulator() {
        return cacheElementPopulator;
    }

    public Cache(String cacheId,
                 String CACHE_KEY_SEPARATOR,
                 String AUTH_SUCCESS_TIMEOUT_UNIT,
                 int AUTH_SUCCESS_TIMEOUT_DURATION,
                 String AUTH_FAILURE_TIMEOUT_UNIT,
                 int AUTH_FAILURE_TIMEOUT_DURATION,
                 String AUTH_EXCEPTION_TIMEOUT_UNIT,
                 int AUTH_EXCEPTION_TIMEOUT_DURATION,
                 CacheElementPopulator cacheElementPopulator) {
        this.cacheId = cacheId;
        this.CACHE_KEY_SEPARATOR = CACHE_KEY_SEPARATOR;
        this.AUTH_SUCCESS_TIMEOUT_UNIT = AUTH_SUCCESS_TIMEOUT_UNIT;
        this.AUTH_SUCCESS_TIMEOUT_DURATION = AUTH_SUCCESS_TIMEOUT_DURATION;
        this.AUTH_FAILURE_TIMEOUT_UNIT = AUTH_FAILURE_TIMEOUT_UNIT;
        this.AUTH_FAILURE_TIMEOUT_DURATION = AUTH_FAILURE_TIMEOUT_DURATION;
        this.AUTH_EXCEPTION_TIMEOUT_UNIT = AUTH_EXCEPTION_TIMEOUT_UNIT;
        this.AUTH_EXCEPTION_TIMEOUT_DURATION = AUTH_EXCEPTION_TIMEOUT_DURATION;
        this.cacheElementPopulator = cacheElementPopulator;
        cacheAbbrev = FilterSetup.getFilterNameAbbrev(getCacheId());
    }

    private final Map cache = new Hashtable();

    public final void audit(String userid) {
        String m = getCacheAbbrev() + " audit() ";
        String key = getKey(userid/* , password, getCacheKeySeparator() */);
        CacheElement cacheElement = getCacheElement(userid);
        if (cacheElement == null) {
            LOG.debug(m + "cache element is null for " + userid);
        } else {
            cacheElement.audit();
        }
    }

    private static final String getKey(String userid /*
                                                         * , String password,
                                                         * String
                                                         * cacheKeySeparator
                                                         */) {
        return userid /* + cacheKeySeparator + password */;
    }

    /*
     * synchronize so that each access gets the same item instance (protect
     * against overlapping calls) note that expiration logic of cache element
     * changes the element's state -- elements are never removed from cache or
     * replaced
     */
    private final synchronized CacheElement getCacheElement(String userid /*
                                                                             * ,
                                                                             * String
                                                                             * password
                                                                             */) {
        String m = getCacheAbbrev() + " getCacheElement() ";
        CacheElement cacheElement = null;
        String keytemp = getKey(userid/* ,password,CACHE_KEY_SEPARATOR */);
        Integer key = new Integer(keytemp.hashCode());
        LOG.debug(m + "keytemp==" + keytemp);
        LOG.debug(m + "key==" + key);
        if (cache.containsKey(key)) {
            LOG.debug(m + "cache already has element");
        } else {
            LOG.debug(m + "cache does not have element; create and put");
            CacheElement itemtemp =
                    new CacheElement(userid, getCacheId(), getCacheAbbrev());
            cache.put(key, itemtemp);
        }
        cacheElement = (CacheElement) cache.get(key);
        if (cacheElement == null) {
            LOG.error(m + "cache does not contain element");
        } else {
            LOG.debug(m + "element retrieved from cache successfully");
        }
        return cacheElement;
    }

    public static final void testAssert() {
        try {
            assert false;
            LOG.debug("asserts are not turned on");
        } catch (Throwable t) {
            LOG.debug("asserts are turned on");
        }
    }

    public final Boolean authenticate(CacheElementPopulator authenticator,
                                      String userid,
                                      String password) throws Throwable {
        if (firstCall) {
            testAssert();
            firstCall = false;
        }
        String m = getCacheAbbrev() + " authenticate() ";

        if (LOG.isDebugEnabled()) {
            LOG.debug(m + "----------------------------------------------");
            LOG.debug(m + "> " + getCacheId() + " [" + userid + "] ["
                    + password + "]");
        } else {
            LOG.info("Authenticating user [" + userid + "]");
        }

        CacheElement cacheElement = getCacheElement(userid /* , password */);
        LOG.debug(m + "cacheElement==" + cacheElement.getInstanceId());

        Boolean authenticated = null;
        try {
            authenticated = cacheElement.authenticate(this, password);
        } catch (Throwable t) {
            LOG.fatal(m + ".authenticate() catch");
            LOG.fatal(m + t.getMessage());
            LOG.fatal(m
                    + (t.getCause() == null ? "" : t.getCause().getMessage()));
            throw t;
        }
        LOG.debug(m + "< " + authenticated);

        return authenticated;
    }

    public final Map getNamedValues(CacheElementPopulator authenticator,
                                    String userid,
                                    String password) throws Throwable {
        if (firstCall) {
            testAssert();
            firstCall = false;
        }
        String m = getCacheAbbrev() + " getNamedValues() ";

        if (LOG.isDebugEnabled()) {
            LOG.debug(m + "----------------------------------------------");
            LOG.debug(m + "> " + getCacheId() + " [" + userid + "] ["
                    + password + "]");
        }

        CacheElement cacheElement = getCacheElement(userid /* , password */);
        LOG.debug(m + "cacheElement==" + cacheElement.getInstanceId());
        Map namedValues = null;
        try {
            namedValues = cacheElement.getNamedValues(this, password);
        } catch (Throwable t) {
            LOG.fatal(m + ".authenticate");
            LOG.fatal(m + t.getMessage());
            LOG.fatal(m
                    + (t.getCause() == null ? "" : t.getCause().getMessage()));

            throw t;
        }
        LOG.debug(m + "< " + namedValues);

        return namedValues;
    }

}
