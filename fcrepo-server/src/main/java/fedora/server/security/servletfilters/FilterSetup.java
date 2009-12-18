/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Niebel
 */
public class FilterSetup
        extends Base
        implements Filter {

    protected static Log log = LogFactory.getLog(FilterSetup.class);

    protected static final String NOT_SET = "NOT SET";

    protected String FILTER_NAME = NOT_SET;

    protected boolean inited = false;

    public static final String getFilterNameAbbrev(String filterName) {
        LogFactory.getLog(FilterSetup.class).debug(">>>>>>>>>>>>>>>>>>"
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
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        inited = false;
        initErrors = false;
        if (filterConfig != null) {
            FILTER_NAME = filterConfig.getFilterName();
            if (FILTER_NAME == null || "".equals(FILTER_NAME)) {
                if (log.isErrorEnabled()) {
                    log.error(format(method, "FILTER_NAME not set"));
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(format(method, null, "FILTER_NAME", FILTER_NAME));
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
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    public void destroy() {
        String method = "destroy()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    protected void initThisSubclass(String key, String value) {
        log.debug("AF.iTS");
        String method = "initThisSubclass() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.initThisSubclass(key, value);
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    public ExtendedHttpServletRequest wrap(HttpServletRequest httpServletRequest)
            throws Exception {
        String method = "wrap() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        ExtendedHttpServletRequestWrapper wrap =
                new ExtendedHttpServletRequestWrapper(httpServletRequest);
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
        return wrap;
    }

    public boolean doThisSubclass(ExtendedHttpServletRequest extendedHttpServletRequest,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        String test = null;

        test = "init";
        if (!inited || initErrors) {
            if (log.isErrorEnabled()) {
                log.error("inited==" + inited);
            }
            if (log.isErrorEnabled()) {
                log.error("initErrors==" + initErrors);
            }
            String msg = fail(method, test);
            if (log.isErrorEnabled()) {
                log.error(msg);
            }
            throw new Exception(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(pass(method, test));
        }

        test = "HttpServletRequest";
        if (!(extendedHttpServletRequest instanceof HttpServletRequest)) {
            String msg = fail(method, test);
            if (log.isErrorEnabled()) {
                log.error(msg);
            }
            throw new Exception(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(pass(method, test));
        }

        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }

        return false; // i.e., don't signal to terminate servlet filter chain         
    }

    public void doFilter(ServletRequest servletRequest,
                         ServletResponse response,
                         FilterChain chain) throws ServletException {
        String method = "doFilter() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        if (log.isDebugEnabled()) {
            log.debug(format(method, "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"));
        }
        if (log.isDebugEnabled()) {
            log.debug(format(method, "FILTER_NAME", FILTER_NAME));
        }
        String test = null;
        boolean terminateServletFilterChain = false;
        ExtendedHttpServletRequest extendedHttpServletRequest = null;
        try {

            //only one filter should wrap
            if (servletRequest instanceof ExtendedHttpServletRequest) {
                log.debug(format(method, "using existing request..."));
                extendedHttpServletRequest =
                        (ExtendedHttpServletRequest) servletRequest;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(format(method, "wrapping request..."));
                }
                extendedHttpServletRequest =
                        wrap((HttpServletRequest) servletRequest);
            }

            test = "HttpServletResponse";
            if (!(response instanceof HttpServletResponse)) {
                String msg = fail(method, test);
                if (log.isErrorEnabled()) {
                    log.error(msg);
                }
                throw new Exception(msg);
            }
            if (log.isDebugEnabled()) {
                log.debug(pass(method, test));
            }

            terminateServletFilterChain =
                    doThisSubclass(extendedHttpServletRequest,
                                   (HttpServletResponse) response);

        } catch (Throwable th) {
            showThrowable(th, log, "can't process this filter()");
            //current filter should not break the filter chain -- go ahead, regardless of internal failure
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug(format(method, "before next doFilter()"));
                log.debug(format(method, null, "extendedHttpServletRequest")
                        + extendedHttpServletRequest);
                log.debug(format(method,
                                 "extendedHttpServletRequest",
                                 extendedHttpServletRequest.getClass()
                                         .getName()));
                log.debug(format(method, null, "response" + response));
            }
            if (terminateServletFilterChain) {
                log.debug(format(method, "terminating servlet filter chain"));
            } else {
                chain.doFilter(extendedHttpServletRequest, response);
            }
            if (log.isDebugEnabled()) {
                log.debug("back from next doFilter()");
            }
        } catch (ServletException e) {
            throw e;
        } catch (Throwable th) {
            showThrowable(th, log, "can't do next doFilter()");
        } finally {
            if (log.isDebugEnabled()) {
                log.debug(exit(method));
            }
        }
    }

}
