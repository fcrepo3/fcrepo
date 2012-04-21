package org.fcrepo.server;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.fcrepo.common.Constants;
import org.fcrepo.server.utilities.status.ServerState;
import org.fcrepo.server.utilities.status.ServerStatusFile;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


@SuppressWarnings("serial")
public abstract class SpringServlet extends HttpServlet {

    protected ApplicationContext m_appContext;

    protected Server m_server;

    protected ServerStatusFile m_status;

    /**
     * Prints a "FEDORA STARTUP ERROR" to STDERR along with the stacktrace of
     * the Throwable (if given) and finally, throws a ServletException.
     */
    protected void failStartup(String message, Throwable th)
            throws ServletException {
        System.err.println("\n**************************");
        System.err.println("** FEDORA STARTUP ERROR **");
        System.err.println("**************************\n");
        System.err.println(message);
        if (th == null) {
            System.err.println();
            throw new ServletException(message);
        } else {
            th.printStackTrace();
            System.err.println();
            throw new ServletException(message, th);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        File fedoraHomeDir = getFedoraHomeDir();
        // get file for writing startup status
        try {
            m_status = new ServerStatusFile(new File(fedoraHomeDir, "server"));
        } catch (Throwable th) {
            failStartup("Error initializing server status file", th);
        }

        try {
            m_appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
            m_server = (Server)m_appContext.getBean("org.fcrepo.server.Server");
            if (m_server == null) failStartup("Could not retrieve org.fcrepo.server.Server bean",null);
        } catch (Throwable th) {
            String msg = "Fedora startup failed";
            try {
                m_status.appendError(ServerState.STARTUP_FAILED, th);
            } catch (Exception e) {
            }
            failStartup(msg, th);
        }
    }

    @Override
    public void destroy(){
        m_appContext = null;
        m_status = null;
    }

    /**
     * Validates and returns the value of FEDORA_HOME.
     *
     * @return the FEDORA_HOME directory.
     * @throws ServletException
     *         if FEDORA_HOME (or fedora.home) was not set, does not denote an
     *         existing directory, or is not writable by the current user.
     */
    private File getFedoraHomeDir() throws ServletException {

        String fedoraHome = Constants.FEDORA_HOME;
        if (fedoraHome == null) {
            failStartup("FEDORA_HOME was not configured properly.  It must be "
                    + "set via the fedora.home servlet init-param (preferred), "
                    + "the fedora.home system property, or the FEDORA_HOME "
                    + "environment variable.", null);
        }
        File fedoraHomeDir = new File(fedoraHome);
        if (!fedoraHomeDir.isDirectory()) {
            failStartup("The FEDORA_HOME directory, " + fedoraHomeDir.getPath()
                    + " does not exist", null);
        }
        File writeTest = new File(fedoraHomeDir, "writeTest.tmp");
        String writeErrorMessage =
                "The FEDORA_HOME directory, " + fedoraHomeDir.getPath()
                        + " is not writable by " + "the current user, "
                        + System.getProperty("user.name");
        try {
            writeTest.createNewFile();
            if (!writeTest.exists()) {
                throw new IOException("");
            }
            writeTest.delete();
        } catch (IOException e) {
            failStartup(writeErrorMessage, null);
        }

        return fedoraHomeDir;
    }

}