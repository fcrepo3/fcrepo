/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config.webxml;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.commons.betwixt.strategy.NamespacePrefixMapper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fedora.common.Constants;

public class WebXML
        implements Constants, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String BETWIXT_MAPPING =
            "/fedora/server/config/webxml/webxml-mapping.xml";

    private String id;

    private String version;

    private String displayName;

    private final List<Listener> listeners;
    
    private final List<ContextParam> contextParams;
    
    private final List<Servlet> servlets;

    private final List<ServletMapping> servletMappings;

    private final List<Filter> filters;

    private final List<FilterMapping> filterMappings;

    private final List<SecurityConstraint> securityConstraints;

    private WelcomeFileList welcomeFileList;

    private final List<ErrorPage> errorPages;

    private LoginConfig loginConfig;

    private final List<SecurityRole> securityRoles;

    public WebXML() {
        listeners = new ArrayList<Listener>();
        contextParams = new ArrayList<ContextParam>();
        servlets = new ArrayList<Servlet>();
        servletMappings = new ArrayList<ServletMapping>();
        filters = new ArrayList<Filter>();
        filterMappings = new ArrayList<FilterMapping>();
        securityConstraints = new ArrayList<SecurityConstraint>();
        errorPages = new ArrayList<ErrorPage>();
        securityRoles = new ArrayList<SecurityRole>();
    }

    public static WebXML getInstance() {
        return new WebXML();
    }

    /**
     * Create an instance of WebXML from the specified file.
     * 
     * @param webxml
     *        Path to web.xml file.
     * @return instance of WebXML
     */
    public static WebXML getInstance(String webxml) {
        WebXML wx = null;
        BeanReader reader = new BeanReader();
        reader.getXMLIntrospector().getConfiguration()
                .setAttributesForPrimitives(false);
        reader.getBindingConfiguration().setMapIDs(false);

        try {
            reader.registerMultiMapping(getBetwixtMapping());
            wx = (WebXML) reader.parse(new File(webxml).toURI().toString());
        } catch (IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return wx;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public List<Listener> getListeners() {
        return listeners;
    }
    
    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    public List<ContextParam> getContextParams() {
        return contextParams;
    }
    
    public void addContextParam(ContextParam contextParam) {
        contextParams.add(contextParam);
    }
    
    public List<Servlet> getServlets() {
        return servlets;
    }

    public void addServlet(Servlet servlet) {
        servlets.add(servlet);
    }

    public void removeServlet(Servlet servlet) {
        servlets.remove(servlet);
    }

    public List<ServletMapping> getServletMappings() {
        return servletMappings;
    }

    public void addServletMapping(ServletMapping servletMapping) {
        servletMappings.add(servletMapping);
    }

    public void removeServletMapping(ServletMapping servletMapping) {
        servletMappings.remove(servletMapping);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public List<FilterMapping> getFilterMappings() {
        return filterMappings;
    }

    public void addFilterMapping(FilterMapping filterMapping) {
        filterMappings.add(filterMapping);
    }
    
    public void removeFilter(Filter filter) {
    	filters.remove(filter);
    }

    public void removeFilterMapping(FilterMapping filterMapping) {
        filterMappings.remove(filterMapping);
    }

    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    public void addSecurityConstraint(SecurityConstraint securityConstraint) {
        securityConstraints.add(securityConstraint);
    }

    public void removeSecurityConstraint(SecurityConstraint securityConstraint) {
        securityConstraints.remove(securityConstraint);
    }

    public WelcomeFileList getWelcomeFileList() {
        return welcomeFileList;
    }

    public void setWelcomeFileList(WelcomeFileList welcomeFileList) {
        this.welcomeFileList = welcomeFileList;
    }

    public List<ErrorPage> getErrorPages() {
        return errorPages;
    }

    public void addErrorPage(ErrorPage errorPage) {
        errorPages.add(errorPage);
    }

    public LoginConfig getLoginConfig() {
        return loginConfig;
    }

    public void setLoginConfig(LoginConfig loginConfig) {
        this.loginConfig = loginConfig;
    }

    public List<SecurityRole> getSecurityRoles() {
        return securityRoles;
    }

    public void addSecurityRole(SecurityRole securityRole) {
        securityRoles.add(securityRole);
    }

    public void write(Writer outputWriter) throws IOException {
        //
        NamespacePrefixMapper nspm = new NamespacePrefixMapper();
        nspm.setPrefix(XSI.uri, "xsi");
        nspm.setPrefix("http://java.sun.com/xml/ns/j2ee", "xmlns");
        //

        outputWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        BeanWriter beanWriter = new BeanWriter(outputWriter);
        beanWriter.getBindingConfiguration().setMapIDs(false);
        beanWriter.setWriteEmptyElements(false);
        beanWriter.enablePrettyPrint();
        try {
            beanWriter.getXMLIntrospector().register(getBetwixtMapping());
            beanWriter.getXMLIntrospector().getConfiguration()
                    .setPrefixMapper(nspm);
            beanWriter.write("web-app", this);
        } catch (IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        beanWriter.flush();
        beanWriter.close();
    }

    private static InputSource getBetwixtMapping() {
        return new InputSource(WebXML.class
                .getResourceAsStream(BETWIXT_MAPPING));
    }
}
