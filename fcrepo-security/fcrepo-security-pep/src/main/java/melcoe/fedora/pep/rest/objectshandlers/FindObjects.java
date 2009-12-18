/*
 * File: FindObjects.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
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

import org.apache.axis.AxisFault;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

import fedora.common.Constants;

/**
 * Filter to handle the FindObjects operation.
 * 
 * @author nish.naidoo@gmail.com
 */
public class FindObjects
        extends AbstractFilter {

    private static Logger log = Logger.getLogger(FindObjects.class.getName());

    private ContextUtil contextUtil = null;

    private Transformer xFormer = null;

    private Tidy tidy = null;

    /**
     * Default constructor.
     * 
     * @throws PEPException
     */
    public FindObjects()
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
        RequestCtx req = null;

        Map<URI, AttributeValue> resAttr = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();

        try {
            resAttr.put(Constants.OBJECT.PID.getURI(),
                        new StringAttribute("FedoraRepository"));
            resAttr
                    .put(new URI("urn:oasis:names:tc:xacml:1.0:resource:resource-id"),
                         new AnyURIAttribute(new URI("FedoraRepository")));

            actions.put(Constants.ACTION.ID.getURI(),
                        new StringAttribute(Constants.ACTION.FIND_OBJECTS
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
                            Constants.ACTION.FIND_OBJECTS.getURI()
                                    .toASCIIString(),
                            "FedoraRepository",
                            null);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw AxisFault.makeFault(e);
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
        if (request.getParameter("terms") == null
                && request.getParameter("query") == null
                && request.getParameter("sessionToken") == null) {
            return null;
        }

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
        NodeList rows = null;
        try {
            rows =
                    (NodeList) xpath
                            .evaluate("/result/resultList/objectFields",
                                      doc,
                                      XPathConstants.NODESET);
        } catch (XPathExpressionException xpe) {
            throw new ServletException("Error parsing HTML for search results: ",
                                       xpe);
        }

        if (rows.getLength() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No results to filter.");
            }

            return body;
        }

        Map<String, Node> pids = new HashMap<String, Node>();
        for (int x = 0; x < rows.getLength(); x++) {
            NodeList children = rows.item(x).getChildNodes();
            for (int y = 0; y < children.getLength(); y++) {
                if ("pid".equals(children.item(y).getNodeName())) {
                    pids.put(children.item(y).getFirstChild().getNodeValue(),
                             rows.item(x));
                    break;
                }
            }
        }

        Set<Result> results = evaluatePids(pids.keySet(), request, response);

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
                Node node = pids.get(rid);
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
        String body = new String(response.getData());

        InputStream is = new ByteArrayInputStream(body.getBytes());
        Document doc = tidy.parseDOM(is, null);

        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList rows = null;
        try {
            rows =
                    (NodeList) xpath
                            .evaluate("/html/body/center/center/table/tr",
                                      doc,
                                      XPathConstants.NODESET);
        } catch (XPathExpressionException xpe) {
            throw new ServletException("Error parsing HTML for search results: ",
                                       xpe);
        }

        // only the header row, no results.
        if (rows.getLength() == 1) {
            if (log.isDebugEnabled()) {
                log.debug("No results to filter.");
            }
            return body;
        }

        NodeList headers = rows.item(0).getChildNodes();
        int numHeaders = headers.getLength();
        int pidHeader = -1;

        // ensure we have 'pid' in the list and also that it exists
        for (int x = 0; x < numHeaders; x++) {
            String header =
                    headers.item(x).getFirstChild().getFirstChild()
                            .getNodeValue();
            if ("pid".equals(header)) {
                pidHeader = x;
            }
        }

        if (pidHeader == -1) {
            throw new ServletException("pid field not in result list!");
        }

        Map<String, Node> pids = new HashMap<String, Node>();

        // start from 1 to skip the header row.
        for (int x = 1; x < rows.getLength(); x++) {
            NodeList elements = rows.item(x).getChildNodes();
            Node pidParentA = elements.item(pidHeader).getFirstChild();
            if (pidParentA != null && pidParentA.getNodeName().equals("a")) {
                String pid =
                        elements.item(pidHeader).getFirstChild()
                                .getFirstChild().getNodeValue();
                pids.put(pid, rows.item(x));
            }
        }

        Set<Result> results = evaluatePids(pids.keySet(), request, response);

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
                Node node = pids.get(rid);
                node.getParentNode().removeChild(node.getNextSibling());
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
     * @param pids
     *        the list of pids to check
     * @param request
     *        the http servlet request
     * @param response
     *        the http servlet resposne
     * @return a set of XACML results.
     * @throws ServletException
     */
    private Set<Result> evaluatePids(Set<String> pids,
                                     HttpServletRequest request,
                                     DataResponseWrapper response)
            throws ServletException {
        Set<String> requests = new HashSet<String>();
        for (String pid : pids) {
            if (log.isDebugEnabled()) {
                log.debug("Checking: " + pid);
            }

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr =
                    new HashMap<URI, AttributeValue>();

            try {
                actions
                        .put(Constants.ACTION.ID.getURI(),
                             new StringAttribute(Constants.ACTION.LIST_OBJECT_IN_FIELD_SEARCH_RESULTS
                                     .getURI().toASCIIString()));

                if (pid != null && !"".equals(pid)) {
                    resAttr.put(Constants.OBJECT.PID.getURI(),
                                new StringAttribute(pid));
                }
                if (pid != null && !"".equals(pid)) {
                    resAttr.put(new URI(XACML_RESOURCE_ID),
                                new AnyURIAttribute(new URI(pid)));
                }

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
