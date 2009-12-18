/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config.webxml;

import java.util.ArrayList;
import java.util.List;

public class ServletMapping {

    private String servletName;

    /**
     * Only one url-pattern per servlet-mapping is supported pre-Servlet 2.5.
     */
    private final List<String> urlPatterns;

    public ServletMapping() {
        urlPatterns = new ArrayList<String>();
    }

    public String getServletName() {
        return servletName;
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    /**
     * Only one url-pattern per servlet-mapping is supported pre-Servlet 2.5.
     * 
     * @param urlPattern
     *        the url-pattern to add to this ServletMapping.
     */
    public void addUrlPattern(String urlPattern) {
        urlPatterns.add(urlPattern);
    }
}
