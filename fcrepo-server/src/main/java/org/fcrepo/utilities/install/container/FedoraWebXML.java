/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.install.container;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import java.util.ArrayList;
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

import org.fcrepo.server.config.webxml.ContextParam;
import org.fcrepo.server.config.webxml.Filter;
import org.fcrepo.server.config.webxml.FilterMapping;
import org.fcrepo.server.config.webxml.InitParam;
import org.fcrepo.server.config.webxml.SecurityConstraint;
import org.fcrepo.server.config.webxml.Servlet;
import org.fcrepo.server.config.webxml.ServletMapping;
import org.fcrepo.server.config.webxml.UserDataConstraint;
import org.fcrepo.server.config.webxml.WebResourceCollection;
import org.fcrepo.server.config.webxml.WebXML;

import org.fcrepo.utilities.install.InstallOptions;


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

    private final String FILTER_AUTHN;
    private final String FILTER_ENFORCE_AUTHN = "EnforceAuthnFilter";
    private final String FILTER_RESTAPI = "RestApiAuthnFilter";
    private final String FILTER_PEP = "PEPFilter";
    private final String FILTER_PEP_CLASS = "org.fcrepo.server.security.xacml.pep.rest.PEP";
    private final String FILTER_SETUP = "SetupFilter";
    private final String FILTER_XMLUSERFILE = "XmlUserfileFilter";
    private final String FILTER_FINALIZE = "FinalizeFilter";
    private final String FILTER_JAAS = "AuthFilterJAAS";


    private final Collection<String> FILTER_APIA_SERVLET_NAMES =
        Arrays.asList("AccessServlet", "DescribeRepositoryServlet",
                      "FieldSearchServlet", "GetObjectHistoryServlet",
                      "ListDatastreamsServlet", "ListMethodsServlet",
                      "MethodParameterResolverServlet", "OAIProviderServlet",
                      "ReportServlet", "RISearchServlet");

    private final Collection<String> FILTER_APIA_URL_PATTERNS =
        Arrays.asList("/services/access");

    private final Collection<String> FILTER_APIM_SERVLET_NAMES =
        Arrays.asList("AxisServlet", "ControlServlet", "GetNextPIDServlet",
                      "UploadServlet", "UserServlet");

    private final Collection<String> FILTER_APIM_URL_PATTERNS =
        Arrays.asList("/getDSAuthenticated", "/index.html",
                      "/services/management", "/user");

    private final Collection<String> SC_APIA_URL_PATTERNS =
        Arrays.asList("/", "/describe", "/get/*", "/getAccessParmResolver",
                      "/getObjectHistory/*", "/listDatastreams/*",
                      "/listMethods/*", "/oai", "/report", "/risearch",
                      "/search", "/services/access", "/wsdl", "*.jsp");

    private final Collection<String> SC_APIM_URL_PATTERNS =
        Arrays.asList("/index.html", "/getDSAuthenticated",
                      "/management/getNextPID", "/management/upload",
                      "/services/management", "*.jws");

    private final Map<String,String> FESL_SERVLET_MAPPINGS =
    	new HashMap<String,String>() {
			private static final long serialVersionUID = 1L;
			{put("UserServlet", "/user");
			}
    	};

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
        fedoraWebXML = org.fcrepo.server.config.webxml.WebXML.getInstance(webXML);
        if (options.requireFeslAuthN()) {
            FILTER_AUTHN = FILTER_JAAS;
        } else {
            FILTER_AUTHN = FILTER_ENFORCE_AUTHN;
        }

        setFedoraHome();
        setFilters();
        setServletMappings();
        setFilterMappings();
        Collections.sort(fedoraWebXML.getFilterMappings(),
                         new FilterMappingComparator());
        setSecurityConstraints();
    }

    /**
     * Add or remove servlet filters based on configuration.
     * At the moment, this only adds or removes the FILTER_PEP servlet filter
     * depending on whether or not FeSL AuthZ was enabled.
     */
    private void setFilters() {
    	Filter f = new Filter();
		f.setFilterName(FILTER_PEP);
		f.setFilterClass(FILTER_PEP_CLASS);
    	if (options.requireFeslAuthZ()) {
    		fedoraWebXML.addFilter(f);
    	} else {
    		fedoraWebXML.removeFilter(f);
    	}

    	for (Filter filter : fedoraWebXML.getFilters()) {
    	    for (InitParam param : filter.getInitParams()) {
                if (param.getParamName().equals("authnAPIA")) {
                    param.setParamValue(Boolean.toString(options.requireApiaAuth()));
                }
            }
    	}
    }

    /**
     * Set the servlet-mappings.
     */
    private void setServletMappings() {
    	if (options.requireFeslAuthN()) {
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
        Set<String> filterNames = new HashSet<String>();
        filterNames.add(FILTER_AUTHN);
        if (options.requireFeslAuthZ()) {
            filterNames.add(FILTER_PEP);
        }

        // Explicitly remove all APIM authentication filter mappings before
        // adding, in the event that the web.xml is mixing legacy & FeSL
        // authn filter-mappings
        removeFilterMappings(Arrays.asList(FILTER_ENFORCE_AUTHN, FILTER_JAAS), FILTER_APIM_SERVLET_NAMES, FILTER_APIM_URL_PATTERNS);

        // Add the APIM filter-mappings
        addFilterMappings(filterNames, FILTER_APIM_SERVLET_NAMES, FILTER_APIM_URL_PATTERNS);

        // APIA filter-mappings
        if (options.requireApiaAuth()) {
            addFilterMappings(filterNames, FILTER_APIA_SERVLET_NAMES,
                              FILTER_APIA_URL_PATTERNS);
        } else {
            removeFilterMappings(filterNames, FILTER_APIA_SERVLET_NAMES,
                                 FILTER_APIA_URL_PATTERNS);
        }

        // REST-API
        removeFilterMappings(Arrays.asList(FILTER_RESTAPI, FILTER_ENFORCE_AUTHN, FILTER_JAAS),
                             Arrays.asList("RestServlet"), null);
        Set<String> restFilters = new HashSet<String>();

        if (options.requireFeslAuthN()) {
            restFilters.add(FILTER_AUTHN);
        } else {
            if (options.requireApiaAuth()) {
                restFilters.add(FILTER_AUTHN);
            } else {
                restFilters.add(FILTER_RESTAPI);
            }
        }
        if (options.requireFeslAuthZ()) {
            restFilters.add(FILTER_PEP);
        }
        for (String fn : restFilters) {
            FilterMapping restFM = new FilterMapping();
            restFM.setFilterName(fn);
            restFM.addServletName("RestServlet");
            fedoraWebXML.addFilterMapping(restFM);
            // and rest upload servlet
            restFM = new FilterMapping();
            restFM.setFilterName(FILTER_AUTHN);
            restFM.addServletName("UploadRestServlet");
            fedoraWebXML.addFilterMapping(restFM);
        }

        // If FeSL AuthN is enabled, remove legacy filter-mappings
        if (options.requireFeslAuthN()) {
            Collection<String> toDelete = Arrays.asList(FILTER_SETUP,
                                                        FILTER_XMLUSERFILE,
                                                        FILTER_FINALIZE);
            String filterName;
            FilterMapping fMap;
            Iterator<FilterMapping> filterMappings =
                    fedoraWebXML.getFilterMappings().iterator();
            while (filterMappings.hasNext()) {
                fMap = filterMappings.next();
                filterName = fMap.getFilterName();
                if (toDelete.contains(filterName)) {
                    filterMappings.remove();
                }
            }
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
    private void addUserDataConstraint(Collection<String> urlPatterns) {
        Set<String> targetSet = new HashSet<String>(urlPatterns);
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
    private void removeUserDataConstraint(Collection<String> urlPatterns) {
        List<String> up = new ArrayList<String>(urlPatterns);

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

    /**
     * For each specified servlet-name and url-pattern, add a filter-mapping
     * with each specified filter-name.
     *
     * @param filterNames
     * @param servletNames
     * @param urlPatterns
     */
    private void addFilterMappings(Collection<String> filterNames,
                                   Collection<String> servletNames,
                                   Collection<String> urlPatterns) {
        if (filterNames == null || filterNames.size() == 0) {
            return;
        }
        if (servletNames == null) {
            servletNames = Collections.emptySet();
        }
        if (urlPatterns == null) {
            urlPatterns = Collections.emptySet();
        }

        // If filter-mappings already exist for a specified servlet-name or
        // url-pattern, don't duplicate it
        for (FilterMapping fMap : fedoraWebXML.getFilterMappings()) {
            String fn = fMap.getFilterName();
            if (filterNames.contains(fn)) {
                for (String servletName : fMap.getServletNames()) {
                    servletNames.remove(servletName);
                }
                for (String urlPattern : fMap.getUrlPatterns()) {
                    urlPatterns.remove(urlPattern);
                }
            }
        }

        for (String servletName : servletNames) {
            for (String filterName : filterNames) {
                FilterMapping fm = new FilterMapping();
                fm.setFilterName(filterName);
                fm.addServletName(servletName);
                fedoraWebXML.addFilterMapping(fm);
            }
        }

        for (String urlPattern : urlPatterns) {
            for (String filterName : filterNames) {
                FilterMapping fm = new FilterMapping();
                fm.setFilterName(filterName);
                fm.addUrlPattern(urlPattern);
                fedoraWebXML.addFilterMapping(fm);
            }
        }
    }

    /**
     * Removes filter-mappings with the specified parameters.
     *
     * @param filterNames
     * @param servletNames
     * @param urlPatterns
     */
    private void removeFilterMappings(Collection<String> filterNames,
                                      Collection<String> servletNames,
                                      Collection<String> urlPatterns) {
        if (filterNames == null || filterNames.size() == 0) {
            return;
        }
        if (servletNames == null) {
            servletNames = Collections.emptySet();
        }
        if (urlPatterns == null) {
            urlPatterns = Collections.emptySet();
        }

        FilterMapping fMap;
        Iterator<FilterMapping> filterMappings = fedoraWebXML.getFilterMappings().iterator();
        while (filterMappings.hasNext()) {
            fMap = filterMappings.next();
            String fn = fMap.getFilterName();
            if (filterNames.contains(fn)) {
                Iterator<String>sNames = fMap.getServletNames().iterator();
                while(sNames.hasNext()) {
                    String servletName = sNames.next();
                    if (servletNames.contains(servletName)) {
                        sNames.remove();
                    }
                }

                Iterator<String>uPatterns = fMap.getUrlPatterns().iterator();
                while(uPatterns.hasNext()) {
                    String urlPattern = uPatterns.next();
                    if (urlPatterns.contains(urlPattern)) {
                        uPatterns.remove();
                    }
                }
                if (fMap.getServletNames().size() == 0
                        && fMap.getUrlPatterns().size() == 0) {
                    filterMappings.remove();
                }
            }
        }
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

        private final String SETUP_FILTER = "SetupFilter";

        private final String XMLUSERFILE_FILTER = "XmlUserfileFilter";

        private final String FINALIZE_FILTER = "FinalizeFilter";

        private final String WILDCARD_URL_PATTERN = "/*";

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

            // FILTER_JAAS mappings always precede FILTER_PEP
            if (fn1.equals(FILTER_JAAS) && fn2.equals(FILTER_PEP)) {
                return -1;
            }

            if (fn2.equals(FILTER_JAAS) && fn1.equals(FILTER_PEP)) {
                return 1;
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
