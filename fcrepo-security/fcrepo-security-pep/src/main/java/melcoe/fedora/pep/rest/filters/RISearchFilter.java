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

package melcoe.fedora.pep.rest.filters;

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

import melcoe.fedora.pep.PEPException;
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
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.Attribute;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.ctx.Status;

import fedora.common.Constants;

/**
 * Handles the risearch operation.
 * 
 * @author nishen@melcoe.mq.edu.au
 */
public class RISearchFilter
        extends AbstractFilter {

    private static Logger log =
            Logger.getLogger(RISearchFilter.class.getName());

    private static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    private Map<String, Transformer> transformers = null;

    private Map<String, String> mimeType = null;

    private ContextUtil contextUtil = null;

    private Tidy tidy = null;

    /**
     * Default constructor.
     * 
     * @throws PEPException
     */
    public RISearchFilter()
            throws PEPException {
        super();
        contextUtil = new ContextUtil();

        tidy = new Tidy();
        tidy.setShowWarnings(false);
        tidy.setQuiet(true);

        transformers = new HashMap<String, Transformer>();
        mimeType = new HashMap<String, String>();
        TransformerFactory xFormerFactory = TransformerFactory.newInstance();

        try {
            Transformer transformer = xFormerFactory.newTransformer();
            transformers.put("RDF/XML", transformer);
            mimeType.put("RDF/XML", "text/xml");
        } catch (TransformerConfigurationException tce) {
            log.warn("Error loading the rdfxml2nTriples.xsl stylesheet", tce);
            throw new PEPException("Error loading the rdfxml2nTriples.xsl stylesheet",
                                   tce);
        }

        try {
            String stylesheetLocation =
                    "melcoe/fedora/pep/rest/filters/rdfxml2nTriples.xsl";
            InputStream stylesheet =
                    this.getClass().getClassLoader()
                            .getResourceAsStream(stylesheetLocation);
            if (stylesheet == null) {
                throw new FileNotFoundException("Could not find file: rdfxml2nTriples.xsl");
            }

            Transformer transformer =
                    xFormerFactory.newTransformer(new StreamSource(stylesheet));
            transformers.put("N-Triples", transformer);
            mimeType.put("N-Triples", "text/plain");
        } catch (TransformerConfigurationException tce) {
            log.warn("Error loading the rdfxml2n3.xsl stylesheet", tce);
            throw new PEPException("Error loading the rdfxml2n3.xsl stylesheet",
                                   tce);
        } catch (FileNotFoundException fnfe) {
            log.warn(fnfe.getMessage());
            throw new PEPException(fnfe.getMessage());
        }
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
     * melcoe.fedora.pep.rest.filters.RESTFilter#handleResponse(javax.servlet
     * .http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
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
            if (log.isDebugEnabled()) {
                log.debug("RISearchIndexFilter PID: " + pid);
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
                    log
                            .warn("This resource has no resource identifier in the xacml response results!");
                } else if (log.isDebugEnabled()) {
                    log.debug("Checking: " + rid);
                }

                if (r.getStatus().getCode().contains(Status.STATUS_OK)
                        && r.getDecision() != Result.DECISION_PERMIT) {
                    List<String> pids = resultMap.get(rid);
                    for (String pid : pids) {
                        List<Node> nodeList = nodeMap.get(pid);

                        for (Node node : nodeList) {
                            if (node != null && node.getParentNode() != null) {
                                node.getParentNode().removeChild(node);
                                if (log.isDebugEnabled()) {
                                    log.debug("Removing: " + pid + " [" + rid
                                            + "]");
                                }
                            } else {
                                log.warn("Could not locate and/or remove: "
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
        if (log.isDebugEnabled()) {
            os = new ByteArrayOutputStream();
            dst = new StreamResult(os);

            Transformer xFormer = transformers.get("RDF/XML");
            try {
                xFormer.transform(src, dst);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            log.debug("RDF/XML:\n" + new String(os.toByteArray()));
        }

        os = new ByteArrayOutputStream();
        dst = new StreamResult(os);
        try {
            if (log.isDebugEnabled()) {
                log.debug("Transforming format: " + format);
            }
            Transformer xFormer = transformers.get(format);
            xFormer.transform(src, dst);
        } catch (TransformerException te) {
            throw new ServletException("error generating output", te);
        }

        if (log.isDebugEnabled()) {
            log.debug("RDF/XML:\n" + new String(os.toByteArray()));
        }

        res.setData(os.toByteArray());
        res.setContentType(mimeType.get(format));

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
            if (log.isDebugEnabled()) {
                log.debug("Checking: " + pidDN);
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
                        contextUtil.buildRequest(getSubjects(request),
                                                 actions,
                                                 resAttr,
                                                 getEnvironment(request));

                String xacmlResourceId = getXacmlResourceId(req);
                if (xacmlResourceId != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Extracted XacmlResourceId: "
                                + xacmlResourceId);
                    }

                    List<String> resultPid = resultMap.get(xacmlResourceId);
                    if (resultPid == null) {
                        resultPid = new ArrayList<String>();
                        resultMap.put(xacmlResourceId, resultPid);
                    }
                    resultPid.add(pidDN);
                }

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

    private String getXacmlResourceId(RequestCtx req) {
        @SuppressWarnings("unchecked")
        Set<Attribute> attributes = req.getResource();

        for (Attribute attr : attributes) {
            if (log.isDebugEnabled()) {
                log.debug("Attribute: " + attr.getId().toString());
            }
            if (attr.getId().toString().equals(XACML_RESOURCE_ID)) {
                return attr.getValue().encode();
            }
        }

        return null;
    }
}
