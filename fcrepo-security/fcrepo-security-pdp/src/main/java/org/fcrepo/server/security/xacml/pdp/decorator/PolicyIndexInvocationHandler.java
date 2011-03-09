/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.decorator;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Server;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.proxy.AbstractInvocationHandler;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndex;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndexException;
import org.fcrepo.server.security.xacml.pdp.data.PolicyIndexFactory;
import org.fcrepo.server.storage.DOManager;



/**
 * A {@link java.lang.reflect.InvocationHandler InvocationHandler} responsible
 * for updating the FeSL XACML policy index whenever API-M
 * invocations result in changes to a policy stored in a Fedora object.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */

public class PolicyIndexInvocationHandler
extends AbstractInvocationHandler {

    /** Logger for this class. */
    private static Logger LOG =
        LoggerFactory.getLogger(PolicyIndexInvocationHandler.class.getName());


    private Boolean initialised = null;

    private DOManager m_DOManager = null;

    // for updating the policy cache
    private PolicyIndex policyIndex = null;

    public boolean init() {
        if (initialised != null)
            return initialised;

        initialised = false;

        Server s_server = null;
        try {
            s_server = Server.getInstance(new File(Constants.FEDORA_HOME), false);
        } catch (Exception  e) {
            LOG.error("Failed to get server instance",e);
            return false;
        }
        m_DOManager = (DOManager) s_server.getModule(
        "org.fcrepo.server.storage.DOManager");
        if (m_DOManager == null) {
            LOG.error("failed to get DOManager module");
            return false;
        }

        try {
            PolicyIndexFactory  factory = new PolicyIndexFactory();
            policyIndex = factory.newPolicyIndex();
        } catch (PolicyIndexException e) {
            LOG.error("Failed to initialise PolicyIndex", e);
            return false;
        }

        initialised = true;
        return initialised;


    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable {

        // if initialisation fails, just call the target method and quit
        if (!init())
            return invokeTarget(target, method, args);

        // get the method parameters and "type"
        ManagementMethodInvocation managementMethod = new ManagementMethodInvocation(method, args);

        // if it's not a method that requires policy cache modifications, quit
        if (managementMethod.action.equals(ManagementMethodInvocation.Action.NA))
            return invokeTarget(target, method, args);

        Object returnValue = null;

        // holds the state of the Fedora policy object before and after the API method is invoked
        PolicyObject policyBefore = null;
        PolicyObject policyAfter = null;

        switch (managementMethod.target) {
            case DIGITALOBJECT: // API methods operating on the object
                switch (managementMethod.component) {
                    case STATE: // object state change
                        // if the requested object state is null, then there's no state change - no change to policy index
                        if (managementMethod.parameters.objectState != null && managementMethod.parameters.objectState.length() > 0) {
                            // get the "before" object and determine if it was indexed in the cache
                            policyBefore = new PolicyObject(m_DOManager,
                                                            managementMethod.parameters.context,
                                                            managementMethod.parameters.pid,
                                                            null,
                                                            null,
                                                            null);
                            boolean indexedBefore = policyBefore.isPolicyActive();

                            returnValue = invokeTarget(target, method, args);

                            // get the object after the state change and determine if it should be indexed
                            policyAfter = new PolicyObject(m_DOManager,
                                                           managementMethod.parameters.context,
                                                           managementMethod.parameters.pid,
                                                           managementMethod.parameters.objectState,
                                                           null,
                                                           null);

                            // if the indexing status has changed, make appropriate changes to the policy cache/index
                            if (indexedBefore != policyAfter.isPolicyActive()) {
                                if (policyAfter.isPolicyActive() ) {
                                    addOrUpdatePolicy(managementMethod.parameters.pid, policyAfter.getDsContent());
                                } else {
                                    deletePolicy(managementMethod.parameters.pid);
                                }
                            }
                        }
                        break;

                    case CONTENT: // object ingest or purge
                        switch (managementMethod.action){
                            case CREATE: // ingest
                                // do the API call
                                returnValue = invokeTarget(target, method, args);

                                // get the ingested policy - note ingested object PID is the return value
                                policyAfter = new PolicyObject(m_DOManager,
                                                               managementMethod.parameters.context,
                                                               (String)returnValue,
                                                               null,
                                                               null,
                                                               null);
                                // add to the cache if required
                                if (policyAfter.isPolicyActive()) {
                                    addOrUpdatePolicy((String)returnValue, policyAfter.getDsContent());
                                }

                                break;
                            case DELETE: // purge
                                // get the policy object that existed prior to ingest and see if it was indexed
                                policyBefore = new PolicyObject(m_DOManager,
                                                                managementMethod.parameters.context,
                                                                managementMethod.parameters.pid,
                                                                null,
                                                                null,
                                                                null);
                                boolean wasIndexed = policyBefore.isPolicyActive();

                                // do the API call
                                returnValue = invokeTarget(target, method, args);

                                // if the policy was indexed, delete it from the cache/index
                                if (wasIndexed)
                                    deletePolicy(managementMethod.parameters.pid);

                                break;
                            default:
                        }
                        break;

                    default:

                }
                break;


            case DATASTREAM: // operations on datastreams
                // note, DS ID can be null - server-assigned ID
                if (managementMethod.parameters.dsID != null && managementMethod.parameters.dsID.equals(FedoraPolicyStore.POLICY_DATASTREAM)) {
                    switch (managementMethod.component) {
                        case STATE: // datastream state change

                            // get the object prior to the API call and see if it was indexed/cached
                            policyBefore = new PolicyObject(m_DOManager,
                                                            managementMethod.parameters.context,
                                                            managementMethod.parameters.pid,
                                                            null,
                                                            managementMethod.parameters.dsID,
                                                            null);
                            boolean wasIndexed = policyBefore.isPolicyActive();

                            // do the API call
                            returnValue = invokeTarget(target, method, args);

                            // the object after the call
                            policyAfter = new PolicyObject(m_DOManager,
                                                           managementMethod.parameters.context,
                                                           managementMethod.parameters.pid,
                                                           null,
                                                           managementMethod.parameters.dsID,
                                                           managementMethod.parameters.dsState);

                            // if indexing status has changed, update the cache/index
                            if (wasIndexed != policyAfter.isPolicyActive()) {
                                if (policyAfter.isPolicyActive()) {
                                    addOrUpdatePolicy(managementMethod.parameters.pid, policyAfter.getDsContent());
                                } else {
                                    deletePolicy(managementMethod.parameters.pid);
                                }
                            }
                            break;

                        case CONTENT: // datastream add, modify, purge

                            switch (managementMethod.action){
                                case CREATE:
                                    // do the API call
                                    returnValue = invokeTarget(target, method, args);

                                    // get the policy object after the method call
                                    policyAfter = new PolicyObject(m_DOManager,
                                                                   managementMethod.parameters.context,
                                                                   managementMethod.parameters.pid,
                                                                   null,
                                                                   managementMethod.parameters.dsID,
                                                                   managementMethod.parameters.dsState);
                                    if (policyAfter.isPolicyActive())
                                        addOrUpdatePolicy(managementMethod.parameters.pid, policyAfter.getDsContent());

                                    break;
                                case DELETE:
                                    // get the policy object before the call and see if it was indexed
                                    policyBefore = new PolicyObject(m_DOManager,
                                                                    managementMethod.parameters.context,
                                                                    managementMethod.parameters.pid,
                                                                    null,
                                                                    managementMethod.parameters.dsID,
                                                                    managementMethod.parameters.dsState);
                                    wasIndexed = policyBefore.isPolicyActive();

                                    // invoke the method
                                    returnValue = invokeTarget(target, method, args);

                                    policyAfter = new PolicyObject(m_DOManager,
                                                                   managementMethod.parameters.context,
                                                                   managementMethod.parameters.pid,
                                                                   null,
                                                                   managementMethod.parameters.dsID,
                                                                   null);

                                    if (wasIndexed) {
                                        //if policy is still active after, only a version was purged and the policy has changed.  Update
                                        if (policyAfter.isPolicyActive()) {
                                            addOrUpdatePolicy(managementMethod.parameters.pid, policyAfter.getDsContent());

                                            //policy is not active after, then it was deleted and needs to be removed from index.
                                        } else {
                                            deletePolicy(managementMethod.parameters.pid);
                                        }
                                    }
                                    break;

                                case UPDATE:
                                    // do the API call, get the policy object after the call
                                    returnValue = invokeTarget(target, method, args);
                                    policyAfter = new PolicyObject(m_DOManager,
                                                                   managementMethod.parameters.context,
                                                                   managementMethod.parameters.pid,
                                                                   null,
                                                                   managementMethod.parameters.dsID,
                                                                   null);
                                    if (policyAfter.isPolicyActive())
                                        addOrUpdatePolicy(managementMethod.parameters.pid, policyAfter.getDsContent());

                                    break;

                                default:
                            }

                            break;

                        default:

                    }
                }

                break;

            default:


        }


        // if API call not made as part of the above (ie no action was required on policy cache), do it now
        if (returnValue == null)
            returnValue = invokeTarget(target,method, args);

        return returnValue;
    }

    /**
     * Invoke the underlying method, catching any InvocationTargetException and rethrowing the target exception
     */
    private Object invokeTarget(Object target, Method method, Object[] args) throws Throwable {
        Object returnValue;
        try {
            returnValue = method.invoke(target, args);
        } catch(InvocationTargetException ite) {
            throw ite.getTargetException();
        }
        return returnValue;
    }

    /**
     * Add/update policy to cache/index
     * @param pid
     * @param dsContent
     * @throws PolicyIndexException
     * @throws GeneralException
     * @throws IOException
     */
    private void addOrUpdatePolicy(String pid, InputStream dsContent) throws GeneralException  {
        LOG.debug("Adding/Updating policy " + pid);

        String policy;

        try {
            policy = IOUtils.toString(dsContent);
            // make sure stream is closed - with Akubra on Windows this can leave the stream open
            // and leave the file locked (prevents a purge later)
            dsContent.close();
            policyIndex.addPolicy(pid, policy);
        } catch (IOException e) {
            throw new GeneralException("Error adding policy " + pid + " to policy index: " + e.getMessage(), e);
        } catch (PolicyIndexException e) {
            throw new GeneralException("Error adding policy " + pid + " to policy index: " + e.getMessage(), e);
        }

        // TODO: invalidate PEP cache here? - or do in all policyIndex.addPolicy ?


    }
    /**
     * Remove the specified policy from the cache
     * @param pid
     * @throws PolicyIndexException
     */
    private void deletePolicy(String pid) throws GeneralException  {
        LOG.debug("Deleting policy " + pid);

        try {
            // in case the policy doesn't exist (corrupt policy index, failed to add because invalid, etc)
            policyIndex.deletePolicy(pid);
        } catch (PolicyIndexException e) {
            throw new GeneralException("Error deleting policy " + pid + " from policy index: " + e.getMessage(), e);
        }
    }



}

