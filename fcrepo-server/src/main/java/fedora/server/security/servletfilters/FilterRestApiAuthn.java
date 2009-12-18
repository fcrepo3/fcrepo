/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Branan
 */
public class FilterRestApiAuthn
        extends FilterEnforceAuthn {

    protected static Log log = LogFactory.getLog(FilterRestApiAuthn.class);

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest request,
                                  HttpServletResponse response)
            throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug(enter("doThisSubclass()"));
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
