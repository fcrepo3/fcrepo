/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
public class FilterSetup
        extends Base
        implements Filter {

    private static final Logger logger =
            LoggerFactory.getLogger(FilterSetup.class);

    protected static final String NOT_SET = "NOT SET";

    protected String FILTER_NAME = NOT_SET;

    protected boolean inited = false;

    public static final String getFilterNameAbbrev(String filterName) {
        LoggerFactory.getLogger(FilterSetup.class).debug(">>>>>>>>>>>>>>>>>>"
                + filterName);
        String rc = filterName;
        if ("XmlUserfileFilter".equals(filterName)) {
            rc = "X";
        } else if ("PubcookieFilter".equals(filterName)) {
            rc = "P";
        } else if ("LdapFilter".equals(filterName)) {
            rc = "L";
        } else if ("LdapFilterForAttributes".equals(filterName)) {
            rc = "A";
        } else if ("LdapFilterForGroups".equals(filterName)) {
            rc = "G";
        }
        return rc;
    }

    public void init(FilterConfig filterConfig) {
        String method = "init() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        inited = false;
        initErrors = false;
        if (filterConfig != null) {
            FILTER_NAME = filterConfig.getFilterName();
            if (FILTER_NAME == null || "".equals(FILTER_NAME)) {
                if (logger.isErrorEnabled()) {
                    logger.error(format(method, "FILTER_NAME not set"));
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(format(method, null, "FILTER_NAME", FILTER_NAME));
                }
                Enumeration enumer = filterConfig.getInitParameterNames();
                while (enumer.hasMoreElements()) {
                    String key = (String) enumer.nextElement();
                    String value = filterConfig.getInitParameter(key);
                    initThisSubclass(key, value);
                }
                inited = true;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    public void destroy() {
        String method = "destroy()";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    @Override
    protected void initThisSubclass(String key, String value) {
        logger.debug("AF.iTS");
        String method = "initThisSubclass() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.initThisSubclass(key, value);
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    public ExtendedHttpServletRequest wrap(HttpServletRequest httpServletRequest)
            throws Exception {
        String method = "wrap() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        ExtendedHttpServletRequestWrapper wrap =
                new ExtendedHttpServletRequestWrapper(httpServletRequest);
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
        return wrap;
    }

    public boolean doThisSubclass(ExtendedHttpServletRequest extendedHttpServletRequest,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        String test = null;

        test = "init";
        if (!inited || initErrors) {
            if (logger.isErrorEnabled()) {
                logger.error("inited==" + inited);
            }
            if (logger.isErrorEnabled()) {
                logger.error("initErrors==" + initErrors);
            }
            String msg = fail(method, test);
            if (logger.isErrorEnabled()) {
                logger.error(msg);
            }
            throw new Exception(msg);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(pass(method, test));
        }

        test = "HttpServletRequest";
        if (!(extendedHttpServletRequest instanceof HttpServletRequest)) {
            String msg = fail(method, test);
            if (logger.isErrorEnabled()) {
                logger.error(msg);
            }
            throw new Exception(msg);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(pass(method, test));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }

        return false; // i.e., don't signal to terminate servlet filter chain
    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse response,
                         FilterChain chain) throws ServletException {
        String method = "doFilter() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(format(method, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(format(method, "FILTER_NAME", FILTER_NAME));
        }
        String test = null;
        boolean terminateServletFilterChain = false;
        ExtendedHttpServletRequest extendedHttpServletRequest = null;
        try {

            //only one filter should wrap
            if (servletRequest instanceof ExtendedHttpServletRequest) {
                logger.debug(format(method, "using existing request..."));
                extendedHttpServletRequest =
                        (ExtendedHttpServletRequest) servletRequest;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(format(method, "wrapping request..."));
                }
                extendedHttpServletRequest =
                        wrap((HttpServletRequest) servletRequest);
            }

            test = "HttpServletResponse";
            if (!(response instanceof HttpServletResponse)) {
                String msg = fail(method, test);
                if (logger.isErrorEnabled()) {
                    logger.error(msg);
                }
                throw new Exception(msg);
            }
            if (logger.isDebugEnabled()) {
                logger.debug(pass(method, test));
            }

            terminateServletFilterChain =
                    doThisSubclass(extendedHttpServletRequest,
                                   (HttpServletResponse) response);

        } catch (Throwable th) {
            logger.error("Error processing filter", th);
            //current filter should not break the filter chain -- go ahead, regardless of internal failure
        }

        try {
            if (logger.isDebugEnabled()) {
                logger.debug(format(method, "before next doFilter()"));
                logger.debug(format(method, null, "extendedHttpServletRequest")
                        + extendedHttpServletRequest);
                logger.debug(format(method,
                                 "extendedHttpServletRequest",
                                 extendedHttpServletRequest.getClass()
                                         .getName()));
                logger.debug(format(method, null, "response" + response));
            }
            if (terminateServletFilterChain) {
                logger.debug(format(method, "terminating servlet filter chain"));
            } else {
                chain.doFilter(extendedHttpServletRequest, response);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("back from next doFilter()");
            }
        } catch (ServletException e) {
            throw e;
        } catch (Throwable th) {
            logger.error("Can't do next doFilter()", th);
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug(exit(method));
            }
        }
    }

}
