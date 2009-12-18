/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config.webxml;

import java.util.ArrayList;
import java.util.List;

public class FilterMapping {

    private String filterName;

    /**
     * Only one servlet-name per filter-mapping is supported pre-Servlet 2.5.
     */
    private final List<String> servletNames;

    /**
     * Only one url-pattern per filter-mapping is supported pre-Servlet 2.5.
     */
    private final List<String> urlPatterns;

    public FilterMapping() {
        servletNames = new ArrayList<String>();
        urlPatterns = new ArrayList<String>();
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public List<String> getServletNames() {
        return servletNames;
    }

    public void addServletName(String servletName) {
        servletNames.add(servletName);
    }

    public void removeServletName(String servletName) {
        urlPatterns.remove(servletName);
    }

    public List<String> getUrlPatterns() {
        return urlPatterns;
    }

    public void addUrlPattern(String urlPattern) {
        urlPatterns.add(urlPattern);
    }

    public void removeUrlPattern(String urlPattern) {
        urlPatterns.remove(urlPattern);
    }
}
