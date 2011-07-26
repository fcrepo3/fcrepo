/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 * @deprecated
 */
@Deprecated
public class FilterEnforceAuthn
        extends FilterSetup {

    private static final Logger logger =
            LoggerFactory.getLogger(FilterEnforceAuthn.class);

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest request,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.doThisSubclass(request, response);
        request.lockWrapper();

        boolean terminateServletFilterChain =
                request.getUserPrincipal() == null;
        if (terminateServletFilterChain) {
            if (logger.isDebugEnabled()) {
                logger.debug(format(method, "no principal found, sending 401"));
            }
            String realm = "fedora";
            String value = "BASIC realm=\"" + realm + "\"";
            String name = "WWW-Authenticate";
            int sc = HttpServletResponse.SC_UNAUTHORIZED;
            response.reset();
            if (response.containsHeader(name)) {
                response.setHeader(name, value);
            } else {
                response.addHeader(name, value);
            }
            try {
                response.sendError(sc, "supply credentials");
            } catch (IOException e1) {
                logger.error("Error sending error response", e1);
            }
            response.setContentType("text/plain");
            try {
                response.flushBuffer();
            } catch (IOException e) {
                logger.error("Error flushing response", e);
            }
        }
        return terminateServletFilterChain;
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

}
