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

import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.xacml.EvaluationCtx;

import com.sleepycat.dbxml.XmlDocument;
import com.sleepycat.dbxml.XmlDocumentConfig;
import com.sleepycat.dbxml.XmlException;
import com.sleepycat.dbxml.XmlQueryContext;
import com.sleepycat.dbxml.XmlQueryExpression;
import com.sleepycat.dbxml.XmlResults;
import com.sleepycat.dbxml.XmlValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.security.xacml.util.AttributeBean;

/**
 * Encapsulates indexed access to policies stored in DbXml.
 *
 * See DbXmlPolicyStore for CRUD operations on policies in DbXml.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class DbXmlPolicyIndex
        extends XPathPolicyIndex
        implements PolicyIndex {

    private static final Logger log =
            LoggerFactory.getLogger(DbXmlPolicyIndex.class.getName());

    private DbXmlManager dbXmlManager = null;

    private volatile long lastUpdate;

    private PolicyUtils utils;

    private  Map<String, XmlQueryExpression> queries = null;


    protected DbXmlPolicyIndex()
            throws PolicyIndexException {
        super();
        init();

        queries = new ConcurrentHashMap<String, XmlQueryExpression>();
    }

    private void init() throws PolicyIndexException {
        try {
            dbXmlManager = new DbXmlManager();
        } catch (PolicyStoreException e) {
            throw new PolicyIndexException("Error initialising DbXmlManager - " + e.getMessage(), e);
        }
        dbXmlManager.indexMap = indexMap;

        utils = new PolicyUtils();

    }

    /*
     * (non-Javadoc)
     * @seemelcoe.xacml.pdp.data.PolicyDataManager#getPolicies(com.sun.xacml.
     * EvaluationCtx)
     */
    @Override
    public Map<String, byte[]> getPolicies(EvaluationCtx eval)
            throws PolicyIndexException {
        long a = 0;
        long b = 0;
        long total = 0;

        Map<String, byte[]> documents = new HashMap<String, byte[]>();

        XmlQueryExpression qe = null;
        XmlQueryContext context = null;

        try {
            // Get the query (query gets prepared if necesary)
            a = System.nanoTime();

            Map<String, Set<AttributeBean>> attributeMap =
                getAttributeMap(eval);

            context = dbXmlManager.manager.createQueryContext();
            context.setDefaultCollection(dbXmlManager.CONTAINER);

            for (String prefix : namespaces.keySet()) {
                context.setNamespace(prefix, namespaces.get(prefix));
            }


            // not clear why this is needed.... but it is used in hashing the queries
            int resourceComponentCount = 0;
            Map<String, String> variables = getXpathVariables(attributeMap);
            for (String variable : variables.keySet()) {
                context.setVariableValue(variable, new XmlValue(variables.get(variable)));
                if (variable.equals(XACML_RESOURCE_ID)) {
                    resourceComponentCount++;
                }
            }


            qe =
                getQuery(attributeMap, context, resourceComponentCount);


        } catch (XmlException xe) {
            throw new PolicyIndexException("Error while constructing query", xe);
        } catch (URISyntaxException e) {
            throw new PolicyIndexException("Error while constructing query", e);
        }

        DbXmlManager.readLock.lock();
        try {
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
                byte[] content = value.asDocument().getContent();
                if (content.length > 0) {
                    documents.put(value.asDocument().getName(), content);
                } else {
                    throw new PolicyIndexException("Zero-length result found");
                }
            }
            results.delete();
        } catch (XmlException xe) {
            log.error("Error getting query results." + xe.getMessage());
            throw new PolicyIndexException("Error getting query results." + xe.getMessage(), xe);

        } finally {
            DbXmlManager.readLock.unlock();
        }

        if (log.isDebugEnabled()) {
            log.debug("Total exec. time: " + total + "ns");
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
     * @throws XmlException
     * @throws PolicyIndexException
     */
    private XmlQueryExpression getQuery(Map<String, Set<AttributeBean>> attributeMap,
                                        XmlQueryContext context,
                                        int r) throws XmlException  {
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
        return "collection('" + dbXmlManager.CONTAINER + "')" + getXpath(attributeMap, r);

    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#addPolicy
     * (java.lang.String, java.lang.String)
     */
    @Override
    public String addPolicy(String name, String document)
            throws PolicyIndexException {

        String docName = null;
        DbXmlManager.writeLock.lock();
        try {

            XmlDocument doc = makeDocument(name, document);
            docName = doc.getName();
            log.debug("Adding document: " + docName);
            dbXmlManager.container.putDocument(doc,
                                               dbXmlManager.updateContext);
            setLastUpdate(System.currentTimeMillis());
        } catch (XmlException xe) {
            if (xe.getErrorCode() == XmlException.UNIQUE_ERROR) {
                throw new PolicyIndexException("Document already exists: "
                                               + docName);
            } else {
                throw new PolicyIndexException("Error adding policy: "
                                               + xe.getMessage(), xe);
            }
        } finally {
            DbXmlManager.writeLock.unlock();
        }

        return docName;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#deletePolicy
     * (java.lang.String)
     */
    @Override
    public boolean deletePolicy(String name) throws PolicyIndexException {
        log.debug("Deleting document: " + name);

        DbXmlManager.writeLock.lock();
        try {
            dbXmlManager.container.deleteDocument(name, dbXmlManager.updateContext);
            setLastUpdate(System.currentTimeMillis());
        } catch (XmlException xe) {
            // safe delete - only warn if not found
            if (xe.getDbError() == XmlException.DOCUMENT_NOT_FOUND){
                log.warn("Error deleting document: " + name + " - document does not exist");
            } else {
                throw new PolicyIndexException("Error deleting document: " + name + xe.getMessage(), xe);
            }
        } finally {
            DbXmlManager.writeLock.unlock();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.fcrepo.server.security.xacml.pdp.data.PolicyDataManager#updatePolicy
     * (java.lang.String, java.lang.String)
     */
    @Override
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyIndexException {
        log.debug("Updating document: " + name);

        // FIXME:  DBXML container.updateDocument is failing to update document metadata (this tested on DBXML ver 2.5.13)
        // specifically anySubject, anyResource metadata elements are not changing
        // if Subjects and Resources elements are added/deleted from document.


        // FIXME: this will acquire and release write locks for each operation
        // should instead do this just once for an update
        deletePolicy(name);
        addPolicy(name, newDocument);

        // FIXME:  code below would also need updating for transactions, deadlocks, single thread updates...

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
    @Override
    public byte[] getPolicy(String name) throws PolicyIndexException {
        log.debug("Getting document: " + name);
        XmlDocument doc = null;
        DbXmlManager.readLock.lock();
        try {
            doc = dbXmlManager.container.getDocument(name);
            return doc.getContent();
        } catch (XmlException xe) {
            throw new PolicyIndexException("Error getting Policy: " + name + xe.getMessage()  + " - " + xe.getDatabaseException().getMessage(), xe);
        } finally {
            DbXmlManager.readLock.unlock();
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
    @Override
    public boolean contains(String policyName)
            throws PolicyIndexException {
        log.debug("Determining if document exists: " + policyName);
        DbXmlManager.readLock.lock();
        try {
            dbXmlManager.container.getDocument(policyName,
                                               new XmlDocumentConfig().setLazyDocs(true));
        } catch (XmlException e) {
            if (e.getErrorCode() == XmlException.DOCUMENT_NOT_FOUND) {
                return false;
            } else {
                throw new PolicyIndexException("Error executing contains. " + e.getMessage(), e);
            }
        } finally {
            DbXmlManager.readLock.unlock();
        }

        return true;
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
        init();
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
