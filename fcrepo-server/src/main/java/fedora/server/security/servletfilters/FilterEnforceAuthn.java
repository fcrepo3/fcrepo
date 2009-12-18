/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Niebel
 */
public class FilterEnforceAuthn
        extends FilterSetup {

    protected static Log log = LogFactory.getLog(FilterEnforceAuthn.class);

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest request,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.doThisSubclass(request, response);
        request.lockWrapper();

        boolean terminateServletFilterChain =
                request.getUserPrincipal() == null;
        if (terminateServletFilterChain) {
            if (log.isDebugEnabled()) {
                log.debug(format(method, "no principal found, sending 401"));
            }
            String realm = "fedora";
            String value = "BASIC realm=\"" + realm + "\"";
            String name = "WWW-Authenticate";
            int sc = HttpServletResponse.SC_UNAUTHORIZED;
            response.reset();
            //httpServletResponse.sendError(sc, "supply credentials"); //same as after
            if (response.containsHeader(name)) {
                response.setHeader(name, value);
            } else {
                response.addHeader(name, value);
            }
            try {
                response.sendError(sc, "supply credentials");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } //here, no bad auth msg at wget
            response.setContentType("text/plain");
            try {
                response.flushBuffer();
            } catch (IOException e) {
                showThrowable(e, log, "response flush error");
            }
        }
        return terminateServletFilterChain;
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

}
