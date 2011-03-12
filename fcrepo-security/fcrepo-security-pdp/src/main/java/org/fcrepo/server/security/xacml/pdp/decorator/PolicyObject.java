/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.decorator;

import java.io.InputStream;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
/**
 * Wrapper for a FeSL XACML policy stored as a Fedora digital object.
 *
 * Contains method for determining whether or not this is a digital object that
 * represents an active FeSL XACML policy.
 *
 * Only objects that have a policy datastream which is active within an active object
 * are deemed to be active policies.
 *
 * @author Stephen Bayliss
 * @version $Id$
 */




public class PolicyObject {

    private Datastream m_datastream = null;

    private InputStream m_dsContent = null;

    public static final String POLICY_DATASTREAM = FedoraPolicyStore.FESL_POLICY_DATASTREAM;

    private Boolean m_hasPolicyDatastream = null;

    private String m_pid = null;

    private String m_dsState = null;

    private String m_objectState = null;

    private DOManager m_DOManager = null;
    private DOReader m_reader = null;
    private Context m_context = null;

    @SuppressWarnings("unused")
    private PolicyObject() {}

    /**
     * Create a new PolicyObject, initialising the object based on the supplied parameters.
     * Parameters are the same parameters that are generally supplied to API-M methods.
     * Parameters therefore represent "already-known" information about the policy object
     * (after the API-M method succeeded).
     * @param manager
     * @param context
     * @param pid
     * @param objectState
     * @param dsID
     * @param dsState
     */
    public PolicyObject(DOManager manager, Context context, String pid, String objectState, String dsID, String dsState) {

        m_DOManager = manager;
        m_context = context;
        m_pid = pid;
        if (objectState != null)
            m_objectState = objectState;
        if (dsID != null) {
            if (dsID.equals(POLICY_DATASTREAM)) {
                m_hasPolicyDatastream = true;
            }
        }
        if (dsState != null)
            m_dsState = dsState;

    }



    /**
     * determines if this object represents an active FeSL policy
     *
     * @return
     * @throws ServerException
     */
    public boolean isPolicyActive() throws ServerException {

        return (hasPolicyDatastream() && isDatastreamActive() && isObjectActive());
    }

    /**
     * determines if this digital object is active
     * @return
     * @throws ServerException
     */
    public boolean isObjectActive() throws ServerException {
        if (m_objectState == null)
            m_objectState = getReader().GetObjectState();

        return m_objectState.equals("A");

    }

    /**
     * determines if the policy datastream in this object is active
     * @return
     * @throws ServerException
     */
    public boolean isDatastreamActive() throws ServerException {
        if (m_dsState == null && getDatastream() != null)
            m_dsState = m_datastream.DSState;

        return m_dsState != null && m_dsState.equals("A");
    }

    /**
     * determines if this object contains a policy datastream
     * @return
     * @throws ServerException
     */
    public boolean hasPolicyDatastream() throws ServerException {

        if (m_hasPolicyDatastream == null) {
            m_hasPolicyDatastream = false;
            Datastream[] allDs = getReader().GetDatastreams(null, null);
            for (int i = 0; i < allDs.length; i++) {
                if (allDs[i].DatastreamID.equals(POLICY_DATASTREAM)) {
                    m_hasPolicyDatastream = true;
                    // also have the datastream now
                    m_datastream = allDs[i];
                    break;
                }
            }
        }
        return m_hasPolicyDatastream;
    }

    private DOReader getReader() throws ServerException {
        if (m_reader == null) {
            m_reader = m_DOManager.getReader(false, m_context, m_pid);
        }
        return m_reader;
    }

    /**
     * get the policy datastream content
     * @return
     * @throws ServerException
     */
    public InputStream getDsContent() throws ServerException {
        if (m_dsContent == null && getDatastream() != null) {
            m_dsContent = getDatastream().getContentStream();

            // remember we found the policy datastream
            m_hasPolicyDatastream = true;
        }
        return m_dsContent;
    }

    private Datastream getDatastream() throws ServerException {
        if (m_datastream == null) {
            m_datastream = getReader().GetDatastream(POLICY_DATASTREAM, null);

        }
        // if we got the policy datastream, we know it has one
        m_hasPolicyDatastream = true;
        return m_datastream;
    }

}
