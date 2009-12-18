/*
 * File: PolicyDataManagerService.java
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

package melcoe.xacml.pdp.data;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import melcoe.xacml.util.AttributeBean;

/**
 * This is a wrapper class for a PolicyDataManager class that exposes the
 * management interface as a web service. The wrapper was needed as WSDL does
 * not support classes that container function overloading.
 * 
 * @author nishen@melcoe.mq.edu.au
 * @see PolicyDataManager
 * @see DbXmlPolicyDataManager
 */
public class PolicyDataManagerService {

    private PolicyDataManager policyDataManager = null;

    public PolicyDataManagerService()
            throws PolicyDataManagerException {
        setPolicyDataManager(new DbXmlPolicyDataManager());
    }

    /**
     * Retrieves the document of the given name from the document store and
     * returns it as an array of bytes.
     * 
     * @param name
     *        the document name to return
     * @return the document as a byte array
     * @throws PolicyDataManagerException
     */
    public DocumentInfo getPolicy(String name)
            throws PolicyDataManagerException {
        byte[] documentData = policyDataManager.getPolicy(name);
        if (documentData == null) {
            return null;
        }

        String data = null;
        try {
            data = new String(documentData, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new PolicyDataManagerException(uee.getMessage(), uee);
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
     * @throws PolicyDataManagerException
     */
    public String addPolicy(String document, String name)
            throws PolicyDataManagerException {
        return policyDataManager.addPolicy(document, name);
    }

    /**
     * Removes the document of the given name from the document store.
     * 
     * @param name
     *        the name of the document to remove.
     * @throws PolicyDataManagerException
     */
    public boolean deletePolicy(String name) throws PolicyDataManagerException {
        return policyDataManager.deletePolicy(name);
    }

    /**
     * Updates a document of the given name by replacing it with the new
     * document provided as a String.
     * 
     * @param name
     *        the name of the document to update.
     * @param newDocument
     *        the new document as a String.
     * @throws PolicyDataManagerException
     */
    public boolean updatePolicy(String name, String newDocument)
            throws PolicyDataManagerException {
        return policyDataManager.updatePolicy(name, newDocument);
    }

    /**
     * Lists all documents in the document store.
     * 
     * @return array of Strings that are the names of all stored documents.
     * @throws PolicyDataManagerException
     */
    public String[] listPolicies() throws PolicyDataManagerException {
        List<String> result = policyDataManager.listPolicies();
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
        return policyDataManager.getLastUpdate();
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
     * @throws PolicyDataManagerException
     */
    public DocumentInfo[] findPolicies(AttributeBean[] attributes)
            throws PolicyDataManagerException {
        Map<String, byte[]> result = policyDataManager.findPolicies(attributes);
        if (result == null) {
            return null;
        }

        List<DocumentInfo> docList = new ArrayList<DocumentInfo>();
        for (String name : result.keySet()) {
            String data = null;
            try {
                data = new String(result.get(name), "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new PolicyDataManagerException(uee.getMessage(), uee);
            }

            DocumentInfo d = new DocumentInfo(name, data);
            docList.add(d);
        }
        return docList.toArray(new DocumentInfo[docList.size()]);
    }

    /**
     * @return the policyDataManager
     */
    public PolicyDataManager getPolicyDataManager() {
        return policyDataManager;
    }

    /**
     * @param policyDataManager
     *        the policyDataManager to set
     */
    public void setPolicyDataManager(PolicyDataManager policyDataManager) {
        this.policyDataManager = policyDataManager;
    }
}
