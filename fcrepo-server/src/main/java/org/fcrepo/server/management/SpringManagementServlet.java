package org.fcrepo.server.management;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.fcrepo.server.SpringServlet;


@SuppressWarnings("serial")
public abstract class SpringManagementServlet extends SpringServlet {
    /** Instance of Management subsystem (for storing uploaded files). */
    protected Management m_management = null;

    /**
     * Initialize servlet. Gets a reference to the fedora Server object.
     *
     * @throws ServletException
     *         If the servet cannot be initialized.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        m_management = (Management)
                m_server.getModule("org.fcrepo.server.management.Management");
        if (m_management == null) {
            throw new ServletException("Unable to get Management module from server.");
        }
    }

}