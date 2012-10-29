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

package org.fcrepo.server.security.xacml.pep.rest.objectshandlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.NamespaceContext;
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

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.pep.rest.filters.DataResponseWrapper;
import org.fcrepo.server.security.xacml.pep.rest.filters.ResponseHandlingRESTFilter;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.fcrepo.server.utilities.CXFUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


/**
 * Filter to handle the FindObjects operation.
 *
 * @author nish.naidoo@gmail.com
 */
public class FindObjects
        extends AbstractFilter implements ResponseHandlingRESTFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(FindObjects.class);

    private static final NamespaceContext TYPES_NAMESPACE = new TypesNamespaceContext();

    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    private static final DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    static {
        // the default namespace must be prefixed to be accessible to xpath
        BUILDER_FACTORY.setNamespaceAware(true);
    }

    private ContextUtil m_contextUtil = null;

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

    public void setContextUtil(ContextUtil contextUtil) {
        m_contextUtil = contextUtil;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleRequest(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        RequestCtx req = null;

        Map<URI, AttributeValue> resAttr;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();

        try {
            resAttr = getRepositoryResources(request);

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.FIND_OBJECTS
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIA.getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.FIND_OBJECTS.uri,
                            Constants.FEDORA_REPOSITORY_PID.uri,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            CXFUtility.getFault(e);
        }

        return req;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
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
            logger.debug("filtering html");
            result = filterHTML(request, res);
        } else if (body.startsWith("<?xml")) {
            logger.debug("filtering xml");
            result = filterXML(request, res);
        } else {
            logger.debug("not filtering due to unexpected output: {}", body);
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
            synchronized(BUILDER_FACTORY){
                docBuilder =
                        BUILDER_FACTORY.newDocumentBuilder();
            }
            doc = docBuilder.parse(new ByteArrayInputStream(response.getData()));
        } catch (Exception e) {
            throw new ServletException(e);
        }

        XPath xpath;
        synchronized(XPATH_FACTORY){
            xpath = XPATH_FACTORY.newXPath();
        }
        xpath.setNamespaceContext(TYPES_NAMESPACE);
        NodeList rows = null;
        try {
            rows =
                    (NodeList) xpath
                            .evaluate("/:result/:resultList/:objectFields/:pid",
                                      doc,
                                      XPathConstants.NODESET);
        } catch (XPathExpressionException xpe) {
            throw new ServletException("Error parsing XML for search results: ",
                                       xpe);
        }

        if (rows.getLength() == 0) {
            logger.debug("No results to filter.");

            return body;
        }

        Map<String, Node> pids = new HashMap<String, Node>();
        for (int x = 0; x < rows.getLength(); x++) {
            Node pid = rows.item(x);
            pids.put(pid.getFirstChild().getNodeValue(), pid.getParentNode());
        }

        Set<Result> results = evaluatePids(pids.keySet(), request, response);

        for (Result r : results) {
            if (r.getResource() == null || "".equals(r.getResource())) {
                logger.warn("This resource has no resource identifier in the xacml response results!");
            } else if (logger.isDebugEnabled()) {
                logger.debug("Checking: {}", r.getResource());
            }

            String[] ridComponents = r.getResource().split("\\/");
            String rid = ridComponents[ridComponents.length - 1];

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() != Result.DECISION_PERMIT) {
                Node node = pids.get(rid);
                node.getParentNode().removeChild(node);
                logger.debug("Removing: {} [{}]", r.getResource(), rid);
            }
        }
        // since namespaces are disabled, set the attribute explicitly
        doc.getDocumentElement().setAttribute("xmlns", "http://www.fedora.info/definitions/1/0/types/");
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

        XPath xpath;
        synchronized(XPATH_FACTORY) {
            xpath = XPATH_FACTORY.newXPath();
        }
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
            logger.debug("No results to filter.");
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
                String pid = pidParentA.getFirstChild().getNodeValue();
                pids.put(pid, rows.item(x));
            }
        }

        Set<Result> results = evaluatePids(pids.keySet(), request, response);

        for (Result r : results) {
            if (r.getResource() == null || "".equals(r.getResource())) {
                logger.warn("This resource has no resource identifier in the xacml response results!");
            } else if (logger.isDebugEnabled()) {
                logger.debug("Checking: {}", r.getResource());
            }

            String[] ridComponents = r.getResource().split("\\/");
            String rid = ridComponents[ridComponents.length - 1];

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() != Result.DECISION_PERMIT) {
                Node node = pids.get(rid);
                node.getParentNode().removeChild(node.getNextSibling());
                node.getParentNode().removeChild(node);
                logger.debug("Removing: {} [{}]", r.getResource(), rid);
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
            logger.debug("Checking: {}", pid);

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr;

            try {
                actions
                        .put(Constants.ACTION.ID.getURI(),
                             Constants.ACTION.LIST_OBJECT_IN_FIELD_SEARCH_RESULTS
                                     .getStringAttribute());
                actions.put(Constants.ACTION.API.getURI(),
                            Constants.ACTION.APIA.getStringAttribute());
                resAttr = getResources(pid);

                RequestCtx req =
                        getContextHandler()
                                .buildRequest(getSubjects(request),
                                              actions,
                                              resAttr,
                                              getEnvironment(request));

                String r = m_contextUtil.makeRequestCtx(req);
                logger.debug(r);

                requests.add(r);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ServletException(e.getMessage(), e);
            }
        }

        String res = null;
        ResponseCtx resCtx = null;
        try {
            logger.debug("Number of requests: {}", requests.size());

            res =
                    getContextHandler().evaluateBatch(requests
                            .toArray(new String[requests.size()]));

            logger.debug("Response: {}", res);

            resCtx = m_contextUtil.makeResponseCtx(res);
        } catch (MelcoeXacmlException pe) {
            throw new ServletException("Error evaluating pids: "
                    + pe.getMessage(), pe);
        }

        @SuppressWarnings("unchecked")
        Set<Result> results = resCtx.getResults();

        return results;
    }

    static class TypesNamespaceContext implements NamespaceContext {
        static List<String> PREFIXES = Arrays.asList(new String[]{"types",""});
        static List<String> XSI_PREFIXES = Arrays.asList(new String[]{"xsi"});
        static ArrayList<String> EMPTY = new ArrayList<String>(0);
        @Override
        public String getNamespaceURI(String prefix) {
            if ("types".equals(prefix) || "".equals(prefix)) {
               return "http://www.fedora.info/definitions/1/0/types/";
            }
            if ("xsi".equals(prefix)){
                return "http://www.w3.org/2001/XMLSchema-instance";
            }
            return "";
        }

        @Override
        public String getPrefix(String uri) {
            if ("http://www.fedora.info/definitions/1/0/types/".equals(uri)) {
                return "types";
            }
            if ("http://www.w3.org/2001/XMLSchema-instance".equals(uri)){
                return "xsi";
            }
            return null;
        }

        @Override
        public Iterator getPrefixes(String uri) {
            if("http://www.fedora.info/definitions/1/0/types/".equals(uri)) {
                return PREFIXES.iterator();
            }
            if ("http://www.w3.org/2001/XMLSchema-instance".equals(uri)){
                return XSI_PREFIXES.iterator();
            }
            return EMPTY.iterator();
        }
    }
}
