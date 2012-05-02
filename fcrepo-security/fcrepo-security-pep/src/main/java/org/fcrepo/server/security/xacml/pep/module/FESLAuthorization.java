package org.fcrepo.server.security.xacml.pep.module;

import java.util.Date;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.security.Authorization;


public class FESLAuthorization implements Authorization {

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
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

    @Override
    public void enforceValidate(Context context, String pid, Date asOfDateTime)
            throws AuthzException {
        // TODO Determine whether FESL auth checks should be performed at the module level

    }

}
