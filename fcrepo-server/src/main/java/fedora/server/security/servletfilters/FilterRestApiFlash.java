/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Determines if the client is a flash player (based on a query string
 * value of 'flash' being set to true) and wraps the response to
 * guarantee only 2xx range response codes.
 *
 * Responses which would normally return with a status outside of the 2xx
 * range are updated such that the response code is set to 200 and the
 * response body ends with the string "::ERROR(code)" where code
 * is replaced by the actual response code.
 *
 * @author Bill Branan
 */
public class FilterRestApiFlash implements Filter {

    protected static Log log = LogFactory.getLog(FilterRestApiFlash.class);

    /**
     * Required Filter method
     */
    public void init(FilterConfig arg0) throws ServletException {
    }

    /**
     * Perform flash client response filtering
     */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain filterChain)
    throws IOException, ServletException {
        if (log.isDebugEnabled()) {
            log.debug("Entering FilterRestApiFlash.doThisSubclass()");
        }

        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String queryString = httpRequest.getQueryString();
        if(queryString != null &&
           queryString.contains("flash=true")) {
            StatusHttpServletResponseWrapper sHttpResponse =
                new StatusHttpServletResponseWrapper(httpResponse);

            filterChain.doFilter(request, sHttpResponse);

            if(sHttpResponse.status != sHttpResponse.realStatus) {
                // Append the error indicator with real status
                try {
                    ServletOutputStream out = sHttpResponse.getOutputStream();
                    out.print("::ERROR("+sHttpResponse.realStatus+")");
                } catch(IllegalStateException ise) {
                    PrintWriter out = sHttpResponse.getWriter();
                    out.print("::ERROR("+sHttpResponse.realStatus+")");
                }
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Required Filter method
     */
    public void destroy() {
    }

    /**
     * Ensures that all returned HTTP status codes are in the 2xx range,
     * this allows Flash-based client applications to have access to
     * error responses.
     */
    private static class StatusHttpServletResponseWrapper
    extends HttpServletResponseWrapper {
        private int realStatus;
        private int status;

      public StatusHttpServletResponseWrapper(HttpServletResponse response) {
          super(response);
      }

      @Override
      public void setStatus(final int statusCode) {
        realStatus = statusCode;
        if ((statusCode - 200) < 0 ||
            (statusCode - 200) >= 100) {
            // Set code to 200 for all responses
            status = SC_OK;
            super.setStatus(status);
        } else {
            status = statusCode;
        }
      }
    }

}
