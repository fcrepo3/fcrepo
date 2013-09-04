package org.fcrepo.server.security.xacml.pep.impl;

import java.net.URI;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Context;
import org.fcrepo.server.MultiValueMap;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.security.Authorization;
import org.fcrepo.server.security.PolicyEnforcementPoint;


public class FESLAuthorization implements Authorization {
    private static final Logger logger = LoggerFactory.getLogger(FESLAuthorization.class);
    private static final URI BLANK = URI.create("");
    
    private PolicyEnforcementPoint m_pep;
    public FESLAuthorization(PolicyEnforcementPoint pep) {
        m_pep = pep;
    }

    @Override
    public void reloadPolicies(Context context) throws Exception {
        // TODO Implement policy loading for non-web actions

    }

    @Override
    public void enforceAddDatastream(Context context, String pid, String dsId,
            String[] altIDs, String MIMEType, String formatURI,
            String dsLocation, String controlGroup, String dsState,
            String checksumType, String checksum) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceExport(Context context, String pid, String format,
            String exportContext, String exportEncoding) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceExportObject(Context context, String pid, String format,
            String exportContext, String exportEncoding) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetDatastream(Context context, String pid,
            String datastreamId, Date asOfDateTime) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetDatastreamHistory(Context context, String pid,
            String datastreamId) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetDatastreams(Context context, String pid,
            Date asOfDate, String state) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetNextPid(Context context, String namespace,
            int nNewPids) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetObjectXML(Context context, String pid,
            String objectXmlEncoding) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceIngest(Context context, String pid, String format,
            String ingestEncoding) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceIngestObject(Context context, String pid, String format,
            String ingestEncoding) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceListObjectInFieldSearchResults(Context context,
            String pid) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceListObjectInResourceIndexResults(Context context,
            String pid) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceModifyDatastreamByReference(Context context, String pid,
            String datastreamId, String[] altIDs, String mimeType,
            String formatURI, String datastreamNewLocation,
            String checksumType, String checksum) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceModifyDatastreamByValue(Context context, String pid,
            String datastreamId, String[] altIDs, String mimeType,
            String formatURI, String checksumType, String checksum)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceModifyObject(Context context, String pid,
            String objectState, String ownerId) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforcePurgeDatastream(Context context, String pid,
            String datastreamId, Date endDT) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforcePurgeObject(Context context, String pid)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceSetDatastreamState(Context context, String pid,
            String datastreamId, String datastreamNewState)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceSetDatastreamVersionable(Context context, String pid,
            String datastreamId, boolean versionableNewState)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceCompareDatastreamChecksum(Context context, String pid,
            String datastreamId, Date versionDate) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetRelationships(Context context, String pid,
            String predicate) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void
            enforceAddRelationship(Context context, String pid,
                    String predicate, String object, boolean isLiteral,
                    String datatype) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void
            enforcePurgeRelationship(Context context, String pid,
                    String predicate, String object, boolean isLiteral,
                    String datatype) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceDescribeRepository(Context context)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceFindObjects(Context context) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceRIFindObjects(Context context) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetDatastreamDissemination(Context context, String pid,
            String datastreamId, Date asOfDate) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetDissemination(Context context, String pid,
            String sDefPID, String methodName, Date asOfDate,
            String authzAux_objState, String authzAux_sdefState,
            String authzAux_sDepPID, String authzAux_sDepState,
            String authzAux_dissState) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetObjectHistory(Context context, String pid)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceGetObjectProfile(Context context, String pid,
            Date asOfDate) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceListDatastreams(Context context, String pid,
            Date asOfDate) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceListMethods(Context context, String pid, Date ofAsDate)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceServerStatus(Context context) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceOAIRespond(Context context) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceUpload(Context context) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforce_Internal_DSState(Context context, String PID,
            String state) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceResolveDatastream(Context context, Date ticketDateTime)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceReloadPolicies(Context context) throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceRetrieveFile(Context context, String fileURI)
            throws AuthzException {
        try {
            logger.debug("Entered enforceRetrieveFile for {}", fileURI);
            String target = Constants.ACTION.RETRIEVE_FILE.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.setReturn(Constants.DATASTREAM.FILE_URI.attributeId, fileURI);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't be set " +
                Constants.DATASTREAM.FILE_URI.attributeId, e);
            }
            context.setResourceAttributes(resourceAttributes);
            m_pep.enforce(context
                    .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             "",
                             extractNamespace(fileURI),
                             context);
        } finally {
            logger.debug("Exiting enforceRetrieveFile");
        }
    }

    @Override
    public void enforceValidate(Context context, String pid, Date asOfDateTime)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    private final String extractNamespace(String pid) {
        String namespace = "";
        int colonPosition = pid.indexOf(':');
        if (-1 < colonPosition) {
            namespace = pid.substring(0, colonPosition);
        }
        return namespace;
    }

}
