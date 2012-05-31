/*
 * File: RISearchFilter.java
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

package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import javax.xml.transform.stream.StreamSource;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.fcrepo.server.security.xacml.util.RelationshipResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;


/**
 * Handles the risearch operation.
 *
 * @author nishen@melcoe.mq.edu.au
 */
public class RISearchFilter
        extends AbstractFilter implements ResponseHandlingRESTFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(RISearchFilter.class);

    private static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    private Map<String, Transformer> m_transformers = null;

    private Map<String, String> m_mimeType = null;

    private ContextUtil m_contextUtil = null;

    private Tidy m_tidy = null;

    private RelationshipResolver m_relationshipResolver;

    /**
     * Default constructor.
     *
     * @throws PEPException
     */
    public RISearchFilter()
            throws PEPException {
        super();

        m_tidy = new Tidy();
        m_tidy.setShowWarnings(false);
        m_tidy.setQuiet(true);

        m_transformers = new HashMap<String, Transformer>();
        m_mimeType = new HashMap<String, String>();
        TransformerFactory xFormerFactory = TransformerFactory.newInstance();

        try {
            Transformer transformer = xFormerFactory.newTransformer();
            m_transformers.put("RDF/XML", transformer);
            m_mimeType.put("RDF/XML", "text/xml");
        } catch (TransformerConfigurationException tce) {
            logger.warn("Error loading the rdfxml2nTriples.xsl stylesheet", tce);
            throw new PEPException("Error loading the rdfxml2nTriples.xsl stylesheet",
                                   tce);
        }

        try {
            String stylesheetLocation =
                    "org/fcrepo/server/security/xacml/pep/rest/filters/rdfxml2nTriples.xsl";
            InputStream stylesheet =
                    this.getClass().getClassLoader()
                            .getResourceAsStream(stylesheetLocation);
            if (stylesheet == null) {
                throw new FileNotFoundException("Could not find file: rdfxml2nTriples.xsl");
            }

            Transformer transformer =
                    xFormerFactory.newTransformer(new StreamSource(stylesheet));
            m_transformers.put("N-Triples", transformer);
            m_mimeType.put("N-Triples", "text/plain");
        } catch (TransformerConfigurationException tce) {
            logger.warn("Error loading the rdfxml2n3.xsl stylesheet", tce);
            throw new PEPException("Error loading the rdfxml2n3.xsl stylesheet",
                                   tce);
        } catch (FileNotFoundException fnfe) {
            logger.warn(fnfe.getMessage());
            throw new PEPException(fnfe.getMessage());
        }
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
        LogUtil.statLog(request.getRemoteUser(),
                        Constants.ACTION.RI_FIND_OBJECTS.getURI()
                                .toASCIIString(),
                        "FedoraResposoty:ResourceIndex",
                        null);

        return null;
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
        ParameterRequestWrapper req = (ParameterRequestWrapper) request;
        DataResponseWrapper res = (DataResponseWrapper) response;

        String body = new String(res.getData());

        if (body.startsWith("<html>")) {
            return null;
        }

        DocumentBuilder docBuilder = null;
        Document doc = null;

        try {
            docBuilder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.parse(new ByteArrayInputStream(res.getData()));
        } catch (Exception e) {
            throw new ServletException(e);
        }

        Map<String, List<Node>> nodeMap = new HashMap<String, List<Node>>();

        NodeList nodes = doc.getElementsByTagName("rdf:Description");
        for (int x = 0; x < nodes.getLength(); x++) {
            String pid =
                    nodes.item(x).getAttributes().getNamedItem("rdf:about")
                            .getNodeValue();
            if (logger.isDebugEnabled()) {
                logger.debug("RISearchIndexFilter PID: " + pid);
            }

            List<Node> nodeList = nodeMap.get(pid);
            if (nodeList == null) {
                nodeList = new ArrayList<Node>();
                nodeMap.put(pid, nodeList);
            }

            nodeList.add(nodes.item(x));
        }

        if (nodeMap.keySet().size() > 0) {
            Map<String, List<String>> resultMap =
                    new HashMap<String, List<String>>();
            Set<Result> results =
                    evaluatePids(nodeMap.keySet(), resultMap, request, res);

            for (Result r : results) {
                String rid = r.getResource();
                if (rid == null || "".equals(rid)) {
                    logger.warn("This resource has no resource identifier in the xacml response results!");
                } else {
                    logger.debug("Checking: {}", rid);
                }

                if (r.getStatus().getCode().contains(Status.STATUS_OK)
                        && r.getDecision() != Result.DECISION_PERMIT) {
                    List<String> pids = resultMap.get(rid);
                    for (String pid : pids) {
                        List<Node> nodeList = nodeMap.get(pid);

                        for (Node node : nodeList) {
                            if (node != null && node.getParentNode() != null) {
                                node.getParentNode().removeChild(node);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Removing: " + pid + " [" + rid
                                            + "]");
                                }
                            } else {
                                logger.warn("Could not locate and/or remove: "
                                        + pid + " [" + rid + "]");
                            }
                        }
                    }
                }
            }
        }

        String[] formats = req.getFormat();
        String format = null;
        if (formats != null
                && formats.length > 0
                && ("RDF/XML".equals(formats[0]) || "N-Triples"
                        .equals(formats[0]))) {
            format = formats[0];
        } else {
            format = "RDF/XML";
        }

        Source src = new DOMSource(doc);
        ByteArrayOutputStream os = null;
        new ByteArrayOutputStream();
        javax.xml.transform.Result dst = null;
        new StreamResult(os);
        if (logger.isDebugEnabled()) {
            os = new ByteArrayOutputStream();
            dst = new StreamResult(os);

            Transformer xFormer = m_transformers.get("RDF/XML");
            try {
                xFormer.transform(src, dst);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            logger.debug("RDF/XML:\n" + new String(os.toByteArray()));
        }

        os = new ByteArrayOutputStream();
        dst = new StreamResult(os);
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Transforming format: " + format);
            }
            Transformer xFormer = m_transformers.get(format);
            xFormer.transform(src, dst);
        } catch (TransformerException te) {
            throw new ServletException("error generating output", te);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("RDF/XML:\n" + new String(os.toByteArray()));
        }

        res.setData(os.toByteArray());
        res.setContentType(m_mimeType.get(format));

        return null;
    }

    /**
     * Takes a given list of PID's and evaluates them.
     *
     * @param pids
     *        the list of pids to check
     * @param resultMap
     *        the map of pid's to rest based pids from contextUtil
     * @param request
     *        the http servlet request
     * @param response
     *        the http servlet resposne
     * @return a set of XACML results
     * @throws ServletException
     */
    private Set<Result> evaluatePids(Set<String> pids,
                                     Map<String, List<String>> resultMap,
                                     HttpServletRequest request,
                                     DataResponseWrapper response)
            throws ServletException {
        Set<String> requests = new HashSet<String>();
        for (String pidDN : pids) {
            if (logger.isDebugEnabled()) {
                logger.debug("Checking: " + pidDN);
            }

            Map<URI, AttributeValue> actions =
                    new HashMap<URI, AttributeValue>();
            Map<URI, AttributeValue> resAttr =
                    new HashMap<URI, AttributeValue>();

            String[] components = pidDN.split("\\/");
            String pid = components[1];
            String dsID = null;

            if (components.length == 3) {
                dsID = components[2];
            }

            try {
                actions
                        .put(Constants.ACTION.ID.getURI(),
                             new StringAttribute(Constants.ACTION.LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS
                                     .getURI().toASCIIString()));

                // Modification to uniquely identify datastreams

                if (pid != null && !"".equals(pid)) {
                    resAttr.put(Constants.OBJECT.PID.getURI(),
                                new StringAttribute(pid));
                }
                if (pid != null && !"".equals(pid)) {
                    resAttr.put(new URI(XACML_RESOURCE_ID),
                                new AnyURIAttribute(new URI(pid)));
                }
                if (dsID != null && !"".equals(dsID)) {
                    resAttr.put(Constants.DATASTREAM.ID.getURI(),
                                new StringAttribute(dsID));
                }

                RequestCtx req =
                        m_contextUtil.buildRequest(getSubjects(request),
                                                 actions,
                                                 resAttr,
                                                 getEnvironment(request),
                                                 m_relationshipResolver);

                String xacmlResourceId = getXacmlResourceId(req);
                if (xacmlResourceId != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Extracted XacmlResourceId: "
                                + xacmlResourceId);
                    }

                    List<String> resultPid = resultMap.get(xacmlResourceId);
                    if (resultPid == null) {
                        resultPid = new ArrayList<String>();
                        resultMap.put(xacmlResourceId, resultPid);
                    }
                    resultPid.add(pidDN);
                }

                String r = m_contextUtil.makeRequestCtx(req);
                if (logger.isDebugEnabled()) {
                    logger.debug(r);
                }

                requests.add(r);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ServletException(e.getMessage(), e);
            }
        }

        String res = null;
        ResponseCtx resCtx = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Number of requests: " + requests.size());
            }

            res =
                    getContextHandler().evaluateBatch(requests
                            .toArray(new String[requests.size()]));

            if (logger.isDebugEnabled()) {
                logger.debug("Response: " + res);
            }

            resCtx = m_contextUtil.makeResponseCtx(res);
        } catch (MelcoeXacmlException pe) {
            throw new ServletException("Error evaluating pids: "
                    + pe.getMessage(), pe);
        }

        @SuppressWarnings("unchecked")
        Set<Result> results = resCtx.getResults();

        return results;
    }

    private String getXacmlResourceId(RequestCtx req) {
        @SuppressWarnings("unchecked")
        Set<Attribute> attributes = req.getResource();

        for (Attribute attr : attributes) {
            if (logger.isDebugEnabled()) {
                logger.debug("Attribute: " + attr.getId().toString());
            }
            if (attr.getId().toString().equals(XACML_RESOURCE_ID)) {
                return attr.getValue().encode();
            }
        }

        return null;
    }
}
