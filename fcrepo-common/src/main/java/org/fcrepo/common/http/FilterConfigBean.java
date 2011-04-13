
package org.fcrepo.common.http;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

public class FilterConfigBean
        implements FilterConfig {

    private ServletContext cxt;

    private String filterName;

    private final Map<String, String> params =
            new LinkedHashMap<String, String>();

    public void setFilterName(String name) {
        filterName = name;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setServletContext(ServletContext sc) {
        cxt = sc;
    }

    public ServletContext getServletContext() {
        return cxt;
    }

    public void addInitParameter(String key, String value) {
        params.put(key, value);
    }

    public String getInitParameter(String name) {
        return params.get(name);
    }

    public Enumeration<String> getInitParameterNames() {
        return new Enumeration<String>() {

            Iterator<String> i = params.keySet().iterator();

            public boolean hasMoreElements() {
                return i.hasNext();
            }

            public String nextElement() {
                return i.next();
            }
        };
    }

}
