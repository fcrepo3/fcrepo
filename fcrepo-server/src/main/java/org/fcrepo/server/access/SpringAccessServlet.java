package org.fcrepo.server.access;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.fcrepo.server.SpringServlet;


@SuppressWarnings("serial")
public abstract class SpringAccessServlet extends SpringServlet {

    /** Instance of the access subsystem. */
    protected Access m_access = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        m_access = (Access) m_server
                .getModule("org.fcrepo.server.access.Access");
    }


    @Override
    public void destroy() {
        m_access = null;
        super.destroy();
    }
}