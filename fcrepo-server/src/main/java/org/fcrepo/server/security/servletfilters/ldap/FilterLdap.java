/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters.ldap;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import javax.servlet.FilterConfig;

import org.fcrepo.server.errors.authorization.PasswordComparisonException;
import org.fcrepo.server.security.servletfilters.BaseCaching;
import org.fcrepo.server.security.servletfilters.CacheElement;
import org.fcrepo.server.security.servletfilters.FilterSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Bill Niebel
 */
public class FilterLdap
        extends BaseCaching {

    public static final String CONTEXT_VERSION_KEY = "java.naming.ldap.version";

    public static final String VERSION_KEY = "version";

    public static final String BIND_FILTER_KEY = "bind-filter";

    public static final String URL_KEY = "url";

    public static final String BASE_KEY = "search-base";

    public static final String FILTER_KEY = "search-filter";

    public static final String USERID_KEY = "id-attribute";

    public static final String PASSWORD_KEY = "password-attribute";

    public static final String ATTRIBUTES2RETURN_KEY = "attributes";

    public static final String GROUPS_NAME_KEY = "attributes-common-name";

    public static final String SECURITY_AUTHENTICATION_KEY =
            "security-authentication";

    public static final String SECURITY_PRINCIPAL_KEY = "security-principal";

    public static final String SECURITY_CREDENTIALS_KEY =
            "security-credentials";

    private static final Logger logger =
            LoggerFactory.getLogger(FilterLdap.class);

    private String[] DIRECTORY_ATTRIBUTES_NEEDED = null;

    private String VERSION = "2";

    private String BIND_FILTER = "";

    private String URL = "";

    private String BASE = "";

    private String FILTER = "";

    private String PASSWORD = "";

    private String[] ATTRIBUTES2RETURN = null;

    private String GROUPS_NAME = null;

    public String SECURITY_AUTHENTICATION = "none";

    public String SECURITY_PRINCIPAL = null;

    public String SECURITY_CREDENTIALS = null;

    //public Boolean REQUIRE_RETURNED_ATTRS = Boolean.FALSE;

    @Override
    public void init(FilterConfig filterConfig) {
        String m = "L init() ";
        try {
            logger.debug(m + ">");
            super.init(filterConfig);
            m = FilterSetup.getFilterNameAbbrev(FILTER_NAME) + " init() ";
            inited = false;
            if (!initErrors) {
                Set temp = new HashSet();
                if (ATTRIBUTES2RETURN == null) {
                    ATTRIBUTES2RETURN = new String[0];
                } else {
                    for (String element : ATTRIBUTES2RETURN) {
                        temp.add(element);
                    }
                }
                if (AUTHENTICATE && PASSWORD != null && !"".equals(PASSWORD)) {
                    temp.add(PASSWORD);
                }
                DIRECTORY_ATTRIBUTES_NEEDED =
                        (String[]) temp.toArray(StringArrayPrototype);

                boolean haveBindMethod = false;
                if (SECURITY_AUTHENTICATION != null
                        && !"".equals(SECURITY_AUTHENTICATION)) {
                    haveBindMethod = true;
                }

                boolean haveSuperUser = false;
                if (SECURITY_PRINCIPAL != null
                        && !"".equals(SECURITY_PRINCIPAL)) {
                    haveSuperUser = true;
                }

                boolean haveSuperUserPassword = false;
                if (SECURITY_CREDENTIALS != null
                        && !"".equals(SECURITY_CREDENTIALS)) {
                    haveSuperUserPassword = true;
                }

                boolean haveUserPasswordAttributeName = false;
                if (PASSWORD != null && !"".equals(PASSWORD)) {
                    haveUserPasswordAttributeName = true;
                }

                boolean commonBindConfigured = false;
                if (haveBindMethod && haveSuperUserPassword) {
                    boolean error = false;
                    if (!haveSuperUser) {
                        error = true;
                    }
                    if (error) {
                        initErrors = true;
                    } else {
                        commonBindConfigured = true;
                    }
                }

                boolean individualBindConfigured = false;
                boolean individualBindTestConfigured = false;

                if (haveBindMethod && !haveSuperUserPassword) {
                    if (haveSuperUser) {
                        individualBindTestConfigured = true;
                    } else {
                        individualBindConfigured = true;
                    }
                }

                boolean individualCompareConfigured = false;
                if (haveUserPasswordAttributeName) {
                    individualCompareConfigured = true;
                }

            }
            if (initErrors) {
                logger.error(m + "not initialized; see previous error");
            }
            inited = true;
        } finally {
            logger.debug(m + "<");
        }
    }

    @Override
    public void destroy() {
        String m = FilterSetup.getFilterNameAbbrev(FILTER_NAME) + " destroy() ";
        try {
            logger.debug(m + ">");
            super.destroy();
        } finally {
            logger.debug(m + "<");
        }
    }

    @Override
    protected void initThisSubclass(String key, String value) {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " initThisSubclass() ";
        try {
            logger.debug(m + ">");
            logger.debug(m + key + "==" + value);
            boolean setLocally = false;
            if (VERSION_KEY.equals(key)) {
                VERSION = value;
                setLocally = true;
            } else if (BIND_FILTER_KEY.equals(key)) {
                BIND_FILTER = value;
                setLocally = true;
            } else if (URL_KEY.equals(key)) {
                URL = value;
                setLocally = true;
            } else if (BASE_KEY.equals(key)) {
                BASE = value;
                setLocally = true;
            } else if (USERID_KEY.equals(key)) {
                setLocally = true;
            } else if (ATTRIBUTES2RETURN_KEY.equals(key)) {
                if (value.indexOf(",") < 0) {
                    if ("".equals(value)) {
                        ATTRIBUTES2RETURN = null;
                    } else {
                        ATTRIBUTES2RETURN = new String[1];
                        ATTRIBUTES2RETURN[0] = value;
                    }
                } else {
                    ATTRIBUTES2RETURN = value.split(",");
                }
                setLocally = true;
            } else if (GROUPS_NAME_KEY.equals(key)) {
                GROUPS_NAME = value;
                setLocally = true;
            } else if (FILTER_KEY.equals(key)) {
                FILTER = value;
                setLocally = true;
            } else if (PASSWORD_KEY.equals(key)) {
                PASSWORD = value;
                setLocally = true;
            } else if (SECURITY_AUTHENTICATION_KEY.equals(key)) {
                SECURITY_AUTHENTICATION = value;
                setLocally = true;
            } else if (SECURITY_PRINCIPAL_KEY.equals(key)) {
                SECURITY_PRINCIPAL = value;
                setLocally = true;
            } else if (SECURITY_CREDENTIALS_KEY.equals(key)) {
                SECURITY_CREDENTIALS = value;
                setLocally = true;
                /*
                 * } else if (REQUIRE_RETURNED_ATTRS_KEY.equals(key)) {
                 * REQUIRE_RETURNED_ATTRS = Boolean.valueOf(value); setLocally =
                 * true;
                 */
            } else {
                logger.debug(m + "deferring to super");
                super.initThisSubclass(key, value);
            }
            if (setLocally) {
                logger.info(m + "known parameter " + key + "==" + value);
            }
        } finally {
            logger.debug(m + "<");
        }
    }

    private final String applyFilter(String filter, String[] args) {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " applyFilter() ";
        String result = filter;
        logger.debug(m + "result==" + result);
        int i = args.length - 1;
        for (; i >= 0; i--) {
            String regex = "\\{" + Integer.toString(i) + "\\}";
            logger.debug(m + "regex ==" + regex);
            logger.debug(m + "arg ==" + args[i]);
            result = result.replaceFirst(regex, args[i]);
            logger.debug(m + "result==" + result);
        }
        return result;
    }

    private boolean bindRequired() {
        boolean bindRequired = "simple".equals(SECURITY_AUTHENTICATION);
        return bindRequired;
    }

    private boolean individualUserBind() {
        boolean individualUserBind =
                bindRequired() && AUTHENTICATE
                        && (PASSWORD == null || "".equals(PASSWORD));
        return individualUserBind;
    }

    private boolean individualUserComparison() {
        boolean individualUserComparison =
                AUTHENTICATE && PASSWORD != null && !"".equals(PASSWORD);
        return individualUserComparison;
    }

    private Hashtable getEnvironment(String userid, String password) {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " getEnvironment() ";
        Hashtable env = null;

        try {
            env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");

            if (VERSION != null && !"".equals(VERSION)) {
                logger.debug(m + "ldap explicit version==" + VERSION);
                env.put(CONTEXT_VERSION_KEY, VERSION);
            }
            logger.debug(m + "ldap version==" + env.get(CONTEXT_VERSION_KEY));

            env.put(Context.PROVIDER_URL, URL);
            logger.debug(m + "ldap url==" + env.get(Context.PROVIDER_URL));

            if (!bindRequired()) {
                logger.debug(m + "\"binding\" anonymously");
            } else {
                env.put(Context.SECURITY_AUTHENTICATION,
                        SECURITY_AUTHENTICATION);

                String userForBind = null;
                String passwordForBind = null;
                if (!individualUserBind()) {
                    userForBind = SECURITY_PRINCIPAL;
                    passwordForBind = SECURITY_CREDENTIALS;
                    logger.debug(m + "binding to protected directory");
                } else {
                    passwordForBind = password;
                    if (SECURITY_PRINCIPAL == null
                            || "".equals(SECURITY_PRINCIPAL)) {
                        userForBind = userid;
                        logger.debug(m + "binding for real user");
                    } else {
                        //simulate test against user-bind at directory server
                        userForBind = SECURITY_PRINCIPAL;
                        logger.debug(m + "binding for --test-- user");
                    }
                }
                env.put(Context.SECURITY_CREDENTIALS, passwordForBind);
                String[] parms = {userForBind};
                String userFormattedForBind = applyFilter(BIND_FILTER, parms);
                env.put(Context.SECURITY_PRINCIPAL, userFormattedForBind);
            }
            logger.debug(m + "bind w " + env.get(Context.SECURITY_AUTHENTICATION));
            logger.debug(m + "user== " + env.get(Context.SECURITY_PRINCIPAL));
            logger.debug(m + "passwd==" + env.get(Context.SECURITY_CREDENTIALS));
        } catch (Throwable th) {
            if (LOG_STACK_TRACES) {
                logger.error(m + "couldn't set up env for DirContext", th);
            } else {
                logger.error(m + "couldn't set up env for DirContext"
                        + th.getMessage());
            }
        } finally {
            logger.debug(m + "< " + env);
        }
        return env;
    }

    private final String getFilter(String userid) {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME) + " getFilter() ";
        logger.debug(m + ">");
        String filter = null;
        try {
            filter = new String(FILTER);
            filter = filter.replaceFirst("\\{0}", userid);
        } catch (Throwable th) {
            if (LOG_STACK_TRACES) {
                logger.error(m + "couldn't set up filter for dir search", th);
            } else {
                logger.error(m + "couldn't set up filter for dir search"
                        + th.getMessage());
            }
        } finally {
            logger.debug(m + "< " + filter);
        }
        return filter;
    }

    private final SearchControls getSearchControls() {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " getSearchControls() ";
        logger.debug(m + ">");
        SearchControls searchControls = null;
        try {
            int nEntries2return = 0;
            int millisecondTimeLimit = 0;
            boolean retobj = true;
            boolean deref = true;
            searchControls =
                    new SearchControls(SearchControls.SUBTREE_SCOPE,
                                       nEntries2return,
                                       millisecondTimeLimit,
                                       DIRECTORY_ATTRIBUTES_NEEDED,
                                       retobj,
                                       deref);
        } catch (Throwable th) {
            if (LOG_STACK_TRACES) {
                logger.error(m + "couldn't set up search controls for dir search",
                          th);
            } else {
                logger.error(m + "couldn't set up search controls for dir search"
                        + th.getMessage());
            }
        } finally {
            logger.debug(m + "< " + searchControls);
        }
        return searchControls;
    }

    private NamingEnumeration getBasicNamingEnumeration(String userid,
                                                        String password,
                                                        String filter,
                                                        SearchControls searchControls,
                                                        Hashtable env)
            throws NamingException, Exception {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " getNamingEnumeration() ";
        logger.debug(m + ">");
        NamingEnumeration ne = null;
        try {
            DirContext ctx;
            try {
                ctx = new InitialDirContext(env);
            } catch (NamingException th) {
                String msg = "exception getting ldap context";
                if (LOG_STACK_TRACES) {
                    logger.error(m + msg, th);
                } else {
                    logger.error(m + msg + " " + th.getMessage());
                }
                throw th;
            }
            if (ctx == null) {
                logger.error(m + "unexpected null ldap context");
                throw new NamingException("");
            }
            try {
                ne = ctx.search(BASE, filter, searchControls);
            } catch (NamingException th) {
                String msg = "exception getting ldap enumeration";
                if (LOG_STACK_TRACES) {
                    logger.error(m + msg, th);
                } else {
                    logger.error(m + msg + " " + th.getMessage());
                }
                throw th;
            }
            if (ne == null) {
                logger.error(m + "unexpected null ldap enumeration");
                throw new NamingException("");
            }
        } finally {
            logger.debug(m + "< " + ne);
        }
        return ne;
    }

    private NamingEnumeration getNamingEnumeration(String userid,
                                                   String password,
                                                   String filter,
                                                   SearchControls searchControls,
                                                   Hashtable env)
            throws NamingException, Exception {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " getNamingEnumeration() ";
        logger.debug(m + ">");
        // this condition is to -further- protect against behavior suggested by
        // log from hull (see below for first-line protection)
        // the idea here is to steer clear of possible trouble in underlying
        // code and avoid calling ldap w/o a needed and practical password

        String msg = "[LDAP: error code 49 - Bind failed: ";

        if (!individualUserBind()) {
            logger.info(m + "-not- binding individual user");
        } else {
            logger.info(m + "-binding- individual user");
            if (password == null) {
                logger.debug(m + "null password");
                if (USE_FILTER.equalsIgnoreCase(PW_NULL)) {
                    logger.debug(m + "-no- pre null password handling");
                } else {
                    if (AUTHENTICATE) {
                        logger.info(m + "-doing- pre null password handling");
                        if (UNAUTHENTICATE_USER_UNCONDITIONALLY
                                .equalsIgnoreCase(PW_NULL)) {
                            logger.info(m
                                    + "pre unauthenticating for null password");
                            throw new NamingException(msg + "null password]");
                        } else if (SKIP_FILTER.equalsIgnoreCase(PW_NULL)) {
                            logger.info(m + "pre ignoring for null passwd");
                            throw new Exception(msg + "null password]");
                        } else {
                            assert true : "bad value for PW_NULL==" + PW_NULL;
                        }
                    }
                }
            } else if ("".equals(password)) {
                logger.debug(m + "0-length password");
                if (USE_FILTER.equalsIgnoreCase(PW_0)) {
                    logger.debug(m + "-no- pre 0-length password handling");
                } else {
                    if (AUTHENTICATE) {
                        logger.info(m + "-doing- pre 0-length password handling");
                        if (UNAUTHENTICATE_USER_UNCONDITIONALLY
                                .equalsIgnoreCase(PW_0)) {
                            logger.info(m + "pre unauthenticating for 0-length password");
                            throw new NamingException(msg
                                    + "0-length password]");
                        } else if (SKIP_FILTER.equalsIgnoreCase(PW_0)) {
                            logger.info(m + "pre ignoring for 0-length passwd");
                            throw new Exception(msg + "0-length password]");
                        } else {
                            assert true : "bad value for PW_0==" + PW_0;
                        }
                    }
                }
            } else {
                assert password.length() > 0;
            }
        }

        NamingEnumeration ne = null;
        try {
            ne =
                    getBasicNamingEnumeration(userid,
                                              password,
                                              filter,
                                              searchControls,
                                              env);
            assert ne != null;
            if (ne.hasMoreElements()) {
                logger.debug(m + "enumeration has elements");
            } else {
                logger.debug(m + "enumeration has no elements, yet no exceptions");
                if (bindRequired() && !individualUserBind()) {
                    logger.debug(m + "failed security bind");
                    throw new NamingException(msg + "failed security bind]");
                }
                if (!AUTHENTICATE) {
                    logger.debug(m
                            + "user authentication -not- done by this filter");
                } else {
                    logger.debug(m + "user authentication -done- by this filter");
                    if (!bindRequired()) {
                        logger.debug(m + "but -not- binding");
                    } else {
                        logger.debug(m + "-and- binding");
                        if (SKIP_FILTER.equalsIgnoreCase(EMPTY_RESULTS)) {
                            logger.debug(m + "passing thru for EMPTY_RESULTS");
                            throw new Exception(msg + "null password]");
                        } else if (UNAUTHENTICATE_USER_UNCONDITIONALLY
                                .equalsIgnoreCase(EMPTY_RESULTS)) {
                            logger.debug(m + "failing for EMPTY_RESULTS");
                            throw new NamingException(msg + "null password]");
                        } else if (USE_FILTER.equalsIgnoreCase(EMPTY_RESULTS)) {
                            logger.debug(m + "passing for EMPTY_RESULTS");
                            //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                        } else if (UNAUTHENTICATE_USER_CONDITIONALLY
                                .equalsIgnoreCase(EMPTY_RESULTS)) {
                            if (ATTRIBUTES2RETURN == null
                                    || ATTRIBUTES2RETURN.length < 1) {
                                logger.debug(m + "fair enough");
                            } else {
                                throw new NamingException(msg + "expected some");
                            }
                        } else {
                            assert true : "bad value for EMPTY_RESULTS=="
                                    + EMPTY_RESULTS;
                        }
                    }
                }
            }
        } finally {
            logger.debug(m + "< " + ne);
        }
        return ne;
    }

    private static Boolean comparePassword(Attributes attributes,
                                           String password,
                                           String passwordAttribute)
            throws PasswordComparisonException {
        String m = "- comparePassword() ";
        logger.debug(m + ">");
        Boolean rc = null;
        try {
            logger.debug(m + "looking for return attribute==" + passwordAttribute);
            Attribute attribute = attributes.get(passwordAttribute);
            if (attribute == null) {
                logger.error(m + "null object");
            } else {
                int size = attribute.size();
                logger.debug(m + "object with n==" + size);
                for (int j = 0; j < size; j++) {
                    Object o = attribute.get(j);
                    if (password.equals(o.toString())) {
                        logger.debug(m + "compares true");
                        if (rc == null) {
                            logger.debug(m + "1st comp:  authenticate");
                            rc = Boolean.TRUE;
                        } else {
                            logger.error(m + "dup comp:  keep previous rc==" + rc);
                        }
                    } else {
                        logger.debug(m + "compares false, -un-authenticate");
                        if (rc == null) {
                            logger.debug(m + "1st comp (fyi)");
                        } else {
                            logger.error(m + "dup comp (fyi)");
                        }
                        rc = Boolean.FALSE;
                    }
                }
            }
        } catch (Throwable th) {
            logger.error(m + "resetting to null rc==" + rc + th.getMessage());
            throw new PasswordComparisonException("in ldap servlet filter", th);
        } finally {
            logger.debug(m + "< " + rc);
        }
        return rc;
    }

    private void getAttributes(Attributes attributes, Map map) throws Throwable {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " getAttributes() ";
        logger.debug(m + ">");
        try {
            for (String key : ATTRIBUTES2RETURN) {
                logger.debug(m + "looking for return attribute==" + key);
                Attribute attribute = attributes.get(key);
                if (attribute == null) {
                    logger.error(m + "null object...continue to next attr sought");
                    continue;
                }
                if (GROUPS_NAME != null && !"".equals(GROUPS_NAME)) {
                    key = GROUPS_NAME;
                    logger.debug(m
                            + "values collected and interpreted as groups=="
                            + key);
                }
                Set values;
                if (map.containsKey(key)) {
                    logger.debug(m + "already a value-set for attribute==" + key);
                    values = (Set) map.get(key);
                } else {
                    logger.debug(m + "making+storing a value-set for attribute=="
                            + key);
                    values = new HashSet();
                    map.put(key, values);
                }
                int size = attribute.size();
                logger.debug(m + "object with n==" + size);
                for (int j = 0; j < size; j++) {
                    Object o = attribute.get(j);
                    values.add(o);
                    logger.debug(m + "added value==" + o.toString() + ", class=="
                            + o.getClass().getName());
                }
            }
        } finally {
            logger.debug(m + "<");
        }
    }

    private Boolean processNamingEnumeration(NamingEnumeration ne,
                                             String password,
                                             Boolean authenticated,
                                             Map map) {
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " processNamingEnumeration() ";
        logger.debug(m + ">");
        try {
            boolean errorOnSomeComparison = false;
            while (ne.hasMoreElements()) {
                logger.debug(m + "another element");
                SearchResult s = null;
                try {
                    Object o = ne.nextElement();
                    logger.debug(m + "got a " + o.getClass().getName());
                    s = (SearchResult) o;
                } catch (Throwable th) {
                    logger.error(m + "naming enum contains obj not SearchResult");
                    continue;
                }
                Attributes attributes = s.getAttributes();
                getAttributes(attributes, map);
                if (individualUserComparison()) {
                    Boolean temp = null;
                    try {
                        temp = comparePassword(attributes, password, PASSWORD);
                        logger.debug(m + "-this- comp yields " + temp);
                        if (authenticated != null && !authenticated) {
                            logger.debug(m + "keeping prev failed authn");
                        } else {
                            logger.debug(m + "replacing prvsuccess or null authn");
                            if (errorOnSomeComparison) {
                                logger.debug(m + "errorOnSomeComparison=="
                                        + errorOnSomeComparison);
                            } else {
                                authenticated = temp;
                            }
                        }
                    } catch (Throwable th) {
                        logger.debug(m
                                + "in iUC conditional, caught throwable th=="
                                + th);
                        errorOnSomeComparison = true;
                        authenticated = null;
                    }
                }
            }
            if (individualUserComparison()) {
                if (errorOnSomeComparison) {
                    logger.debug(m + "exception, so assuring authenticated=="
                            + authenticated);
                    authenticated = null;
                    map.clear();
                } else if (authenticated == null) {
                    authenticated = Boolean.FALSE;
                    logger.debug(m + "no passwd attr found, so authenticated=="
                            + authenticated);
                }
            }
        } catch (Throwable th) { // play it safe:
            map.clear();
            if (authenticated != null && authenticated) {
                // drop an earlier authentication, before exception was thrown
                authenticated = null;
            } // but leave alone a earlier -failed- authentication
            if (LOG_STACK_TRACES) {
                logger.error(m + "ldap filter failure", th);
            } else {
                logger.error(m + "ldap filter failure" + th.getMessage());
            }
        } finally {
            logger.debug(m + "< authenticated==" + authenticated + " map==" + map);
        }
        return authenticated;
    }

    @Override
    public void populateCacheElement(CacheElement cacheElement, String password) {
        //this is heavy on logging for field reporting
        String m =
                FilterSetup.getFilterNameAbbrev(FILTER_NAME)
                        + " populateCacheElement() ";
        logger.debug(m + ">");
        Boolean authenticated = null;
        Map map = new Hashtable();
        try {
            logger.debug(m + "about to call getNamingEnumeration()");

            String filter = getFilter(cacheElement.getUserid());

            SearchControls searchControls = getSearchControls();

            Hashtable env = getEnvironment(cacheElement.getUserid(), password);

            NamingEnumeration ne = null;

            try {
                ne =
                        getNamingEnumeration(cacheElement.getUserid(),
                                             password,
                                             filter,
                                             searchControls,
                                             env);
                assert ne != null;
                logger.debug(m + "got expected non-null ne, no exception thrown");
                if (AUTHENTICATE && individualUserBind()) {
                    authenticated = Boolean.TRUE;
                }
                if (AUTHENTICATE && individualUserBind()
                        && !authenticated.booleanValue()) {
                    logger.debug(m + "-not- calling processNamingEnumeration()");
                } else {
                    logger.debug(m + "about to call processNamingEnumeration()");

                    assert map.isEmpty();
                    try {
                        authenticated =
                                processNamingEnumeration(ne,
                                                         password,
                                                         authenticated,
                                                         map);

                        logger.debug(m + "back from pNE.  AUTHENTICATE=="
                                + AUTHENTICATE + " authenticated=="
                                + authenticated + " map==" + map);
                        if (authenticated != null) {
                            logger.debug(m + "authenticated.booleanValue()=="
                                    + authenticated.booleanValue());
                        }
                        if (map != null) {
                            logger.debug(m + "map.isEmpty()==" + map.isEmpty());
                        }

                        if (AUTHENTICATE
                                && (authenticated == null || !authenticated
                                        .booleanValue())) {
                            map.clear();
                        }

                        logger.debug(m + "before catch");

                    } catch (Throwable th) {
                        map.clear();
                        if (AUTHENTICATE && individualUserBind()) {
                            authenticated = Boolean.FALSE;
                        } else {
                            //to be sure:  likely hasn't changed from initial
                            authenticated = null;
                        }
                        if (LOG_STACK_TRACES) {
                            logger.error(m + "caught th==" + th);
                        } else {
                            logger.error(m + "caught th==" + th.getMessage());
                        }
                    }
                }
            } catch (NamingException e) {
                // the -error- logs here are because, though ne==null
                // never before seen, yet hull log suggests caution and
                // preemptive logging
                logger.error(m + "unexpected null ne w/o exception thrown");
                if (!AUTHENTICATE) {
                    logger.error(m + "wasn't authenticating");
                } else {
                    authenticated = Boolean.FALSE;
                    if (individualUserComparison()) {
                        logger.error(m + "can't do password comparison, so false");
                    } else if (individualUserBind()) {
                        logger.error(m + "accept to mean failed bind, so false");
                    } else {
                        logger.error(m + "authenticating, so now set false");
                    }
                }
            } catch (Exception e) {
                // this seemingly was a condition reached at hull, of course
                // though, in an earlier different code version
                if (AUTHENTICATE && individualUserComparison()) {
                    authenticated = null; //Boolean.FALSE; PASSTHROUGH
                    logger.error(m + "has no ret vals, so reject authentication");
                } else if (AUTHENTICATE && individualUserBind()) {
                    authenticated = null; //Boolean.FALSE; PASSTHROUGH
                    logger.error(m + "has no ret vals, so reject authentication");
                }
            }

        } finally {
            logger.debug(m + "in finally, authenticated==" + authenticated
                    + " map==" + map);
            cacheElement.populate(authenticated, null, map, null);
            logger.debug(m + "<");
        }

    }

}
