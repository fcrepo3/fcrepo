/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Branan
 * @deprecated
 */
@Deprecated
public class FilterRestApiAuthn
        extends FilterEnforceAuthn {

    private static final Logger logger =
            LoggerFactory.getLogger(FilterRestApiAuthn.class);

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest request,
                                  HttpServletResponse response)
            throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug(enter("doThisSubclass()"));
        }

        boolean enforceAuthN = false;

        // Since API-A AuthN is off, leave AuthN off for all GET requests
        // except those which are known to be part of API-M
        if(request.getMethod().equals("GET")) {
            String requestPath = request.getPathInfo();
            if(requestPath != null) {
                // API-M.export() or API-M.getObjectXML()
                if (requestPath.endsWith("/export") ||
                    requestPath.endsWith("/objectXML")) {
                    enforceAuthN = true;
                }
                // API-M.getDatastream()
                if (requestPath.contains("/datastreams/") &&
                    !requestPath.endsWith("/content")) {
                    enforceAuthN = true;
                }
                if (requestPath.endsWith("/datastreams") &&
                    Boolean.valueOf(request.getParameter("profiles"))) {
                    enforceAuthN = true;
                }
                if (!enforceAuthN) logger.warn("No authn requested for {}", requestPath);
            }
        } else {
            enforceAuthN = true;
        }

        if(enforceAuthN) {
            return super.doThisSubclass(request, response);
        } else {
            return false;
        }
    }

}
