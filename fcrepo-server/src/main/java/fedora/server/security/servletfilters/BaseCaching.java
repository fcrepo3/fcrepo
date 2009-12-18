/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.security.Principal;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.servlet.FilterConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Niebel
 */
public abstract class BaseCaching
        extends BaseContributing
        implements CacheElementPopulator {

    protected static Log log = LogFactory.getLog(BaseCaching.class);

    //use additional indirection level to distinguish multiple uses of the same code for different filter instances
    private static final Map superCache = new Hashtable();

    protected final Cache getCache(String filterName) {
        String method = "getCache()";
        if (log.isDebugEnabled()) {
            log.debug(enterExit(method));
        }
        return (Cache) superCache.get(filterName);
    }

    private final void putCache(String filterName, Cache cache) {
        String method = "putCache()";
        if (log.isDebugEnabled()) {
            log.debug(enterExit(method));
        }
        superCache.put(filterName, cache);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        String method = "init()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.init(filterConfig);
        inited = false;
        if (!initErrors) {
            Cache cache = getCache(FILTER_NAME);
            if (cache == null) {
                cache = getNewCache();
                putCache(FILTER_NAME, cache);
            }
        }
        if (initErrors) {
            if (log.isErrorEnabled()) {
                log
                        .error(format(method,
                                      "cache not set up correctly; see previous error"));
            }
        }
        inited = true;
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    public void destroy() {
        String method = "destroy()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.destroy();
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    protected Cache getNewCache() {
        String method = "getNewCache()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        Cache cache = new Cache(FILTER_NAME, "", //CACHE_KEY_SEPARATOR
                                LOOKUP_SUCCESS_TIMEOUT_UNIT,
                                LOOKUP_SUCCESS_TIMEOUT_DURATION,
                                AUTHN_FAILURE_TIMEOUT_UNIT,
                                AUTHN_FAILURE_TIMEOUT_DURATION,
                                LOOKUP_EXCEPTION_TIMEOUT_UNIT,
                                LOOKUP_EXCEPTION_TIMEOUT_DURATION,
                                this);
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
        return cache;
    }

    protected boolean SPONSORING = false;

    public static final String LOOKUP_SUCCESS_TIMEOUT_UNIT_KEY =
            "lookup-success-timeout-unit";

    public static final String LOOKUP_SUCCESS_TIMEOUT_DURATION_KEY =
            "lookup-success-timeout-duration";

    public static final String AUTHN_FAILURE_TIMEOUT_UNIT_KEY =
            "authn-failure-timeout-unit";

    public static final String AUTHN_FAILURE_TIMEOUT_DURATION_KEY =
            "authn-failure-timeout-duration";

    public static final String LOOKUP_EXCEPTION_TIMEOUT_UNIT_KEY =
            "lookup-exception-timeout-unit";

    public static final String LOOKUP_EXCEPTION_TIMEOUT_DURATION_KEY =
            "lookup-exception-timeout-duration";

    //defaults
    private final String LOOKUP_SUCCESS_TIMEOUT_UNIT_DEFAULT = "MINUTE";

    private final int LOOKUP_SUCCESS_TIMEOUT_DURATION_DEFAULT = 10;

    private final String AUTHN_FAILURE_TIMEOUT_UNIT_DEFAULT = "SECOND";

    private final int AUTHN_FAILURE_TIMEOUT_DURATION_DEFAULT = 1;

    private final String LOOKUP_EXCEPTION_TIMEOUT_UNIT_DEFAULT = "SECOND";

    private final int LOOKUP_EXCEPTION_TIMEOUT_DURATION_DEFAULT = 1;

    private String LOOKUP_SUCCESS_TIMEOUT_UNIT =
            LOOKUP_SUCCESS_TIMEOUT_UNIT_DEFAULT;

    private int LOOKUP_SUCCESS_TIMEOUT_DURATION =
            LOOKUP_SUCCESS_TIMEOUT_DURATION_DEFAULT;

    private String AUTHN_FAILURE_TIMEOUT_UNIT =
            AUTHN_FAILURE_TIMEOUT_UNIT_DEFAULT;

    private int AUTHN_FAILURE_TIMEOUT_DURATION =
            AUTHN_FAILURE_TIMEOUT_DURATION_DEFAULT;

    private String LOOKUP_EXCEPTION_TIMEOUT_UNIT =
            LOOKUP_EXCEPTION_TIMEOUT_UNIT_DEFAULT;

    private int LOOKUP_EXCEPTION_TIMEOUT_DURATION =
            LOOKUP_EXCEPTION_TIMEOUT_DURATION_DEFAULT;

    public String AUTHENTICATE_KEY = "authenticate";

    public String AUTHENTICATED_USER_KEY = "associated-filters";

    public String SPONSORED_USER_KEY = "surrogate-associated-filters";

    @Override
    protected void initThisSubclass(String key, String value) {
        String method = "initThisSubclass()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        boolean setLocally = false;
        if (LOOKUP_SUCCESS_TIMEOUT_UNIT_KEY.equals(key)) {
            LOOKUP_SUCCESS_TIMEOUT_UNIT = value;
            setLocally = true;
        } else if (LOOKUP_SUCCESS_TIMEOUT_DURATION_KEY.equals(key)) {
            LOOKUP_SUCCESS_TIMEOUT_DURATION = Integer.parseInt(value);
            setLocally = true;
        } else if (AUTHN_FAILURE_TIMEOUT_UNIT_KEY.equals(key)) {
            AUTHN_FAILURE_TIMEOUT_UNIT = value;
            setLocally = true;
        } else if (AUTHN_FAILURE_TIMEOUT_DURATION_KEY.equals(key)) {
            AUTHN_FAILURE_TIMEOUT_DURATION = Integer.parseInt(value);
            setLocally = true;
        } else if (LOOKUP_EXCEPTION_TIMEOUT_UNIT_KEY.equals(key)) {
            LOOKUP_EXCEPTION_TIMEOUT_UNIT = value;
            setLocally = true;
        } else if (LOOKUP_EXCEPTION_TIMEOUT_DURATION_KEY.equals(key)) {
            LOOKUP_EXCEPTION_TIMEOUT_DURATION = Integer.parseInt(value);
            setLocally = true;
        } else if (AUTHENTICATE_KEY.equals(key)) {
            try {
                AUTHENTICATE = booleanValue(value);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(format(method,
                                     "known parameter, bad value",
                                     key,
                                     value));
                }
                initErrors = true;
            }
            setLocally = true;
        } else if (AUTHENTICATED_USER_KEY.equals(key)) {
            String[] temp = value.split(",");
            FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES =
                    new Vector(temp.length);
            for (String element : temp) {
                FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES.add(element);
            }
            setLocally = true;
        } else if (SPONSORED_USER_KEY.equals(key)) {
            log.error(format(method,
                             null,
                             "\"SPONSORED_USER_KEY\"",
                             SPONSORED_USER_KEY));
            log
                    .error(format(method,
                                  null,
                                  "other filters associated with this filter for surrogates",
                                  value));
            String[] temp = value.split(",");
            FILTERS_CONTRIBUTING_SPONSORED_ATTRIBUTES = new Vector(temp.length);
            for (String element : temp) {
                log.error(format(method, null, "adding", element));
                FILTERS_CONTRIBUTING_SPONSORED_ATTRIBUTES.add(element);
            }
            setLocally = true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(format(method, "deferring to super"));
            }
            super.initThisSubclass(key, value);
        }
        if (setLocally) {
            if (log.isInfoEnabled()) {
                log.info(format(method, "known parameter", key, value));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    protected boolean authenticate(boolean alreadyAuthenticated) {
        boolean authenticate = AUTHENTICATE && !alreadyAuthenticated;
        return authenticate;
    }

    @Override
    public void authenticate(ExtendedHttpServletRequest extendedHttpServletRequest)
            throws Exception {
        String method = "authenticate()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        try {
            String userid = extendedHttpServletRequest.getUser();
            if (log.isDebugEnabled()) {
                log.debug(format(method, null, "userid", userid));
            }
            boolean authenticated = false;
            if (userid != null && !"".equals(userid)) {
                String password = extendedHttpServletRequest.getPassword();
                if (log.isDebugEnabled()) {
                    log.debug(format(method, null, "password", password));
                }
                Cache cache = getCache(FILTER_NAME);
                if (log.isDebugEnabled()) {
                    log.debug(format(method, "calling cache.authenticate()"));
                }
                Boolean result = cache.authenticate(this, userid, password);
                authenticated = result != null && result.booleanValue();
                if (authenticated) {
                    Principal authenticatingPrincipal =
                            new fedora.server.security.servletfilters.Principal(userid);
                    extendedHttpServletRequest
                            .setAuthenticated(authenticatingPrincipal,
                                              FILTER_NAME);
                    if (log.isDebugEnabled()) {
                        log.debug(format(method, "set authenticated"));
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(format(method, "calling audit", "user", userid));
                }
                cache.audit(userid);
            }
        } catch (Throwable th) {
            showThrowable(th, log, "general " + method + " failure");
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    public void contributeAttributes(ExtendedHttpServletRequest extendedHttpServletRequest,
                                     String userid,
                                     String password) throws Exception {
        String method = "gatherAttributes()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        if (!extendedHttpServletRequest.isAuthenticated()) {
            throw new Exception();
        }
        try {
            Cache cache = getCache(FILTER_NAME);
            /*
             * if (log.isDebugEnabled()) log.debug(format(method, "calling
             * cache.getPredicates()")); Set predicates =
             * cache.getPredicates(this, userid, password);
             */
            if (log.isDebugEnabled()) {
                log.debug(format(method, "calling cache.getNamedValues()"));
            }
            Map namedValues = cache.getNamedValues(this, userid, password);
            //extendedHttpServletRequest.addRoles(FILTER_NAME, predicates);
            extendedHttpServletRequest.addAttributes(FILTER_NAME, namedValues);
            if (log.isDebugEnabled()) {
                log.debug(format(method, "gatherAttributes calling audit"));
            }
            cache.audit(userid);
            if (log.isDebugEnabled()) {
                log.debug(format(method, "at end of gatherAttributes"));
            }
        } catch (Throwable th) {
            showThrowable(th, log, "general " + method + " failure");
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    public void contributeAuthenticatedAttributes(ExtendedHttpServletRequest extendedHttpServletRequest)
            throws Exception {
        String method = "gatherAuthenticatedAttributes()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        if (extendedHttpServletRequest.getUserPrincipal() != null) {
            String userid = extendedHttpServletRequest.getUser();
            String password = extendedHttpServletRequest.getPassword();
            contributeAttributes(extendedHttpServletRequest, userid, password);
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    public void contributeSponsoredAttributes(ExtendedHttpServletRequest extendedHttpServletRequest)
            throws Exception {
        String method = "gatherSponsoredAttributes()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        String sponsoredUser = extendedHttpServletRequest.getFromHeader();
        if (sponsoredUser != null && !"".equals(sponsoredUser)) {
            String password = "";
            contributeAttributes(extendedHttpServletRequest,
                                 sponsoredUser,
                                 password);
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    public void populateCacheElement(CacheElement cacheElement, String password) {
        String method = "populateCacheElement()";
        if (log.isWarnEnabled()) {
            log.warn(format(method,
                            "must implement this method in filter subclass"));
        }
    }

}
