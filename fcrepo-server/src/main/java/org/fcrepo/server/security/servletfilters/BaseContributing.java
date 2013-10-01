/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.security.servletfilters;

import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
@SuppressWarnings("deprecation")
public abstract class BaseContributing
        extends FilterSetup {

    private static final Logger logger =
            LoggerFactory.getLogger(BaseContributing.class);

    public static final Set<String> NULL_SET =
        Collections.emptySet();

    public static final Hashtable<?,?> EMPTY_MAP = new Hashtable<Object, Object>();

    public static final String[] EMPTY_ARRAY = new String[] {};

    //defaults
    private static boolean AUTHENTICATE_DEFAULT = true;

    private static Collection<String> FILTERS_CONTRIBUTING_SPONSORED_ATTRIBUTES_DEFAULT =
            NULL_SET;

    protected boolean AUTHENTICATE = AUTHENTICATE_DEFAULT;

    protected Collection<String> FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES =
            NULL_SET;

    protected Collection<String> FILTERS_CONTRIBUTING_SPONSORED_ATTRIBUTES =
            FILTERS_CONTRIBUTING_SPONSORED_ATTRIBUTES_DEFAULT;

    public static final String SURROGATE_ROLE_KEY = "surrogate-role";

    private static String SURROGATE_ROLE_DEFAULT = null;

    protected String SURROGATE_ROLE = SURROGATE_ROLE_DEFAULT;

    public static final String SURROGATE_ATTRIBUTE_KEY = "surrogate-attribute";

    private static String SURROGATE_ATTRIBUTE_DEFAULT = null;

    protected String SURROGATE_ATTRIBUTE = SURROGATE_ATTRIBUTE_DEFAULT;

    public static final String USE_FILTER = "use-filter";

    public static final String SKIP_FILTER = "skip-filter";

    public static final String UNAUTHENTICATE_USER_UNCONDITIONALLY =
            "unauthenticate-user-unconditionally";

    public static final String UNAUTHENTICATE_USER_CONDITIONALLY =
            "unauthenticate-user-conditionally";

    public static final String PW_NULL_KEY = "null-password";

    private static String PW_NULL_DEFAULT = USE_FILTER;

    public String PW_NULL = PW_NULL_DEFAULT;

    public static final String PW_0_KEY = "zerolength-password";

    private static String PW_0_DEFAULT = USE_FILTER;

    public String PW_0 = PW_0_DEFAULT;

    public static final String EMPTY_RESULTS_KEY = "empty-results";

    private static String EMPTY_RESULTS_DEFAULT =
            UNAUTHENTICATE_USER_CONDITIONALLY;

    public String EMPTY_RESULTS = EMPTY_RESULTS_DEFAULT;

    public static final String LOG_STACK_TRACES_KEY = "log-stack-traces";

    private static boolean LOG_STACK_TRACES_DEFAULT = false;

    protected boolean LOG_STACK_TRACES = LOG_STACK_TRACES_DEFAULT;

    @Override
    public void init(FilterConfig filterConfig) {
        String method = "init() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.init(filterConfig);
        inited = false;
        if (!initErrors) {
            if (FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES.isEmpty()) {
                if (FILTER_NAME == null || FILTER_NAME.isEmpty()) {
                    initErrors = true;
                    if (logger.isErrorEnabled()) {
                        logger.error(format(method, "FILTER_NAME not set"));
                    }
                } else {
                    FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES =
                            Collections.singletonList(FILTER_NAME);
                }
            }
        }
        if (initErrors) {
            if (logger.isErrorEnabled()) {
                logger.error(format(method,
                                      "FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES not set; see previous error"));
            }
        }
        inited = true;
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    @Override
    public void destroy() {
        String method = "destroy()";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.destroy();
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    @Override
    protected void initThisSubclass(String key, String value) {
        String m = "initThisSubclass() ";
        logger.debug("{}>", m);
        if (SURROGATE_ROLE_KEY.equals(key)) {
            SURROGATE_ROLE = value;
            logger.info("{}{}=={}", m, key, SURROGATE_ROLE);
        } else if (SURROGATE_ATTRIBUTE_KEY.equals(key)) {
            SURROGATE_ATTRIBUTE = value;
            logger.info("{}{}=={}", m, key, SURROGATE_ATTRIBUTE);
        } else if (LOG_STACK_TRACES_KEY.equals(key)) {
            try {
                LOG_STACK_TRACES = Base.booleanValue(value);
                logger.info("{}{}=={}", m, key, LOG_STACK_TRACES);
            } catch (Throwable t) {
                initErrors = true;
                logger.error(m + "bad config " + key + "==" + value);
            }
        } else if (PW_NULL_KEY.equals(key)) {
            if (SKIP_FILTER.equalsIgnoreCase(value)
                    || USE_FILTER.equalsIgnoreCase(value)
                    || UNAUTHENTICATE_USER_UNCONDITIONALLY
                            .equalsIgnoreCase(value)) {
                PW_NULL = value;
                logger.info("{}{}=={}", m, key, PW_NULL);
            } else {
                initErrors = true;
                logger.error(m + "bad config " + key + "==" + value);
            }
        } else if (PW_0_KEY.equals(key)) {
            if (SKIP_FILTER.equalsIgnoreCase(value)
                    || USE_FILTER.equalsIgnoreCase(value)
                    || UNAUTHENTICATE_USER_UNCONDITIONALLY
                            .equalsIgnoreCase(value)) {
                PW_0 = value;
                logger.info("{}{}=={}", m, key, PW_0);
            } else {
                initErrors = true;
                logger.error(m + "bad config " + key + "==" + value);
            }
        } else if (EMPTY_RESULTS_KEY.equals(key)) {
            if (SKIP_FILTER.equalsIgnoreCase(value)
                    || USE_FILTER.equalsIgnoreCase(value)
                    || UNAUTHENTICATE_USER_UNCONDITIONALLY
                            .equalsIgnoreCase(value)
                    || UNAUTHENTICATE_USER_CONDITIONALLY
                            .equalsIgnoreCase(value)) {
                EMPTY_RESULTS = value;
                logger.info("{}{}=={}", m, key, EMPTY_RESULTS);
            } else {
                initErrors = true;
                logger.error(m + "bad config " + key + "==" + value);
            }
        } else {
            logger.debug("{}deferring {} to super", m, key);
            super.initThisSubclass(key, value);
        }
        logger.debug("{}<", m);
    }

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest extendedHttpServletRequest,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        boolean debug = logger.isDebugEnabled();
        if (debug) {
            logger.debug(enter(method));
        }
        super.doThisSubclass(extendedHttpServletRequest, response);
        boolean alreadyAuthenticated =
                extendedHttpServletRequest.getUserPrincipal() != null;

        if (debug) {
            logger.debug(format(method, null, "alreadyAuthenticated")
                    + alreadyAuthenticated);
            logger.debug(format(method, null, "AUTHENTICATE") + AUTHENTICATE);
        }

        if (authenticate(alreadyAuthenticated)) {
            if (logger.isDebugEnabled()) {
                logger.debug(format(method, "calling authenticate() . . ."));
            }
            authenticate(extendedHttpServletRequest); //"authenticate" is really a conditional cache refresh . . . refactor name?
        } else {
            if (debug) {
                logger.debug(format(method, "not calling authenticate()"));
            }
        }

        String authority = extendedHttpServletRequest.getAuthority();
        if (debug) {
            logger.debug(format(method, null, "authority", authority));
        }
        if (authority != null && !authority.isEmpty()) {

            if (extendedHttpServletRequest.isUserSponsored()) {
                if (debug) {
                    //so neither get normal user attribute nor check for sponsored user
                  logger.debug(format(method, "user already sponsored"));
                }
            } else {
                if (debug) {
                    logger.debug(format(method, "user not already sponsored"));
                }
                if (!FILTERS_CONTRIBUTING_AUTHENTICATED_ATTRIBUTES
                        .contains(authority)) {
                    if (debug) {
                        logger.debug(format(method, "not calling gatherAuthenticatedAttributes()"));
                    }
                } else {
                    if (debug) {
                        logger.debug(format(method, "calling gatherAuthenticatedAttributes() . . ."));
                    }
                    contributeAuthenticatedAttributes(extendedHttpServletRequest);

                    //these newly-collect attributes could allow surrogate feature, so check:
                    boolean surrogateTurnedOnHere = false;
                    if (SURROGATE_ROLE == null || SURROGATE_ROLE.isEmpty()) {
                        if (debug) {
                            logger.debug(format(method, "no surrogate role configured"));
                        }
                    } else {
                        if (debug) {
                            logger.debug(format(method,
                                         "surrogate role configured",
                                         SURROGATE_ROLE_KEY,
                                         SURROGATE_ROLE));
                        }
                        if (extendedHttpServletRequest
                                .isUserInRole(SURROGATE_ROLE)) {
                            if (debug) {
                                logger.debug(format(method,
                                                  "authenticated user has surrogate role"));
                            }
                            surrogateTurnedOnHere = true;
                        } else {
                            if (debug) {
                                logger.debug(format(method,
                                                  "authenticated user doesn't have surrogate role"));
                            }
                        }
                    }
                    if (SURROGATE_ATTRIBUTE == null
                            || SURROGATE_ATTRIBUTE.isEmpty()) {
                        if (debug) {
                            logger.debug(format(method,
                                         "no surrogate attribute configured"));
                        }
                    } else {
                        if (debug) {
                            logger.debug(format(method,
                                         "surrogate attribute configured",
                                         SURROGATE_ATTRIBUTE_KEY,
                                         SURROGATE_ATTRIBUTE));
                        }
                        if (extendedHttpServletRequest
                                .isAttributeDefined(SURROGATE_ATTRIBUTE)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(format(method,
                                                  "authenticated user has surrogate attribute"));
                            }
                            surrogateTurnedOnHere = true;
                        } else {
                            if (debug) {
                                logger.debug(format(method,
                                                  "authenticated user doesn't have surrogate attribute"));
                            }
                        }
                    }
                    if (surrogateTurnedOnHere) {
                        if (debug) {
                            logger.debug(format(method, "setting user to sponsored"));
                        }
                        extendedHttpServletRequest.setSponsoredUser();
                        if (extendedHttpServletRequest.isUserSponsored()) {
                            if (debug) {
                                logger.debug(format(method,
                                                  "verified that user is sponsored"));
                            }
                        } else {
                            logger.error(format(method,
                                                  "user is not correctly sponsored"));
                        }
                    }
                }
            }

            if (extendedHttpServletRequest.isUserSponsored()) { //either from earlier filter or in this filter's code directly above
                if (!FILTERS_CONTRIBUTING_SPONSORED_ATTRIBUTES
                        .contains(authority)) {
                    if (debug) {
                        logger.debug(format(method,
                                              "not calling gatherSponsoredAttributes()"));
                    }
                } else {
                    if (debug) {
                        logger.debug(format(method,
                                              "calling gatherSponsoredAttributes() . . ."));
                    }
                    contributeSponsoredAttributes(extendedHttpServletRequest);
                }
            }

        }
        return false; // i.e., don't signal to terminate servlet filter chain
    }

    // NO CACHING AT THIS SUBCLASSING
    abstract protected void authenticate(ExtendedHttpServletRequest extendedHttpServletRequest)
            throws Exception;

    abstract protected void contributeAuthenticatedAttributes(ExtendedHttpServletRequest extendedHttpServletRequest)
            throws Exception;

    abstract protected void contributeSponsoredAttributes(ExtendedHttpServletRequest extendedHttpServletRequest)
            throws Exception;

    abstract protected boolean authenticate(boolean alreadyAuthenticated);

}
