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

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.fcrepo.server.security.xacml.util.AttributeBean;


/**
 * This is a wrapper class for a PolicyStore class that exposes the
 * management interface as a web service. The wrapper was needed as WSDL does
 * not support classes that container function overloading.
 *
 * @author nishen@melcoe.mq.edu.au
 * @see PolicyStore
 */
// FIXME: not publicly exposed in FeSL
public class PolicyStoreService {

    private PolicyStore policyStorer = null;
    // FIXME: not currently initialised (just fix compilation errors) - only required by lastUpdate, which itself is probably not required
    private final PolicyIndex policyIndex = null;


    public PolicyStoreService()
            throws PolicyStoreException {
        PolicyStoreFactory factory = new PolicyStoreFactory();
        setPolicyStore(factory.newPolicyStore());
    }

    /**
     * Retrieves the document of the given name from the document store and
     * returns it as an array of bytes.
     *
     * @param name
     *        the document name to return
     * @return the document as a byte array
     * @throws PolicyStoreException
     */
    public DocumentInfo getPolicy(String name)
            throws PolicyStoreException {
        byte[] documentData = policyStorer.getPolicy(name);
        if (documentData == null) {
            return null;
        }

        String data = null;
        try {
            data = new String(documentData, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new PolicyStoreException(uee.getMessage(), uee);
        }

        DocumentInfo docInfo = new DocumentInfo(name, data);
        return docInfo;
    }

    /**
     * Adds a policy document to the document store.
     *
     * @param document
     *        the document to add as a String
     * @param name
     *        the name of the document to add. This can be null for extracting
     *        the name from the policy itself.
     * @return the name of the document that was added.
     * @throws PolicyStoreException
     */
    public String addPolicy(String document, String name)
            throws PolicyStoreException {
        return policyStorer.addPolicy(document, name);
    }

    /**
     * Removes the document of the given name from the document store.
     *
     * @param name
     *        the name of the document to remove.
     * @throws PolicyStoreException
     */
    public boolean deletePolicy(String name) throws PolicyStoreException {
        return policyStorer.deletePolicy(name);
    }

    /**
     * Updates a document of the given name by replacing it with the new
     * document provided as a String.
     *
     * @param name
     *        the name of the document to update.
     * @param newDocument
     *        the new document as a String.
     * @throws PolicyStoreException
     */
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyStoreException {
        return policyStorer.updatePolicy(name, newDocument);
    }

    /**
     * Lists all documents in the document store.
     *
     * @return array of Strings that are the names of all stored documents.
     * @throws PolicyStoreException
     */
    public String[] listPolicies() throws PolicyStoreException {
        List<String> result = policyStorer.listPolicies();
        return result.toArray(new String[result.size()]);
    }

    /**
     * Get the time of the latest add/update/delete operation from the document
     * store in milliseconds. The time is based on the document store servers
     * unix epoch time.
     *
     * @return the time the document store was last updated.
     */
    public long lastUpdate() {
        //return policyDataManager.getLastUpdate();
        return 0;  // FIXME:
    }

    /**
     * Search for policies that contain all the attribute id/value pairs in the
     * array of AttributeBeans.
     *
     * @param attributes
     *        Array of AttributeBeans that contain the attributes for which to
     *        search for.
     * @return array of DocumentInfo objects. Each contains the document name
     *         and the byte[] data.
     * @throws PolicyStoreException
     */
    @SuppressWarnings("deprecation")
    public DocumentInfo[] findPolicies(AttributeBean[] attributes)
            throws PolicyIndexException {
        Map<String, byte[]> result = policyIndex.findPolicies(attributes);
        if (result == null) {
            return null;
        }

        List<DocumentInfo> docList = new ArrayList<DocumentInfo>();
        for (String name : result.keySet()) {
            String data = null;
            try {
                data = new String(result.get(name), "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new PolicyIndexException(uee.getMessage(), uee);
            }

            DocumentInfo d = new DocumentInfo(name, data);
            docList.add(d);
        }
        return docList.toArray(new DocumentInfo[docList.size()]);
    }

    /**
     * @return the policyDataManager
     */
    public PolicyStore getPolicyDataManager() {
        return policyStorer;
    }

    /**
     * @param policyStore
     *        the policyDataManager to set
     */
    public void setPolicyStore(PolicyStore policyStore) {
        this.policyStorer = policyStore;
    }
}
