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

package org.fcrepo.server.security.xacml.pep.rest.objectshandlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.RequestCtx;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.pep.rest.filters.AbstractFilter;
import org.fcrepo.server.security.xacml.pep.rest.filters.DataResponseWrapper;
import org.fcrepo.server.security.xacml.pep.rest.filters.ResponseHandlingRESTFilter;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.fcrepo.utilities.XmlTransformUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.DateTimeAttribute;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.ctx.ResponseCtx;
import org.jboss.security.xacml.sunxacml.ctx.Result;
import org.jboss.security.xacml.sunxacml.ctx.Status;


/**
 * Handles the ListDatastreams operation.
 *
 * @author nish.naidoo@gmail.com
 */
public class ListDatastreams
        extends AbstractFilter implements ResponseHandlingRESTFilter{

    private static final Logger logger =
            LoggerFactory.getLogger(ListDatastreams.class);

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

        try {
            xFormer = XmlTransformUtility.getTransformer();
        } catch (Exception e) {
            throw new PEPException("Error initialising SearchFilter", e);
        }

        tidy = new Tidy();
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);
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
        if (logger.isDebugEnabled()) {
            logger.debug("{}/handleRequest!", this.getClass().getName());
        }

        String asOfDateTime = request.getParameter("asOfDateTime");
        if (!isDate(asOfDateTime)) {
            asOfDateTime = null;
        }

        RequestCtx req = null;
        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;
        try {
            String[] parts = getPathParts(request);
            resAttr = ResourceAttributes.getResources(parts);
            if (asOfDateTime != null && !asOfDateTime.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.AS_OF_DATETIME.getURI(),
                            DateTimeAttribute.getInstance(asOfDateTime));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.LIST_DATASTREAMS
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIA.getStringAttribute());

            req =
                    getContextHandler().buildRequest(getSubjects(request),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(request));

            String pid = resAttr.get(Constants.OBJECT.PID.getURI()).toString();
            LogUtil.statLog(request.getRemoteUser(),
                            Constants.ACTION.LIST_DATASTREAMS.uri,
                            pid,
                            null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
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
        DataResponseWrapper res = (DataResponseWrapper) response;
        byte[] data = res.getData();

        String result = null;
        String body = new String(data);

        if (body.startsWith("<html>")) {
            if (logger.isDebugEnabled()) {
                logger.debug("filtering html");
            }
            result = filterHTML(request, res);
        } else if (body.startsWith("<?xml")) {
            if (logger.isDebugEnabled()) {
                logger.debug("filtering html");
            }
            result = filterXML(request, res);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("not filtering due to unexpected output: " + body);
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
            if (logger.isDebugEnabled()) {
                logger.debug("number of rows found: " + rows.getLength());
            }
        } catch (XPathExpressionException xpe) {
            throw new ServletException("Error parsing HTML for search results: ",
                                       xpe);
        }

        // only the header row, no results.
        if (rows.getLength() < 2) {
            if (logger.isDebugEnabled()) {
                logger.debug("No results to filter.");
            }
            return body;
        }

        Map<String, Node> dsids = new HashMap<String, Node>();
        for (int x = 1; x < rows.getLength(); x++) {
            NodeList elements = rows.item(x).getChildNodes();
            String dsid = elements.item(0).getFirstChild().getNodeValue();
            if (logger.isDebugEnabled()) {
                logger.debug("dsid: " + dsid);
            }
            dsids.put(dsid, rows.item(x));
        }

        Set<Result> results =
                evaluatePids(dsids.keySet(), pid, request, response);

        for (Result r : results) {
            String resource = r.getResource();
            if (resource == null || resource.isEmpty()) {
                logger.warn("This resource has no resource identifier in the xacml response results!");
            } else if (logger.isDebugEnabled()) {
                logger.debug("Checking: " + r.getResource());
            }

            int lastSlash = resource.lastIndexOf('/');
            String rid = resource.substring(lastSlash + 1);

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() != Result.DECISION_PERMIT) {
                Node node = dsids.get(rid);
                node.getParentNode().removeChild(node);
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing: " + resource + "[" + rid + "]");
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
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            docBuilder =
                    docBuilderFactory.newDocumentBuilder();
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
            if (logger.isDebugEnabled()) {
                logger.debug("filterXML: pid = [" + pid + "]");
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
            if (logger.isDebugEnabled()) {
                logger.debug("No results to filter.");
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
            String resource = r.getResource();
            if (resource == null || resource.isEmpty()) {
                logger.warn("This resource has no resource identifier in the xacml response results!");
            } else if (logger.isDebugEnabled()) {
                logger.debug("Checking: " + r.getResource());
            }

            int lastSlash = resource.lastIndexOf('/');
            String rid = resource.substring(lastSlash + 1);

            if (r.getStatus().getCode().contains(Status.STATUS_OK)
                    && r.getDecision() != Result.DECISION_PERMIT) {
                Node node = dsids.get(rid);
                node.getParentNode().removeChild(node);
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing: " + resource + "[" + rid + "]");
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
        RequestCtx[] requests = new RequestCtx[dsids.size()];
        int ix = 0;
        for (String dsid : dsids) {
            logger.debug("Checking: {}/{}", pid, dsid);

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr;

            try {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.GET_DATASTREAM
                                    .getStringAttribute());

                resAttr = ResourceAttributes.getResources(pid);
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsid));

                RequestCtx req =
                        getContextHandler()
                                .buildRequest(getSubjects(request),
                                              actions,
                                              resAttr,
                                              getEnvironment(request));

                requests[ix++] = req;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ServletException(e.getMessage(), e);
            }
        }

        ResponseCtx resCtx = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Number of requests: " + requests.length);
            }

            resCtx =
                    getContextHandler().evaluateBatch(requests);

        } catch (MelcoeXacmlException pe) {
            throw new ServletException("Error evaluating pids: "
                    + pe.getMessage(), pe);
        }

        @SuppressWarnings("unchecked")
        Set<Result> results = resCtx.getResults();

        return results;
    }
}
