/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.soapclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet accepts the result of a posted web form containing information
 * about which method parameter values were selected for a dissemination 
 * request.
 * 
 * <p>The information is read from the form and translated into an 
 * appropriate dissemination request and then executes the dissemination
 * request.
 * 
 * @author Ross Wayland
 */
public class MethodParameterResolverServlet
        extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** A string constant for the html MIME type */
    static final private String HTML_CONTENT_TYPE = "text/html";

    /** Servlet mapping for this servlet */
    private static String SERVLET_PATH = null;

    /** Properties file for soap client */
    private static final String soapClientPropertiesFile =
            "WEB-INF/soapclient.properties";

    @Override
    public void init() throws ServletException {
        try {
            System.out
                    .println("Realpath Properties File: "
                            + getServletContext()
                                    .getRealPath(soapClientPropertiesFile));
            FileInputStream fis =
                    new FileInputStream(getServletContext()
                            .getRealPath(soapClientPropertiesFile));
            Properties p = new Properties();
            p.load(fis);
            SERVLET_PATH = p.getProperty("soapClientServletPath");
            System.out.println("soapClientServletPath: " + SERVLET_PATH);

        } catch (Throwable th) {
            String message =
                    "[FedoraSOAPServlet] An error has occurred. "
                            + "The error was a \"" + th.getClass().getName()
                            + "\"  . The " + "Reason: \"" + th.getMessage()
                            + "\"  .";
            throw new ServletException(message);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    // Process the HTTP Post request.
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String PID = null;
        String sDefPID = null;
        String methodName = null;
        String versDateTime = null;
        StringBuffer methodParms = new StringBuffer();
        Hashtable h_methodParms = new Hashtable();
        response.setContentType(HTML_CONTENT_TYPE);
        PrintWriter out = response.getWriter();

        // Get servlet parameters.
        Enumeration parms = request.getParameterNames();
        while (parms.hasMoreElements()) {
            String name = new String((String) parms.nextElement());
            String value = new String(request.getParameter(name));
            if (name.equals("PID")) {
                PID = URLDecoder.decode(request.getParameter(name), "UTF-8");
            } else if (name.equals("sDefPID")) {
                sDefPID =
                        URLDecoder.decode(request.getParameter(name), "UTF-8");
            } else if (name.equals("methodName")) {
                methodName =
                        URLDecoder.decode(request.getParameter(name), "UTF-8");
            } else if (name.equals("asOfDateTime")) {
                versDateTime = request.getParameter(name);
            } else if (name.equals("Submit")) {
                // Submit parameter is ignored.
            } else {
                // Any remaining parameters are assumed to be method parameters
                // so
                // decode and place in hashtable.
                h_methodParms.put(URLDecoder.decode(name, "UTF-8"), URLDecoder
                        .decode(request.getParameter(name), "UTF-8"));
            }
        }

        // Check for any missing required parameters.
        if (PID == null || PID.equalsIgnoreCase("") || sDefPID == null
                || sDefPID.equalsIgnoreCase("") || methodName == null
                || methodName.equalsIgnoreCase("")) {
            String message =
                    "[MethodParameterResolverServlet] Insufficient "
                            + "information to construct dissemination request. Parameters "
                            + "received from web form were: PID: " + PID
                            + " -- sDefPID: " + sDefPID + " -- methodName: "
                            + methodName + " -- methodParms: "
                            + methodParms.toString() + "\".  ";
            System.out.println(message);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               message);
        } else {
            // Translate parameters into dissemination request.
            StringBuffer redirectURL = new StringBuffer();
            redirectURL.append(SERVLET_PATH + "?action_=GetDissemination&"
                    + "PID_=" + PID + "&" + "sDefPID_=" + sDefPID + "&"
                    + "methodName_=" + methodName);
            // Add method parameters.
            int i = 0;
            for (Enumeration e = h_methodParms.keys(); e.hasMoreElements();) {
                String name =
                        URLEncoder.encode((String) e.nextElement(), "UTF-8");
                String value =
                        URLEncoder.encode((String) h_methodParms.get(name),
                                          "UTF-8");
                i++;
                if (i == h_methodParms.size()) {
                    methodParms.append(name + "=" + value);
                } else {
                    methodParms.append(name + "=" + value + "&");
                }

            }
            if (h_methodParms.size() > 0) {
                if (versDateTime == null || versDateTime.equalsIgnoreCase("")) {
                    redirectURL.append("&" + methodParms.toString());
                } else {
                    redirectURL.append("&asOfDateTime_=" + versDateTime + "&"
                            + methodParms.toString());
                }
            } else {
                if (versDateTime != null && !versDateTime.equalsIgnoreCase("")) {
                    redirectURL.append("&asOfDateTime_=" + versDateTime);
                }
            }

            // Redirect to API-A interface.
            response.sendRedirect(redirectURL.toString());
        }
    }

    // Clean up resources
    @Override
    public void destroy() {
    }
}
