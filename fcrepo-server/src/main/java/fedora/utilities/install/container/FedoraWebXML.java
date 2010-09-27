/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fedora.server.config.webxml.ContextParam;
import fedora.server.config.webxml.Filter;
import fedora.server.config.webxml.FilterMapping;
import fedora.server.config.webxml.InitParam;
import fedora.server.config.webxml.SecurityConstraint;
import fedora.server.config.webxml.Servlet;
import fedora.server.config.webxml.ServletMapping;
import fedora.server.config.webxml.UserDataConstraint;
import fedora.server.config.webxml.WebResourceCollection;
import fedora.server.config.webxml.WebXML;
import fedora.utilities.install.InstallOptions;

/**
 * Configures the web.xml for Fedora. This class does not create a complete 
 * web.xml document from scratch. It assumes that the constructor-provided
 * webXML file has already defined the base set of servlets, servlet-mapping,
 * etc. Specifically, we expect the web.xml located in 
 * fcrepo-webapp-fedora/main/webapp/WEB-INF/web.xml.
 *
 * @author Edwin Shin
 */
public class FedoraWebXML {

    protected final String FEDORA_GENERATED =
            "Fedora-generated security-constraint";

    private final String CONFIDENTIAL = "CONFIDENTIAL";

    private final String FILTER_AUTHN = "EnforceAuthnFilter";
    private final String FILTER_RESTAPI = "RestApiAuthnFilter";
    private final String FILTER_PEP = "PEPFilter";
    private final String FILTER_PEP_CLASS = "melcoe.fedora.pep.rest.PEP";
    private final String FILTER_SETUP = "SetupFilter";
    private final String FILTER_XMLUSERFILE = "XmlUserfileFilter";
    private final String FILTER_FINALIZE = "FinalizeFilter";
    private final String FILTER_JAAS = "AuthFilterJAAS";

    private final String[] FILTER_APIA_SERVLET_NAMES =
            new String[] {"AccessServlet", "DescribeRepositoryServlet",
                    "FieldSearchServlet", "GetObjectHistoryServlet",
                    "ListDatastreamsServlet", "ListMethodsServlet",
                    "MethodParameterResolverServlet", "OAIProviderServlet",
                    "ReportServlet", "RISearchServlet"};

    private final String[] FILTER_APIA_URL_PATTERNS =
            new String[] {"/services/access"};

    private final String[] FILTER_APIM_SERVLET_NAMES =
            new String[] {"AxisServlet", "ControlServlet", "GetNextPIDServlet",
                    "UploadServlet"};

    private final String[] FILTER_APIM_URL_PATTERNS =
            new String[] {"/getDSAuthenticated", "/index.html",
                    "/services/management"};

    private final String[] SC_APIA_URL_PATTERNS =
            new String[] {"/", "/describe", "/get/*", "/getAccessParmResolver",
                    "/getObjectHistory/*", "/listDatastreams/*",
                    "/listMethods/*", "/oai", "/report", "/risearch",
                    "/search", "/services/access", "/wsdl", "*.jsp"};

    private final String[] SC_APIM_URL_PATTERNS =
            new String[] {"/index.html", "/getDSAuthenticated",
                    "/management/getNextPID", "/management/upload",
                    "/services/management", "*.jws"};
    
    private final Map<String,String> FESL_SERVLET_MAPPINGS = 
    	new HashMap<String,String>() {
			private static final long serialVersionUID = 1L;
			{put("UserServlet", "/user");
			}
    	};
    	
    //FIXME for FeSL, what about UserServlet and /user url-pattern?

    private final WebXMLOptions options;

    private final WebXML fedoraWebXML;

    public FedoraWebXML(String webXML, InstallOptions options) {
        this(webXML, new WebXMLOptions(options));
    }

    /**
     * 
     * @param webXML path to the webXML file
     * @param options
     */
    public FedoraWebXML(String webXML, WebXMLOptions options) {
        this.options = options;
        fedoraWebXML = fedora.server.config.webxml.WebXML.getInstance(webXML);

        setFedoraHome();
        setFilters();
        setServletMappings();
        setFilterMappings();
        Collections.sort(fedoraWebXML.getFilterMappings(),
                         new FilterMappingComparator());
        setSecurityConstraints();
    }
    
    private void setFilters() {
    	Filter f = new Filter();
		f.setFilterName(FILTER_PEP);
		f.setFilterClass(FILTER_PEP_CLASS);
    	if (options.requireFesl()) {
    		fedoraWebXML.addFilter(f);
    	} else {
    		fedoraWebXML.removeFilter(f);
    	}
    }

    /**
     * Set the servlet-mappings.
     */
    private void setServletMappings() {
    	if (options.requireFesl()) {
    		for (String servletName : FESL_SERVLET_MAPPINGS.keySet()) {
    			addServletMapping(servletName, FESL_SERVLET_MAPPINGS.get(servletName));
    		}
    	} else {
    		for (String servletName : FESL_SERVLET_MAPPINGS.keySet()) {
    			removeServletMapping(servletName, FESL_SERVLET_MAPPINGS.get(servletName));
    		}
    	}
    }
    
    /**
     * Set the filter-mappings. The filter-mappings for APIM are always set.
     */
    private void setFilterMappings() {
        addFilterMappings(FILTER_APIM_SERVLET_NAMES, FILTER_APIM_URL_PATTERNS);

        // AuthN filter for all REST API methods
        FilterMapping fmAll = new FilterMapping();
        fmAll.setFilterName(FILTER_AUTHN);
        fmAll.addServletName("RestServlet");

        // AuthN filter for REST API methods corresponding to API-M
        FilterMapping fmAPIM = new FilterMapping();
        fmAPIM.setFilterName(FILTER_RESTAPI);
        fmAPIM.addServletName("RestServlet");

        if (options.requireApiaAuth()) {
            addFilterMappings(FILTER_APIA_SERVLET_NAMES,
                              FILTER_APIA_URL_PATTERNS);
            fedoraWebXML.addFilterMapping(fmAll);
        } else {
            removeFilterMappings(FILTER_APIA_SERVLET_NAMES,
                                 FILTER_APIA_URL_PATTERNS);
            fedoraWebXML.addFilterMapping(fmAPIM);
        }
        
        // FeSL
        if (options.requireFesl()) {
        	setFeslFilterMappings();
        }
    }

    private void setSecurityConstraints() {
        if (options.requireApimSSL()) {
            addUserDataConstraint(SC_APIM_URL_PATTERNS);
        } else {
            removeUserDataConstraint(SC_APIM_URL_PATTERNS);
        }

        if (options.requireApiaSSL()) {
            addUserDataConstraint(SC_APIA_URL_PATTERNS);
        } else {
            removeUserDataConstraint(SC_APIA_URL_PATTERNS);
        }
    }

    private void addServletMapping(String servletName, String urlPattern) {
        List<ServletMapping> servletMappings =
                fedoraWebXML.getServletMappings();
        for (ServletMapping servletMapping : servletMappings) {
            if (servletMapping.getServletName().equals(servletName)) {
                if (servletMapping.getUrlPatterns().contains(urlPattern)) {
                    return; // servlet-mapping already exists, no need to add
                }
            }
        }
        ServletMapping servletMapping = new ServletMapping();
        servletMapping.setServletName(servletName);
        servletMapping.addUrlPattern(urlPattern);
        fedoraWebXML.addServletMapping(servletMapping);
    }

    /**
     * Removes the servlet-mapping with the given servlet-name and url-pattern.
     *
     * @param servletName
     *        the servlet-name to match
     * @param urlPattern
     *        the url-pattern to match (or null to match any)
     */
    private void removeServletMapping(String servletName, String urlPattern) {
        ServletMapping servletMapping;
        Iterator<ServletMapping> servletMappings =
                fedoraWebXML.getServletMappings().iterator();
        while (servletMappings.hasNext()) {
            servletMapping = servletMappings.next();
            if (servletName == null || servletName.length() == 0
                    || servletMapping.getServletName().equals(servletName)) {
                if (urlPattern == null || urlPattern.length() == 0
                        || servletMapping.getUrlPatterns().contains(urlPattern)) {
                    servletMappings.remove();
                }
            }
        }
    }

    /**
     * Adds a user-data-constraint with transport-guarantee CONFIDENTIAL to the
     * security-constraint that contains <code>urlPatterns</code>. If an
     * existing security-constraint contains a partial match against urlPatterns
     * (i.e., a subset or a superset), the matching url-patterns will be removed
     * and a new security-constraint containing urlPatterns will be created. If
     * no security-constraint contains <code>urlPatterns</code>, a new
     * security-constraint block will be created.
     *
     * @param urlPatterns
     */
    private void addUserDataConstraint(String[] urlPatterns) {
        Set<String> targetSet = new HashSet<String>(Arrays.asList(urlPatterns));
        Set<String> candidateSet;
        boolean hasUserDataConstraint = false;
        Set<SecurityConstraint> removalSet = new HashSet<SecurityConstraint>();

        for (SecurityConstraint sc : fedoraWebXML.getSecurityConstraints()) {
            candidateSet = new HashSet<String>();
            for (WebResourceCollection wrc : sc.getWebResourceCollections()) {
                candidateSet.addAll(wrc.getUrlPatterns());
            }

            if (targetSet.equals(candidateSet)) {
                if (!hasUserDataConstraint) {
                    if (sc.getUserDataConstraint() == null) {
                        sc
                                .setUserDataConstraint(new UserDataConstraint(CONFIDENTIAL));
                    } else if (sc.getUserDataConstraint()
                            .getTransportGuarantee() == null
                            || !sc.getUserDataConstraint()
                                    .getTransportGuarantee()
                                    .equals(CONFIDENTIAL)) {
                        sc.getUserDataConstraint()
                                .setTransportGuarantee(CONFIDENTIAL);
                    }
                    hasUserDataConstraint = true;
                } else {
                    removalSet.add(sc);
                }
            } else if (targetSet.containsAll(candidateSet)
                    || candidateSet.containsAll(targetSet)) {
                candidateSet.removeAll(targetSet);
                if (candidateSet.isEmpty()) {
                    removalSet.add(sc);
                }
            }
        }

        for (SecurityConstraint sc : removalSet) {
            fedoraWebXML.removeSecurityConstraint(sc);
        }

        if (!hasUserDataConstraint) {
            WebResourceCollection wrc = new WebResourceCollection();
            wrc.addDescription(FEDORA_GENERATED);
            for (String urlPattern : targetSet) {
                wrc.addUrlPattern(urlPattern);
            }
            SecurityConstraint sc = new SecurityConstraint();
            sc.addWebResourceCollection(wrc);
            sc.setUserDataConstraint(new UserDataConstraint(CONFIDENTIAL));
            fedoraWebXML.addSecurityConstraint(sc);
        }
    }

    /**
     * Removes the user-data-constraint if the security-constraint contains
     * <code>urlPatterns</code>.
     *
     * @param urlPatterns
     *        The array of url-patterns to match.
     */
    private void removeUserDataConstraint(String[] urlPatterns) {
        List<String> up = Arrays.asList(urlPatterns);

        scLoop: for (SecurityConstraint sc : fedoraWebXML
                .getSecurityConstraints()) {
            for (WebResourceCollection wrc : sc.getWebResourceCollections()) {
                if (wrc.getUrlPatterns().containsAll(up)) {
                    sc.setUserDataConstraint(null);
                    break scLoop;
                }
            }
        }
    }

    private void addFilterMappings(String[] servletNames, String[] urlPatterns) {
        Set<String> servlets = new HashSet<String>(Arrays.asList(servletNames));
        Set<String> urls = new HashSet<String>(Arrays.asList(urlPatterns));

        for (FilterMapping fMap : fedoraWebXML.getFilterMappings()) {
            if (fMap.getFilterName().equals(FILTER_AUTHN)) {
                for (String servletName : fMap.getServletNames()) {
                    servlets.remove(servletName);
                }
                for (String urlPattern : fMap.getUrlPatterns()) {
                    urls.remove(urlPattern);
                }
            }
        }
        
        for (String servletName : servlets) {
            FilterMapping fm = new FilterMapping();
            fm.setFilterName(FILTER_AUTHN);
            fm.addServletName(servletName);
            fedoraWebXML.addFilterMapping(fm);
        }

        for (String urlPattern : urls) {
            FilterMapping fm = new FilterMapping();
            fm.setFilterName(FILTER_AUTHN);
            fm.addUrlPattern(urlPattern);
            fedoraWebXML.addFilterMapping(fm);
        }
    }

    private void removeFilterMappings(String[] servletNames,
                                      String[] urlPatterns) {
        Set<String> servlets = new HashSet<String>(Arrays.asList(servletNames));
        Set<String> urls = new HashSet<String>(Arrays.asList(urlPatterns));

        fedora.server.config.webxml.WebXML fedoraWebXML =
                fedora.server.config.webxml.WebXML.getInstance();
        for (FilterMapping fMap : fedoraWebXML.getFilterMappings()) {
            if (fMap.getFilterName().equals(FILTER_AUTHN)) {
                for (String servletName : fMap.getServletNames()) {
                    if (servlets.contains(servletName)) {
                        fMap.removeServletName(servletName);
                    }
                }
                for (String urlPattern : fMap.getUrlPatterns()) {
                    if (urls.contains(urlPattern)) {
                        fMap.removeUrlPattern(urlPattern);
                    }
                }
                if (fMap.getServletNames().size() == 0
                        && fMap.getUrlPatterns().size() == 0) {
                    fedoraWebXML.removeFilterMapping(fMap);
                }
            }
        }
    }

    /**
     * Set the filter mappings required by FeSL.
     * This involves replacing the legacy policy enforcement filter, FILTER_AUTHN,
     * with PEP_FILTER as well as removing some unneeded filters and adding the
     * FILTER_JAAS filter.
     * It is assumed that the actual servlets or url-patterns that need filter
     * mapping have already been declared previously, with the exception of 
     * FILTER_JAAS.
     */
    private void setFeslFilterMappings() {
    	Collection<String> toDelete = new HashSet<String>();
    	toDelete.add(FILTER_SETUP);
    	toDelete.add(FILTER_XMLUSERFILE);
    	toDelete.add(FILTER_FINALIZE);
    	
    	Collection<String> toReplace = new HashSet<String>();
    	toReplace.add(FILTER_AUTHN);
    	toReplace.add(FILTER_RESTAPI);
    	
    	String filterName;    	
    	FilterMapping fMap;
        Iterator<FilterMapping> filterMappings =
                fedoraWebXML.getFilterMappings().iterator();
        while (filterMappings.hasNext()) {
        	fMap = filterMappings.next();
        	filterName = fMap.getFilterName();
        	if (toReplace.contains(filterName)) {
            	fMap.setFilterName(FILTER_PEP);
            } else if (toDelete.contains(filterName)) {
            	filterMappings.remove();
            }
        }
        
        fMap = new FilterMapping();
        fMap.setFilterName(FILTER_JAAS);
        fMap.addUrlPattern("/*");
        fedoraWebXML.addFilterMapping(fMap);
    }
    
    /**
     * Sets all context-param/param-value and init-param/param-value elements
     * where param-name=fedora.home
     */
    private void setFedoraHome() {
        for (Servlet servlet : fedoraWebXML.getServlets()) {
            for (InitParam param : servlet.getInitParams()) {
                if (param.getParamName().equals("fedora.home")) {
                    param.setParamValue(options.getFedoraHome()
                            .getAbsolutePath());
                }
            }
        }

        for (ContextParam contextParam : fedoraWebXML.getContextParams()) {
            if (contextParam.getParamName().equals("fedora.home")) {
                contextParam.setParamValue(options.getFedoraHome()
                        .getAbsolutePath());
            }
        }
    }

    public void write(Writer outputWriter) throws IOException {
        fedoraWebXML.write(outputWriter);
    }

    /**
     * Ensures that SETUP_FILTER is first, followed by XMLUSERFILE_FILTER, and
     * FINALIZE_FILTER is last.
     *
     * @author Edwin Shin
     */
    class FilterMappingComparator
            implements Comparator<FilterMapping>, Serializable {

        private static final long serialVersionUID = 1L;

        private static final String SETUP_FILTER = "SetupFilter";

        private static final String XMLUSERFILE_FILTER = "XmlUserfileFilter";

        private static final String FINALIZE_FILTER = "FinalizeFilter";

        private static final String WILDCARD_URL_PATTERN = "/*";

        public int compare(FilterMapping fm1, FilterMapping fm2) {
            String fn1 = fm1.getFilterName();
            String fn2 = fm2.getFilterName();

            List<String> sn1 = fm1.getServletNames();
            List<String> sn2 = fm2.getServletNames();

            List<String> up1 = fm1.getUrlPatterns();
            List<String> up2 = fm2.getUrlPatterns();

            // SETUP_FILTER goes first
            if (fn1.equals(SETUP_FILTER) && !up1.isEmpty()
                    && up1.get(0).equals(WILDCARD_URL_PATTERN)) {
                return -1;
            }

            if (fn2.equals(SETUP_FILTER) && !up2.isEmpty()
                    && up2.get(0).equals(WILDCARD_URL_PATTERN)) {
                return 1;
            }

            // XMLUSERFILE_FILTER goes second
            if (fn1.equals(XMLUSERFILE_FILTER) && !up1.isEmpty()
                    && up1.get(0).equals(WILDCARD_URL_PATTERN)) {
                return -1;
            }

            if (fn2.equals(XMLUSERFILE_FILTER) && !up2.isEmpty()
                    && up2.get(0).equals(WILDCARD_URL_PATTERN)) {
                return 1;
            }

            // FINALIZE_FILTER goes last
            if (fn1.equals(FINALIZE_FILTER) && !up1.isEmpty()
                    && up1.get(0).equals(WILDCARD_URL_PATTERN)) {
                return 1;
            }

            if (fn2.equals(FINALIZE_FILTER) && !up2.isEmpty()
                    && up2.get(0).equals(WILDCARD_URL_PATTERN)) {
                return -1;
            }

            // Other WILDCARD_URL_PATTERN filter-mappings start at 3rd place
            if (!up1.isEmpty() && up1.get(0).equals(WILDCARD_URL_PATTERN)
                    && !up2.isEmpty()
                    && up2.get(0).equals(WILDCARD_URL_PATTERN)) {
                return fn1.compareTo(fn2);
            }

            if (!up1.isEmpty() && up1.get(0).equals(WILDCARD_URL_PATTERN)) {
                return -1;
            }

            if (!up2.isEmpty() && up2.get(0).equals(WILDCARD_URL_PATTERN)) {
                return 1;
            }

            int c = fn1.compareTo(fn2);
            if (c != 0) {
                return c;
            } else {
                // i.e., we put filter-mappings with servlet-names ahead of
                // filter-mappings with url-patterns
                if (!sn1.isEmpty() && sn2.isEmpty()) {
                    return -1;
                }

                if (sn1.isEmpty() && !sn2.isEmpty()) {
                    return 1;
                }

                if (!sn1.isEmpty() && !sn2.isEmpty()) {
                    return sn1.get(0).compareToIgnoreCase(sn2.get(0));
                }

                if (!up1.isEmpty() && !up2.isEmpty()) {
                    return up1.get(0).compareToIgnoreCase(up2.get(0));
                }
            }

            if (fm1.equals(fm2)) {
                return 0;
            }

            return fn1.compareTo(fn2);
        }
    }
}
