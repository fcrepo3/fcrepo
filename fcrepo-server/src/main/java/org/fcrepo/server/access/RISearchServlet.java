/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.access;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.servletExceptionExtensions.InternalError500Exception;
import org.fcrepo.server.errors.servletExceptionExtensions.RootException;
import org.fcrepo.server.resourceIndex.ResourceIndex;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.utilities.ReadableCharArrayWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trippi.RDFFormat;
import org.trippi.TripleIterator;
import org.trippi.TriplestoreReader;
import org.trippi.TriplestoreWriter;
import org.trippi.TupleIterator;
import org.trippi.server.TrippiServer;
import org.trippi.server.http.Styler;



/**
 * RISearchServlet
 *
 * @version $Id$
 */
public class RISearchServlet
extends SpringAccessServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger =
            LoggerFactory.getLogger(RISearchServlet.class);

    private static final String ACTION_LABEL = "Resource Index Search";
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private Authorization m_authorization;

    private ResourceIndex m_writer;

    private TrippiServer m_trippi;

    private Styler m_styler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        m_writer =
                m_server
                  .getBean("org.fcrepo.server.resourceIndex.ResourceIndex", ResourceIndex.class);
        m_authorization =
                m_server
                  .getBean("org.fcrepo.server.security.Authorization", Authorization.class);

        try {
            URL indexStylesheetPath = getResource(getIndexStylesheetLocation());
            URL formStylesheetPath = getResource(getFormStylesheetLocation());
            URL errorStylesheetPath = getResource(getErrorStylesheetLocation());
            m_styler = new Styler(indexStylesheetPath,
                    formStylesheetPath,
                    errorStylesheetPath);
        } catch (Exception e) {
            throw new ServletException("Error loading stylesheet(s)", e);
        }

    }

    private URL getResource(String loc) throws MalformedURLException {
        return (loc != null) ? getServletContext().getResource(loc) : null;
    }

    public TrippiServer getTrippiServer() throws ServletException {
        if (m_trippi == null){
            m_trippi = new TrippiServer(getWriter());
        }
        return m_trippi;
    }

    public TriplestoreWriter getWriter() throws ServletException {
        if (m_writer == null || m_writer.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            throw new ServletException("The Resource Index Module is not "
                    + "enabled.");
        } else {
            return m_writer;
        }
    }

    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
                    throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            doGet(getTrippiServer(), request, response);
        } catch (ServletException e) {
            throw e;
        } catch (Throwable th) {
            try {
                response.setContentType("text/html; charset=UTF-8");
                response.setStatus(500);
                ReadableCharArrayWriter sWriter = new ReadableCharArrayWriter();
                PrintWriter out = new PrintWriter(sWriter);
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                out.print("<error context=\"");
                enc(getContext(request.getContextPath()), out);
                out.println("\">");
                out.print("<message>");
                enc(getLongestMessage(th, "Error"), out);
                out.println("</message>");
                out.print("<detail><![CDATA[");
                th.printStackTrace(out);
                out.println("]]></detail>");
                out.println("</error>");
                out.flush();
                out.close();
                PrintWriter reallyOut = new PrintWriter(
                        new OutputStreamWriter(
                                response.getOutputStream(), "UTF-8"));
                m_styler.sendError(sWriter.toReader(), reallyOut);
                reallyOut.flush();
                reallyOut.close();
            } catch (Exception e2) {
                log("Error sending error response to browser.", e2);
                throw new ServletException(th);
            }
        }
    }

    private void enc(String in, PrintWriter out) {
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '\'') {
                out.append("&apos;");
            } else if (c == '"') {
                out.append("&quot;");
            } else if (c == '&') {
                out.append("&amp;");
            } else {
                out.append(c);
            }
        }
    }

    private String getLongestMessage(Throwable th, String longestSoFar) {
        if (th.getMessage() != null && th.getMessage().length() > longestSoFar.length()) {
            longestSoFar = th.getMessage();
        }
        Throwable cause = th.getCause();
        if (cause == null) return longestSoFar;
        return getLongestMessage(cause, longestSoFar);
    }

    private void doGet(TrippiServer server,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("doGet()\n" + "  type: "
                    + request.getParameter("type") + "\n" + "  template: "
                    + request.getParameter("template") + "\n" + "  lang: "
                    + request.getParameter("lang") + "\n" + "  query: "
                    + request.getParameter("query") + "\n" + "  limit: "
                    + request.getParameter("limit") + "\n" + "  distinct: "
                    + request.getParameter("distinct") + "\n" + "  format: "
                    + request.getParameter("format") + "\n" + "  flush: "
                    + request.getParameter("flush") + "\n" + "  dumbTypes: "
                    + request.getParameter("dumbTypes") + "\n");
        }
        try {
            Context context =
                    ReadOnlyContext.getContext(Constants.HTTP_REQUEST.REST.uri,
                            request);
            m_authorization.enforceRIFindObjects(context);
            doSearch(server, request, response);
        } catch (AuthzException e) {
            logger.error("Authorization failed for request: "
                    + request.getRequestURI() + " (actionLabel=" + ACTION_LABEL
                    + ")", e);
            throw RootException.getServletException(e,
                    request,
                    ACTION_LABEL,
                    EMPTY_STRING_ARRAY);
        } catch (Throwable th) {
            logger.error("Unexpected error servicing API-A request", th);
            throw new InternalError500Exception("",
                    th,
                    request,
                    ACTION_LABEL,
                    "",
                    EMPTY_STRING_ARRAY);
        }
    }

    private void doSearch(TrippiServer server,
            HttpServletRequest request,
            HttpServletResponse response)
                    throws Exception {
        if (server == null) {
            throw new ServletException("No such triplestore.");
        }
        String type = request.getParameter("type");
        String template = request.getParameter("template");
        String lang = request.getParameter("lang");
        String query = request.getParameter("query");
        String limit = request.getParameter("limit");
        String distinct = request.getParameter("distinct");
        String format = request.getParameter("format");
        String dumbTypes = request.getParameter("dt");
        String stream = request.getParameter("stream");
        boolean streamImmediately = (stream != null) && (stream.toLowerCase().startsWith("t") || stream.toLowerCase().equals("on"));
        String flush = request.getParameter("flush");
        if (type == null && template == null && lang == null && query == null && limit == null && distinct == null && format == null) {
            if (flush == null || flush.isEmpty()) flush = "false";
            boolean doFlush = flush.toLowerCase().startsWith("t");
            if (doFlush) {
                TriplestoreWriter writer = server.getWriter();
                if (writer != null) writer.flushBuffer();
            }
            response.setContentType("text/html; charset=UTF-8");
            doForm(server, new PrintWriter(new OutputStreamWriter(
                    response.getOutputStream(), "UTF-8")),
                    request.getRequestURL().toString(),
                    request.getContextPath());
        } else {
            doFind(server, type, template, lang, query, limit, distinct, format, dumbTypes, streamImmediately, flush, response);
        }
    }

    private void doForm(TrippiServer server,
            PrintWriter out,
            String requestURI,
            String contextPath)
                    throws Exception {
        try {
            ReadableCharArrayWriter sWriter = new ReadableCharArrayWriter();
            PrintWriter sout = new PrintWriter(sWriter);
            sout.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            TriplestoreReader reader = server.getReader();
            sout.print("<query-service href=\"");
            enc(requestURI.replaceAll("/$", ""), out);
            sout.print("\" context=\"");
            enc(getContext(contextPath), sout);
            sout.println("\">");
            sout.println("  <alias-map>");
            Map<String, String> map = reader.getAliasMap();
            Iterator<String> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String name = iter.next();
                String uri = map.get(name);
                sout.print("    <alias name=\"");
                sout.print(name);
                sout.print("\" uri=\"");
                enc(uri, sout);
                sout.println("\"/>");
            }
            sout.println("  </alias-map>");
            sout.println("  <triple-languages>");
            String[] langs = reader.listTripleLanguages();
            for (int i = 0; i < langs.length; i++) {
                sout.print("    <language name=\"");
                enc(langs[i], sout);
                sout.println("\"/>");
            }
            sout.println("  </triple-languages>");
            langs = reader.listTupleLanguages();
            sout.println("  <tuple-languages>");
            for (int i = 0; i < langs.length; i++) {
                sout.println("    <language name=\"");
                enc(langs[i], sout);
                sout.println("\"/>");
            }
            sout.println("  </tuple-languages>");
            sout.println("  <triple-output-formats>");
            RDFFormat[] formats = TripleIterator.OUTPUT_FORMATS;
            for (int i = 0; i < formats.length; i++) {
                sout.print("    <format name=\"");
                enc(formats[i].getName(), sout);
                sout.print("\" encoding=\"");
                sout.print(formats[i].getEncoding());
                sout.print("\" media-type=\"");
                sout.print(formats[i].getMediaType());
                sout.print("\" extension=\"");
                sout.print(formats[i].getExtension());
                sout.println("\"/>");
            }
            sout.println("  </triple-output-formats>");
            sout.println("  <tuple-output-formats>");
            formats = TupleIterator.OUTPUT_FORMATS;
            for (int i = 0; i < formats.length; i++) {
                sout.print("    <format name=\"");
                enc(formats[i].getName(), sout);
                sout.print("\" encoding=\"");
                sout.print(formats[i].getEncoding());
                sout.print("\" media-type=\"");
                sout.print(formats[i].getMediaType());
                sout.print("\" extension=\"");
                sout.print(formats[i].getExtension());
                sout.println("\"/>");
            }
            sout.println("  </tuple-output-formats>");
            sout.println("</query-service>");
            sout.flush();
            m_styler.sendForm(sWriter.toReader(), out);
        } finally {
            try {
                out.flush();
                out.close();
            } catch (Exception ex) {
                log("Error closing response", ex);
            }
        }
    }

    public void doFind(TrippiServer server,
            String type,
            String template,
            String lang,
            String query,
            String limit,
            String distinct,
            String format,
            String dumbTypes,
            boolean streamImmediately,
            String flush,
            HttpServletResponse response) throws Exception {
        OutputStream out = null;
        File tempFile = null;
        try {
            if (streamImmediately) {
                String mediaType =
                        TrippiServer.getResponseMediaType(format,
                                !(type != null && type.equals("triples")),
                                TrippiServer.getBoolean(dumbTypes, false));
                try {
                    response.setContentType(mediaType + "; charset=UTF-8");
                    out = response.getOutputStream();
                    server.find(type, template, lang, query, limit, distinct, format, dumbTypes, flush, out);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ServletException("Error querying", e);
                }
            } else {
                tempFile = File.createTempFile("trippi", "result");
                FileOutputStream tempOut = new FileOutputStream(tempFile);
                String mediaType = server.find(type, template, lang, query, limit, distinct, format, dumbTypes, flush, tempOut);
                tempOut.close();
                response.setContentType(mediaType + "; charset=UTF-8");
                out = response.getOutputStream();
                FileInputStream results = new FileInputStream(tempFile);
                sendStream(results, out);
            }
        } finally {
            // make sure the response stream is closed and the tempfile is deld
            if (out != null) try { out.close(); } catch (Exception e) { }
            if (tempFile != null) tempFile.delete();
        }
    }

    private void sendStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[4096];
            int len;
            while ( ( len = in.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log("Could not close result inputstream.");
            }
        }
    }

    /** Exactly the same behavior as doGet. */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private String getIndexStylesheetLocation() {
        return "ri/index.xsl";
    }

    private String getFormStylesheetLocation() {
        return "ri/form.xsl";
    }

    private String getErrorStylesheetLocation() {
        return "ri/error.xsl";
    }

    private String getContext(String origContext) {
        return "ri";
    }
}
