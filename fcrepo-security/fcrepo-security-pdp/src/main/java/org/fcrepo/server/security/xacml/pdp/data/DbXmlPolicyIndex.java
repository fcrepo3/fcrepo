/*
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

package org.fcrepo.server.security.xacml.pdp.data;

import java.io.File;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;

import com.sleepycat.dbxml.XmlDocument;
import com.sleepycat.dbxml.XmlDocumentConfig;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlQueryContext;
import com.sleepycat.dbxml.XmlQueryExpression;
import com.sleepycat.dbxml.XmlResults;
import com.sleepycat.dbxml.XmlTransaction;
import com.sleepycat.dbxml.XmlValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;
import org.fcrepo.server.security.xacml.util.AttributeBean;

/**
 * Encapsulates indexed access to policies stored in DbXml.
 *
 * See DbXmlPolicyStore for CRUD operations on policies in DbXml.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
class DbXmlPolicyIndex
        implements PolicyIndex {

    private static final Logger log =
            LoggerFactory.getLogger(DbXmlPolicyIndex.class.getName());

    private static final String XACML20_POLICY_NS =
            "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    private static final String METADATA_POLICY_NS = "metadata";

    private static final String XACML_RESOURCE_ID =
            "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    private DbXmlManager dbXmlManager = null;

    private long lastUpdate;

    private PolicyUtils utils;

    private  Map<String, XmlQueryExpression> queries = null;

    private  XmlQueryExpression[] searchQueries = null;

    protected DbXmlPolicyIndex()
            throws PolicyStoreException {
        init();

        queries = new HashMap<String, XmlQueryExpression>();

        searchQueries = new XmlQueryExpression[10];

    }

    private void init() throws PolicyStoreException {
        dbXmlManager = new DbXmlManager();

        if (dbXmlManager.validator != null ) {
            utils = new PolicyUtils(dbXmlManager.validator);
        } else {
            utils = new PolicyUtils();
        }

    }

    /*
     * (non-Javadoc)
     * @seemelcoe.xacml.pdp.data.PolicyDataManager#getPolicies(com.sun.xacml.
     * EvaluationCtx)
     */
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
            throws PolicyIndexException {
        long a = 0;
        long b = 0;
        long total = 0;

        Map<String, byte[]> documents = new HashMap<String, byte[]>();

        try {
            // Get the query (query gets prepared if necesary)
            a = System.nanoTime();

            Map<String, Set<AttributeBean>> attributeMap =
                    getAttributeMap(eval);

            XmlQueryContext context = dbXmlManager.manager.createQueryContext();
            context.setDefaultCollection(dbXmlManager.CONTAINER);
            context.setNamespace("p", XACML20_POLICY_NS);
            context.setNamespace("m", METADATA_POLICY_NS);

            // Set all the bind variables in the query context
            String[] types =
                    new String[] {"Subject", "Resource", "Action",
                            "Environment"};
            int resourceComponentCount = 0;

            for (String t : types) {
                int count = 0;
                for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                        + "Attributes")) {
                    if (bean.getId().equals(XACML_RESOURCE_ID)) {
                        context.setVariableValue("XacmlResourceId",
                                                 new XmlValue(bean.getId()));

                        int c = 0;
                        for (String value : bean.getValues()) {
                            XmlValue component = new XmlValue(value);
                            context
                                    .setVariableValue("XacmlResourceIdValue"
                                            + c, component);

                            if (log.isDebugEnabled()) {
                                log
                                        .debug("XacmlResourceIdValue"
                                                + resourceComponentCount + ": "
                                                + value);
                            }

                            resourceComponentCount++;
                            c++;
                        }
                    } else {
                        context.setVariableValue(t + "Id" + count,
                                                 new XmlValue(bean.getId()));

                        if (log.isDebugEnabled()) {
                            log.debug(t + "Id" + count + " = '" + bean.getId()
                                    + "'");
                        }

                        int valueCount = 0;
                        for (String value : bean.getValues()) {
                            context.setVariableValue(t + "Id" + count
                                                             + "-Value"
                                                             + valueCount,
                                                     new XmlValue(value));
                            if (log.isDebugEnabled()) {
                                log.debug(t + "Id" + count + "-Value"
                                        + valueCount + " = '" + value + "'");
                            }

                            valueCount++;
                        }

                        count++;
                    }
                }
            }

            XmlQueryExpression qe =
                    getQuery(attributeMap, context, resourceComponentCount);

            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Query prep. time: " + (b - a) + "ns");
            }

            // execute the query
            a = System.nanoTime();
            XmlResults results = qe.execute(context);
            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Query exec. time: " + (b - a) + "ns");
            }

            // process results
            while (results.hasNext()) {
                XmlValue value = results.next();
                if (log.isDebugEnabled()) {
                    log.debug("Retrieved Document: "
                            + value.asDocument().getName());
                }
                documents.put(value.asDocument().getName(), value.asDocument()
                        .getContent());
            }
            results.delete();

            if (log.isDebugEnabled()) {
                log.debug("Total exec. time: " + total + "ns");
            }
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error getting policies from PolicyDataManager.",
                                                 xe);
        } catch (URISyntaxException use) {
            throw new PolicyIndexException("Error building query.", use);
        }

        return documents;
    }

    /**
     * Either returns a query that has previously been generated, or generates a
     * new one if it has not.
     *
     * @param attributeMap
     *        the Map of attributes, type and values upon which this query is
     *        based
     * @param context
     *        the context for the query
     * @return an XmlQueryExpression that can be executed
     * @throws XmlException
     */
    private XmlQueryExpression getQuery(Map<String, Set<AttributeBean>> attributeMap,
                                        XmlQueryContext context,
                                        int r) throws XmlException {
        // The dimensions for this query.
        StringBuilder sb = new StringBuilder();
        for (Set<AttributeBean> attributeBeans : attributeMap.values()) {
            sb.append(attributeBeans.size() + ":");
            for (AttributeBean bean : attributeBeans) {
                sb.append(bean.getValues().size() + "-");
            }
        }

        // If a query of these dimensions already exists, then just return it.
        String hash = sb.toString() + r;
        XmlQueryExpression result = queries.get(hash);
        if (result != null) {
            return result;
        }

        // We do not have a query of those dimensions. We must make one.
        String query = createQuery(attributeMap, r);

        if (log.isDebugEnabled()) {
            log.debug("Query [" + hash + "]:\n" + query);
        }

        // Once we have created a query, we can parse it and store the
        // execution plan. This is an expensive operation that we do
        // not want to have to do more than once for each dimension
        result = dbXmlManager.manager.prepare(query, context);
        queries.put(hash, result);

        return result;
    }

    /**
     * Given a set of attributes this method generates a DBXML XPath query based
     * on those attributes to extract a subset of policies from the database.
     *
     * @param attributeMap
     *        the Map of Attributes from which to generate the query
     * @param r
     *        the number of components in the resource id
     * @return the query as a String
     */
    private String createQuery(Map<String, Set<AttributeBean>> attributeMap,
                               int r) {
        // The query contains these 4 sections.
        String[] types =
                new String[] {"Subject", "Resource", "Action", "Environment"};

        int sections = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("collection('" + dbXmlManager.CONTAINER + "')/p:Policy/p:Target[");
        for (String t : types) {
            if (attributeMap.get(t.toLowerCase() + "Attributes").size() == 0) {
                continue;
            }

            if (sections > 0) {
                sb.append(" and ");
            }

            sections++;

            sb.append("((exists(dbxml:metadata('m:any" + t + "')))");
            // sb.append("((not('p:" + t + "s'))");

            int count = 0;
            for (AttributeBean bean : attributeMap.get(t.toLowerCase()
                    + "Attributes")) {
                sb.append(" or ");
                sb.append("(");

                if (bean.getId().equals(XACML_RESOURCE_ID) && r > 0) {
                    sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                    sb.append("p:" + t + "AttributeDesignator/@AttributeId = ");
                    sb.append("$XacmlResourceId");
                    sb.append(" and ");

                    /*
                     * sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                     * sb.append("p:" + t + "AttributeDesignator/@DataType = ");
                     * sb.append("$XacmlResourceType"); sb.append(" and ");
                     */

                    sb.append("(");
                    for (int i = 0; i < bean.getValues().size(); i++) {
                        if (i > 0) {
                            sb.append(" or ");
                        }

                        sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                        sb.append("p:AttributeValue = ");
                        sb.append("$XacmlResourceIdValue" + i);
                    }
                    sb.append(")");
                } else {
                    sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                    sb.append("p:" + t + "AttributeDesignator/@AttributeId = ");
                    sb.append("$" + t + "Id" + count);
                    sb.append(" and ");
                    sb.append("(");
                    /*
                     * sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                     * sb.append("p:" + t + "AttributeDesignator/@DataType = ");
                     * sb.append("$" + t + "Type" + count); sb.append(" and ");
                     */

                    for (int valueCount = 0; valueCount < bean.getValues()
                            .size(); valueCount++) {
                        if (valueCount > 0) {
                            sb.append(" or ");
                        }

                        sb.append("p:" + t + "s/p:" + t + "/p:" + t + "Match/");
                        sb.append("p:AttributeValue = ");
                        sb.append("$" + t + "Id" + count + "-Value"
                                + valueCount);
                    }
                    sb.append(")");

                    count++;
                }
                sb.append(")");
            }
            sb.append(")");
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * This method extracts the attributes listed in the indexMap from the given
     * evaluation context.
     *
     * @param eval
     *        the Evaluation Context from which to extract Attributes
     * @return a Map of Attributes for each category (Subject, Resource, Action,
     *         Environment)
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    private Map<String, Set<AttributeBean>> getAttributeMap(EvaluationCtx eval)
            throws URISyntaxException {
        URI defaultCategoryURI =
                new URI(AttributeDesignator.SUBJECT_CATEGORY_DEFAULT);

        Map<String, String> im = null;
        Map<String, Set<AttributeBean>> attributeMap =
                new HashMap<String, Set<AttributeBean>>();
        Map<String, AttributeBean> attributeBeans = null;

        im = dbXmlManager.indexMap.get("subjectAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getSubjectAttribute(new URI(im.get(attributeId)),
                                             new URI(attributeId),
                                             defaultCategoryURI);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put("subjectAttributes", new HashSet(attributeBeans
                .values()));

        im = dbXmlManager.indexMap.get("resourceAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getResourceAttribute(new URI(im.get(attributeId)),
                                              new URI(attributeId),
                                              null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            if (attributeId.equals(XACML_RESOURCE_ID)
                                    && value.encode().startsWith("/")) {
                                String[] components =
                                        makeComponents(value.encode());
                                if (components != null && components.length > 0) {
                                    for (String c : components) {
                                        ab.addValue(c);
                                    }
                                } else {
                                    ab.addValue(value.encode());
                                }
                            } else {
                                ab.addValue(value.encode());
                            }
                        }
                    }
                }
            }
        }
        attributeMap.put("resourceAttributes", new HashSet(attributeBeans
                .values()));

        im = dbXmlManager.indexMap.get("actionAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            EvaluationResult result =
                    eval.getActionAttribute(new URI(im.get(attributeId)),
                                            new URI(attributeId),
                                            null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put("actionAttributes", new HashSet(attributeBeans
                .values()));

        im = dbXmlManager.indexMap.get("environmentAttributes");
        attributeBeans = new HashMap<String, AttributeBean>();
        for (String attributeId : im.keySet()) {
            URI imAttrId = new URI(im.get(attributeId));
            URI attrId = new URI(attributeId);
            EvaluationResult result =
                    eval.getEnvironmentAttribute(imAttrId, attrId, null);
            if (result.getStatus() == null && !result.indeterminate()) {
                AttributeValue attr = result.getAttributeValue();
                if (attr.returnsBag()) {
                    Iterator<AttributeValue> i =
                            ((BagAttribute) attr).iterator();
                    if (i.hasNext()) {
                        while (i.hasNext()) {
                            AttributeValue value = i.next();
                            String attributeType = im.get(attributeId);

                            AttributeBean ab = attributeBeans.get(attributeId);
                            if (ab == null) {
                                ab = new AttributeBean();
                                ab.setId(attributeId);
                                ab.setType(attributeType);
                                attributeBeans.put(attributeId, ab);
                            }

                            ab.addValue(value.encode());
                        }
                    }
                }
            }
        }
        attributeMap.put("environmentAttributes", new HashSet(attributeBeans
                .values()));

        return attributeMap;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataQuery#findPolicies
     * (org.fcrepo.server.security.xacml.util .AttributeBean[])
     */
    public Map<String, byte[]> findPolicies(AttributeBean[] attributes)
            throws PolicyIndexException {
        if (attributes == null || attributes.length == 0) {
            throw new PolicyIndexException("attribute array cannot be null or zero length");
        }

        long a, b, total = 0;
        Map<String, byte[]> documents = new TreeMap<String, byte[]>();
        try {
            a = System.nanoTime();

            XmlQueryContext context = dbXmlManager.manager.createQueryContext();
            context.setDefaultCollection(dbXmlManager.CONTAINER);
            context.setNamespace("p", XACML20_POLICY_NS);
            context.setNamespace("m", METADATA_POLICY_NS);

            for (int x = 0; attributes.length < 0; x++) {
                context.setVariableValue("id" + x, new XmlValue(attributes[x]
                        .getId()));
                // context.setVariableValue("type" + x, new
                // XmlValue(attributes[x].getType()));
                // context.setVariableValue("value" + x, new
                // XmlValue(attributes[x].getValue()));
            }

            if (searchQueries[attributes.length] == null) {
                StringBuilder sb = new StringBuilder();

                sb.append("for $doc in ");
                sb.append("collection('" + dbXmlManager.CONTAINER + "') ");
                sb.append("let $value := $doc//p:AttributeValue ");
                sb.append("let $id := $value/..//@AttributeId ");
                sb.append("where 1 = 1 ");
                for (int x = 0; x < attributes.length; x++) {
                    sb.append("and $value = $value" + x + " ");
                    sb.append("and $id = $id" + x + " ");
                }
                sb.append("return $doc");

                searchQueries[attributes.length] =
                    dbXmlManager.manager.prepare(sb.toString(), context);
            }

            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Query prep. time: " + (b - a) + "ns");
            }

            a = System.nanoTime();
            XmlResults results =
                    searchQueries[attributes.length].execute(context);
            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Search exec. time: " + (b - a) + "ns");
            }

            a = System.nanoTime();

            while (results.hasNext()) {
                XmlValue value = results.next();
                if (log.isDebugEnabled()) {
                    log.debug("Found search result: "
                            + value.asDocument().getName());
                }
                documents.put(value.asDocument().getName(), value.asDocument()
                        .getContent());
            }
            results.delete();

            b = System.nanoTime();
            total += b - a;
            if (log.isDebugEnabled()) {
                log.debug("Result proc. time: " + (b - a) + "ns");
            }

            log.info("Total time: " + total + "ns");
        } catch (XmlException xe) {
            log.error("Exception during findPolicies: " + xe.getMessage(), xe);
            throw new PolicyIndexException("Exception during findPolicies: "
                                                         + xe.getMessage(),
                                                 xe);
        }

        return documents;
    }

    private String[] makeComponents(String resourceId) {
        if (resourceId == null || resourceId.equals("")
                || !resourceId.startsWith("/")) {
            return null;
        }

        List<String> components = new ArrayList<String>();

        String[] parts = resourceId.split("\\/");

        for (int x = 1; x < parts.length; x++) {
            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < x; y++) {
                sb.append("/");
                sb.append(parts[y + 1]);
            }

            components.add(sb.toString());

            if (x != parts.length - 1) {
                components.add(sb.toString() + "/.*");
            } else {
                components.add(sb.toString() + "$");
            }
        }

        return components.toArray(new String[components.size()]);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.io.File)
     */
    public String addPolicy(File f) throws PolicyIndexException {
        return addPolicy(f, null);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.io.File, java.lang.String)
     */
    public String addPolicy(File f, String name)
            throws PolicyIndexException {
        try {
            return addPolicy(utils.fileToString(f), name);
        } catch (MelcoePDPException e) {
            throw new PolicyIndexException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.lang.String)
     */
    public String addPolicy(String document) throws PolicyIndexException {
        return addPolicy(document, null);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.lang.String, java.lang.String)
     */
    public String addPolicy(String document, String name)
            throws PolicyIndexException {


        try {
            utils.validate(document, name);
        } catch (MelcoePDPException e) {
            throw new PolicyIndexException(e);
        }

        String docName = null;
        XmlTransaction txn = null;
        try {
            try {
                XmlDocument doc = makeDocument(name, document);
                docName = doc.getName();
                log.debug("Adding document: " + docName);
                txn = dbXmlManager.manager.createTransaction();
                dbXmlManager.container.putDocument(txn, doc, dbXmlManager.updateContext);
                txn.commit();
                setLastUpdate(System.currentTimeMillis());
            } catch (XmlException xe) {
                if (xe.getErrorCode() == XmlException.UNIQUE_ERROR) {
                    throw new PolicyIndexException("Document already exists: "
                            + docName);
                }
                txn.abort();
                throw new PolicyIndexException("Error adding policy: "
                        + xe.getMessage(), xe);
            }
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return docName;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#deletePolicy
     * (java.lang.String)
     */
    public boolean deletePolicy(String name) throws PolicyIndexException {
        log.debug("Deleting document: " + name);
        XmlTransaction txn = null;
        try {
            try {
                txn = dbXmlManager.manager.createTransaction();
                dbXmlManager.container.deleteDocument(txn, name, dbXmlManager.updateContext);
                txn.commit();
                setLastUpdate(System.currentTimeMillis());
            } catch (XmlException xe) {
                txn.abort();
                throw new PolicyIndexException("Error deleting document: "
                                                             + name,
                                                     xe);
            }
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#updatePolicy
     * (java.lang.String, java.lang.String)
     */
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyIndexException {
        log.debug("Updating document: " + name);

        // FIXME:  DBXML container.updateDocument is failing to update document metadata (this tested on DBXML ver 2.5.13)
        // specifically anySubject, anyResource metadata elements are not changing
        // if Subjects and Resources elements are added/deleted from document.
        // So do a delete then an add

        deletePolicy(name);
        addPolicy(newDocument, name);

        /* original code below works apart from metadata update not happening
        try {
            utils.validate(newDocument, name);
        } catch (MelcoePDPException e) {
            throw new PolicyIndexException(e);
        }

        XmlTransaction txn = null;

        try {
            try {
                txn = dbXmlManager.manager.createTransaction();
                XmlDocument doc = makeDocument(name, newDocument);
                dbXmlManager.container.updateDocument(txn, doc, dbXmlManager.updateContext);
                txn.commit();
                setLastUpdate(System.currentTimeMillis());
            } catch (XmlException xe) {
                txn.abort();
                throw new PolicyIndexException("Error updating document: "
                                                             + name,
                                                     xe);
            }
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }
        */

        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getPolicy
     * (java.lang.String)
     */
    public byte[] getPolicy(String name) throws PolicyIndexException {
        log.debug("Getting document: " + name);
        try {
            XmlDocument doc = dbXmlManager.container.getDocument(name);
            return doc.getContent();
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error retrieving document: "
                    + name, xe);
        }
    }

    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policyName
     * @return true iff the policy store contains a policy identified as
     *         policyName
     * @throws PolicyStoreException
     */
    public boolean contains(String policyName)
            throws PolicyIndexException {
        log.debug("Determining if document exists: " + policyName);
        try {
            dbXmlManager.container.getDocument(policyName, new XmlDocumentConfig()
                    .setLazyDocs(true));
        } catch (XmlException e) {
            if (e.getErrorCode() == XmlException.DOCUMENT_NOT_FOUND) {
                return false;
            } else {
                throw new PolicyIndexException(e.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Check if the policy identified by policyName exists.
     *
     * @param policy
     * @return true iff the policy store contains a policy with the same
     *         PolicyId
     * @throws PolicyStoreException
     */
    public boolean contains(File policy) throws PolicyIndexException {
        try {
            return contains(utils.getPolicyName(policy));
        } catch (MelcoePDPException e) {
            throw new PolicyIndexException(e);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#listPolicies
     * ()
     */
    public List<String> listPolicies() throws PolicyIndexException {
        log.debug("Listing policies");
        List<String> documents = new ArrayList<String>();
        XmlTransaction txn = null;

        try {
            try {
                txn = dbXmlManager.manager.createTransaction();
                XmlDocumentConfig docConf = new XmlDocumentConfig();
                XmlResults results = dbXmlManager.container.getAllDocuments(txn, docConf);
                while (results.hasNext()) {
                    XmlValue value = results.next();
                    documents.add(value.asDocument().getName());
                }
                results.delete();
                txn.commit();
            } catch (XmlException xe) {
                txn.abort();
                throw new PolicyIndexException(xe);
            }
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error aborting transaction: "
                    + xe.getMessage(), xe);
        }

        return documents;
    }



    /**
     * Creates an instance of an XmlDocument for storage in the database.
     *
     * @param name
     *        the name of the document (policy)
     * @param document
     *        the document data as a String
     * @return the XmlDocument instance
     * @throws XmlException
     * @throws PolicyStoreException
     */
    private XmlDocument makeDocument(String name, String document)
            throws XmlException, PolicyIndexException {
        Map<String, String> metadata =
                utils.getDocumentMetadata(document
                        .getBytes());
        XmlDocument doc = dbXmlManager.manager.createDocument();
        String docName = name;

        if (docName == null || "".equals(docName)) {
            docName = metadata.get("PolicyId");
        }

        if (docName == null || "".equals(docName)) {
            throw new PolicyIndexException("Could not extract PolicyID from document.");
        }

        doc.setMetaData("metadata", "PolicyId", new XmlValue(XmlValue.STRING,
                                                             docName));
        doc.setContent(document);
        doc.setName(docName);

        String item = null;
        item = metadata.get("anySubject");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anySubject",
                            new XmlValue(XmlValue.STRING, item));
        }

        item = metadata.get("anyResource");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anyResource",
                            new XmlValue(XmlValue.STRING, item));
        }

        item = metadata.get("anyAction");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anyAction",
                            new XmlValue(XmlValue.STRING, item));
        }

        item = metadata.get("anyEnvironment");
        if (item != null) {
            doc.setMetaData("metadata",
                            "anyEnvironment",
                            new XmlValue(XmlValue.STRING, item));
        }

        return doc;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#getLastUpdate
     * ()
     */
    public long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate
     *        the lastUpdate to set
     */
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean clear() throws PolicyIndexException {

        boolean res = false;

        // get database location
        String dbDir = dbXmlManager.DB_HOME;
        // close the existing manager
        dbXmlManager.close();
        dbXmlManager = null;

        // clear database dir
        res = deleteDirectory(dbDir);

        // and init will create a new database (by creating a new dbXmlManager)
        try {
            init();
        } catch (PolicyStoreException e) {
            throw new PolicyIndexException(e);
        }
        return res;

    }

    private boolean deleteDirectory(String directory) {

        boolean result = false;

        if (directory != null) {
            File file = new File(directory);
            if (file.exists() && file.isDirectory()) {
                // 1. delete content of directory:
                File[] files = file.listFiles();
                result = true; //init result flag
                int count = files.length;
                for (int i = 0; i < count; i++) { //for each file:
                    File f = files[i];
                    if (f.isFile()) {
                        result = result && f.delete();
                    } else if (f.isDirectory()) {
                        result = result && deleteDirectory(f.getAbsolutePath());
                    }
                }//next file

                file.delete(); //finally delete (empty) input directory
            }//else: input directory does not exist or is not a directory
        }//else: no input value

        return result;
    }//deleteDirectory()

}
