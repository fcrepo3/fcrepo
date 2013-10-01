/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.utilities.RuntimeWSDL;



/**
 * Dynamically provides complete and accurate WSDL files for Fedora APIs. This
 * servlet directly includes the common XSD type definitions in each WSDL file
 * and ensures that the binding address reflects the base URL of the Fedora
 * instance, based on the request URI.
 * 
 * @author Chris Wilper
 */
public class WSDLServlet
        extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * This servlet's path, relative to the root of the webapp. IMPLEMENTATION
     * NOTE: For the servlet to work properly, this should match the url-pattern
     * specified for this servlet's mapping in web.xml. This code intentionally
     * does not use request.getServletPath due to inconsistent behavior in
     * various servlet containers.
     */
    private static final String _SERVLET_PATH = "/wsdl";

    /** Relative path to our XSD source file. */
    private static final String _XSD_PATH = "xsd/fedora-types.xsd";

    /** Source WSDL file relative paths, mapped by name */
    private static final Map<String, String> _WSDL_PATHS = new HashMap<String, String>();

    /** Paths to each service endpoint, relative to the root of the webapp. */
    private static final Map<String, String> _SERVICE_PATHS = new HashMap<String, String>();

    /** $FEDORA_HOME/server */
    private File _serverDir;

    static {
        _WSDL_PATHS.put("API-A", "access/Fedora-API-A.wsdl");
        _WSDL_PATHS.put("API-A-LITE", "access/Fedora-API-A-LITE.wsdl");
        _WSDL_PATHS.put("API-M", "management/Fedora-API-M.wsdl");
        _WSDL_PATHS.put("API-M-LITE", "management/Fedora-API-M-LITE.wsdl");

        _SERVICE_PATHS.put("API-A", "services/access");
        _SERVICE_PATHS.put("API-A-LITE", "");
        _SERVICE_PATHS.put("API-M", "services/management");
        _SERVICE_PATHS.put("API-M-LITE", "management");
    }

    /**
     * Respond to an HTTP GET request. The single parameter, "api", indicates
     * which WSDL file to provide. If no parameters are given, a simple HTML
     * index is given instead.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String api = request.getParameter("api");

        if (api == null || api.length() == 0) {
            response.setContentType("text/html; charset=UTF-8");
            getIndex(response.getWriter());
        } else {
            response.setContentType("text/xml; charset=UTF-8");
            getWSDL(api.toUpperCase(), request.getRequestURL()
                            .toString(), response.getWriter());
        }

        response.flushBuffer();
    }

    /**
     * Get a simple HTML index pointing to each WSDL file provided by the
     * servlet.
     */
    private void getIndex(PrintWriter out) {
        out.append("<html><body>WSDL Index<ul>\n");
        Iterator<String> names = _WSDL_PATHS.keySet().iterator();
        while (names.hasNext()) {
            String name = names.next();
            out.append("<li> <a href=\"?api=" + name + "\">" + name
                    + "</a></li>\n");
        }
        out.append("</ul></body></html>");
    }

    /**
     * Get the self-contained WSDL given the api name and request URL.
     */
    private void getWSDL(String api, String requestURL, PrintWriter out) throws IOException,
            ServletException {

        String wsdlPath = (String) _WSDL_PATHS.get(api);
        if (wsdlPath != null) {

            File schemaFile = new File(_serverDir, _XSD_PATH);
            File sourceWSDL = new File(_serverDir, wsdlPath);

            String baseURL = getFedoraBaseURL(requestURL);
            String svcPath = (String) _SERVICE_PATHS.get(api);

            RuntimeWSDL wsdl =
                    new RuntimeWSDL(schemaFile, sourceWSDL, baseURL + "/"
                            + svcPath);

            wsdl.serialize(out);

        } else {
            throw new ServletException("No such api: '" + api + "'");
        }
    }

    /**
     * Determine the base URL of the Fedora webapp given the request URL to this
     * servlet.
     */
    private String getFedoraBaseURL(String requestURL) throws ServletException {
        int i = requestURL.lastIndexOf(_SERVLET_PATH);
        if (i != -1) {
            return requestURL.substring(0, i);
        } else {
            throw new ServletException("Unable to determine Fedora baseURL "
                    + "from request URL.  Request URL does not contain the "
                    + "string '" + _SERVLET_PATH + "', as expected.");
        }
    }

    /**
     * Initialize by setting serverDir based on FEDORA_HOME.
     */
    @Override
    public void init() throws ServletException {

        String fedoraHome = Constants.FEDORA_HOME;

        if (fedoraHome == null || fedoraHome.length() == 0) {
            throw new ServletException("FEDORA_HOME is not defined");
        } else {
            _serverDir = new File(new File(fedoraHome), "server");
            if (!_serverDir.isDirectory()) {
                throw new ServletException("No such directory: "
                        + _serverDir.getPath());
            }
        }

    }

}
