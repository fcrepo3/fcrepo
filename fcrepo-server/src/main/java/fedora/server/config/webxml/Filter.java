/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config.webxml;

import java.util.ArrayList;
import java.util.List;

public class Filter {
	
	private final List<String> descriptions;
	
    private String filterName;

    private String filterClass;
    
    private final List<InitParam> initParams;

    public Filter() {
    	descriptions = new ArrayList<String>();
        initParams = new ArrayList<InitParam>();
    }
    
    public List<String> getDescriptions() {
        return descriptions;
    }

    public void addDescription(String description) {
        descriptions.add(description);
    }

    public void removeDescription(String description) {
        descriptions.remove(description);
    }
    
    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }
    
    public List<InitParam> getInitParams() {
        return initParams;
    }

    public void addInitParam(InitParam initParam) {
        initParams.add(initParam);
    }
}
