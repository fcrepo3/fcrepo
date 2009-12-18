/*
 * File: ListDatastreams.java
 *
 * Copyright 2009 2DC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package melcoe.fedora.pep.rest.objectshandlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import melcoe.fedora.pep.PEPException;
import melcoe.fedora.pep.rest.filters.AbstractFilter;
import melcoe.fedora.pep.rest.filters.DataResponseWrapper;
import melcoe.fedora.util.LogUtil;
import melcoe.xacml.MelcoeXacmlException;
import melcoe.xacml.util.ContextUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

import fedora.common.Constants;

/**
 * Handles the ListDatastreams operation.
 * 
 * @author nish.naidoo@gmail.com
 */
public class ListDatastreams
        extends AbstractFilter {

    private static Logger log =
            Logger.getLogger(ListDatastreams.class.getName());

    private ContextUtil contextUtil = null;

    private Transformer xFormer = null;

    private Tidy tidy = null;

    /**
     * Default constructor.
     * 
     * @throws PEPException
     */
    public ListDatastreams()
            throws PEPException {
        super();

        contextUtil = new ContextUtil();

        try {
            TransformerFactory xFactory = TransformerFactory.newInstance();
            xFormer = xFactory.newTransformer();
        } catch (TransformerConfigurationException tce) {
            throw new PEPException("Error initialising SearchFilter", tce);
        }

        tidy = new Tidy();
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        if (log.isDebugEnabled()) {
            log.debug(this.getClass().getName() + "/handleRequest!");
        }

        String path = request.getPathInfo();
        String[] parts = path.split("/");

        String pid = parts[1];
        String asOfDateTime = request.getParameter("asOfDateTime");
        if (!isDate(asOfDateTime)) {
            asOfDateTime = null;
        }

        RequestCtx req = null;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        try {
            if (pid != null && !"".equals(pid)) {
                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
            }
            if (pid != null && !"".equals(pid)) {
                resAttr.put(new URI(XACML_RESOURCE_ID),
                            new AnyURIAttribute(new URI(pid)));
            }
            if (asOfDateTime != null && !"".equals(asOfDateTime)) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(asOfDateTime));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.LIST_DATASTREAMS
                                .getURI().toASCIIString()));
            actions.put(Constants.ACTION.API.getURI(),
                        new StringAttribute(Constants.ACTION.APIA.getURI()
                                .toASCIIString()));

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.LIST_DATASTREAMS.getURI()
                                    .toASCIIString(),
                            pid,
                            null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }

        return req;
    }

    /*
     * (non-Javadoc)
     * @see
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public RequestCtx handleResponse(HttpServletRequest request,
                                     HttpServletResponse response)
            throws IOException, ServletException {
        DataResponseWrapper res = (DataResponseWrapper) response;
        byte[] data = res.getData();

        String result = null;
        String body = new String(data);

        if (body.startsWith("<html>")) {
            if (log.isDebugEnabled()) {
                log.debug("filtering html");
            }
            result = filterHTML(request, res);
        } else if (body.startsWith("<?xml")) {
            if (log.isDebugEnabled()) {
                log.debug("filtering html");
            }
            result = filterXML(request, res);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("not filtering due to unexpected output: " + body);
            }
            result = body;
        }

        res.setData(result.getBytes());

        return null;
    }

    /**
     * Parses an HTML based response and removes the items that are not
     * permitted.
     * 
     * @param request
     *        the http servlet request
     * @param response
     *        the http servlet response
     * @return the new response body without non-permissable objects.
     * @throws ServletException
     */
    private String filterHTML(HttpServletRequest request,
                              DataResponseWrapper response)
            throws ServletException {
        String path = request.getPathInfo();
        String[] parts = path.split("/");

        String pid = parts[1];

        String body = new String(response.getData());

        InputStream is = new ByteArrayInputStream(body.getBytes());
        Document doc = tidy.parseDOM(is, null);

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList rows = null;
        try {
            rows =
                    (NodeList) xpath.evaluate("//table[2]/tr",
                                              doc,
                                              XPathConstants.NODESET);
            if (log.isDebugEnabled()) {
                log.debug("number of rows found: " + rows.getLength());
            }
        } catch (XPathExpressionException xpe) {
            throw new ServletException("Error parsing HTML for search results: ",
                                       xpe);
        }

        // only the header row, no results.
        if (rows.getLength() < 2) {
            if (log.isDebugEnabled()) {
                log.debug("No results to filter.");
            }
            return body;
        }

        Map<String, Node> dsids = new HashMap<String, Node>();
        for (int x = 1; x < rows.getLength(); x++) {
            NodeList elements = rows.item(x).getChildNodes();
            String dsid = elements.item(0).getFirstChild().getNodeValue();
            if (log.isDebugEnabled()) {
                log.debug("dsid: " + dsid);
            }
            dsids.put(dsid, rows.item(x));
        }

        Set<Result> results =
                evaluatePids(dsids.keySet(), pid, request, response);

        for (Result r : results) {
            if (r.getResource() == null || "".equals(r.getResource())) {
                log
                        .warn("This resource has no resource identifier in the xacml response results!");
            } else if (log.isDebugEnabled()) {
                log.debug("Checking: " + r.getResource());
            }

            String[] ridComponents = r.getResource().split("\\/");
            String rid = ridComponents[ridComponents.length - 1];

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() != Result.DECISION_PERMIT) {
                Node node = dsids.get(rid);
                node.getParentNode().removeChild(node);
                if (log.isDebugEnabled()) {
                    log.debug("Removing: " + r.getResource() + "[" + rid + "]");
                }
            }
        }

        Source src = new DOMSource(doc);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        javax.xml.transform.Result dst = new StreamResult(os);
        try {
            xFormer.transform(src, dst);
        } catch (TransformerException te) {
            throw new ServletException("error generating output", te);
        }

        return new String(os.toByteArray());
    }

    /**
     * Parses an XML based response and removes the items that are not
     * permitted.
     * 
     * @param request
     *        the http servlet request
     * @param response
     *        the http servlet response
     * @return the new response body without non-permissable objects.
     * @throws ServletException
     */
    private String filterXML(HttpServletRequest request,
                             DataResponseWrapper response)
            throws ServletException {
        String body = new String(response.getData());
        DocumentBuilder docBuilder = null;
        Document doc = null;

        try {
            docBuilder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc =
                    docBuilder.parse(new ByteArrayInputStream(response
                            .getData()));
        } catch (Exception e) {
            throw new ServletException(e);
        }

        XPath xpath = XPathFactory.newInstance().newXPath();

        String pid = null;
        NodeList datastreams = null;
        try {
            pid = xpath.evaluate("/objectDatastreams/@pid", doc);
            if (log.isDebugEnabled()) {
                log.debug("filterXML: pid = [" + pid + "]");
            }

            datastreams =
                    (NodeList) xpath.evaluate("/objectDatastreams/datastream",
                                              doc,
                                              XPathConstants.NODESET);
        } catch (XPathExpressionException xpe) {
            throw new ServletException("Error parsing HTML for search results: ",
                                       xpe);
        }

        if (datastreams.getLength() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No results to filter.");
            }

            return body;
        }

        Map<String, Node> dsids = new HashMap<String, Node>();
        for (int x = 0; x < datastreams.getLength(); x++) {
            String dsid =
                    datastreams.item(x).getAttributes().getNamedItem("dsid")
                            .getNodeValue();
            dsids.put(dsid, datastreams.item(x));
        }

        Set<Result> results =
                evaluatePids(dsids.keySet(), pid, request, response);

        for (Result r : results) {
            if (r.getResource() == null || "".equals(r.getResource())) {
                log
                        .warn("This resource has no resource identifier in the xacml response results!");
            } else if (log.isDebugEnabled()) {
                log.debug("Checking: " + r.getResource());
            }

            String[] ridComponents = r.getResource().split("\\/");
            String rid = ridComponents[ridComponents.length - 1];

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() != Result.DECISION_PERMIT) {
                Node node = dsids.get(rid);
                node.getParentNode().removeChild(node);
                if (log.isDebugEnabled()) {
                    log.debug("Removing: " + r.getResource() + "[" + rid + "]");
                }
            }
        }

        Source src = new DOMSource(doc);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        javax.xml.transform.Result dst = new StreamResult(os);
        try {
            xFormer.transform(src, dst);
        } catch (TransformerException te) {
            throw new ServletException("error generating output", te);
        }

        return new String(os.toByteArray());
    }

    /**
     * Takes a given list of PID's and evaluates them.
     * 
     * @param dsids
     *        the list of pids to check
     * @param request
     *        the http servlet request
     * @param response
     *        the http servlet resposne
     * @return a set of XACML results.
     * @throws ServletException
     */
    private Set<Result> evaluatePids(Set<String> dsids,
                                     String pid,
                                     HttpServletRequest request,
                                     DataResponseWrapper response)
            throws ServletException {
        Set<String> requests = new HashSet<String>();
        for (String dsid : dsids) {
            if (log.isDebugEnabled()) {
                log.debug("Checking: " + pid + "/" + dsid);
            }

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr =
                    new HashMap<URI, AttributeValue>();

            try {
                actions.put(Constants.ACTION.ID.getURI(),
                            new StringAttribute(Constants.ACTION.GET_DATASTREAM
                                    .getURI().toASCIIString()));

                resAttr.put(Constants.OBJECT.PID.getURI(),
                            new StringAttribute(pid));
                resAttr.put(new URI(XACML_RESOURCE_ID),
                            new AnyURIAttribute(new URI(pid)));
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsid));

                RequestCtx req =
                        getContextHandler()
                                .buildRequest(getSubjects(request),
                                              actions,
                                              resAttr,
                                              getEnvironment(request));

                String r = contextUtil.makeRequestCtx(req);
                if (log.isDebugEnabled()) {
                    log.debug(r);
                }

                requests.add(r);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ServletException(e.getMessage(), e);
            }
        }

        String res = null;
        ResponseCtx resCtx = null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Number of requests: " + requests.size());
            }

            res =
                    getContextHandler().evaluateBatch(requests
                            .toArray(new String[requests.size()]));

            if (log.isDebugEnabled()) {
                log.debug("Response: " + res);
            }

            resCtx = contextUtil.makeResponseCtx(res);
        } catch (MelcoeXacmlException pe) {
            throw new ServletException("Error evaluating pids: "
                    + pe.getMessage(), pe);
        }

        @SuppressWarnings("unchecked")
        Set<Result> results = resCtx.getResults();

        return results;
    }
}
