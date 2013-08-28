/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.oai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.errors.authorization.AuthzPermittedException;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;



/**
 * @author Chris Wilper
 */
@SuppressWarnings("serial")
public abstract class OAIProviderServlet
        extends HttpServlet {

    public OAIProviderServlet() {
    }

    public static final String ACTION_LABEL = "OAI";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            HashMap params = new HashMap();
            Enumeration enm = request.getParameterNames();
            while (enm.hasMoreElements()) {
                String name = (String) enm.nextElement();
                params.put(name, request.getParameter(name));
            }
            ReadableByteArrayOutputStream out = new ReadableByteArrayOutputStream();
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                               request);
            try {
                getResponder().respond(context, params, out);
                out.close();
            } catch (AuthzException ae) {
                throw RootException.getServletException(ae,
                                                        request,
                                                        ACTION_LABEL,
                                                        new String[0]);
            }
            response.setContentType("text/xml; charset=UTF-8");
            response.getWriter().print(out.getString(Charset.forName("UTF-8")));
        } catch (Throwable t) {
            throw new InternalError500Exception("",
                                                t,
                                                request,
                                                ACTION_LABEL,
                                                "",
                                                new String[0]);
        }
    }

    private static String getMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null) {
            msg = "Unexpected repository error.";
        }
        return msg;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    public void test(String[] args) throws OAIException, RepositoryException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Context context =
                ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                                           null);
        try {
            getResponder().respond(context, getAsParameterMap(args), out);
        } catch (AuthzOperationalException aoe) {
            System.out.println("403 - operational");
        } catch (AuthzDeniedException ade) {
            System.out.println("403");
        } catch (AuthzPermittedException ape) {
            System.out.println("100");
        } catch (AuthzException ae) {
            System.out.println("403 - general");
        }
        System.out.println(new String(out.toByteArray()));
    }

    public abstract OAIResponder getResponder() throws RepositoryException;

    public static HashMap getAsParameterMap(String[] args) {
        HashMap h = new HashMap();
        for (String arg : args) {
            int pos = arg.indexOf("=");
            if (pos != -1) {
                String name = arg.substring(0, pos);
                String value = arg.substring(pos + 1);
                h.put(name, value);
            }
        }
        return h;
    }

}
