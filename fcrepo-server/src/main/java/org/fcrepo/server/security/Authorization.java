/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.authorization.AuthzException;

import java.util.Date;

import org.fcrepo.common.policy.xacml1.XACML1ActionCategoryNamespace;
import org.fcrepo.common.policy.xacml1.XACML1ActionNamespace;
import org.fcrepo.common.policy.xacml1.XACML1EnvironmentCategoryNamespace;
import org.fcrepo.common.policy.xacml1.XACML1EnvironmentNamespace;
import org.fcrepo.common.policy.xacml1.XACML1ResourceCategoryNamespace;
import org.fcrepo.common.policy.xacml1.XACML1ResourceNamespace;
import org.fcrepo.common.policy.xacml1.XACML1SubjectCategoryNamespace;
import org.fcrepo.common.policy.xacml1.XACML1SubjectNamespace;


/**
 * @author Bill Niebel
 */
public interface Authorization {

    //subject
    // "urn:oasis:names:tc:xacml:1.0:subject";
    public static final String SUBJECT_CATEGORY =
            XACML1SubjectNamespace.getInstance().uri;

    // "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"
    public static final String SUBJECT_CATEGORY_ACCESS =
            XACML1SubjectCategoryNamespace.getInstance().ACCESS_SUBJECT.uri;

    //action
    // "urn:oasis:names:tc:xacml:1.0:action";
    public static final String ACTION_CATEGORY =
            XACML1ActionNamespace.getInstance().uri;

    // "urn:oasis:names:tc:xacml:1.0:action-category:access-action";
    public static final String ACTION_CATEGORY_ACCESS =
            XACML1ActionCategoryNamespace.getInstance().ACCESS_ACTION.uri;

    //resource
    // "urn:oasis:names:tc:xacml:1.0:resource";
    public static final String RESOURCE_CATEGORY =
            XACML1ResourceNamespace.getInstance().uri;

    // "urn:oasis:names:tc:xacml:1.0:resource-category:access-resource";
    public static final String RESOURCE_CATEGORY_ACCESS =
            XACML1ResourceCategoryNamespace.getInstance().ACCESS_RESOURCE.uri;

    //environment
    // "urn:oasis:names:tc:xacml:1.0:environment";
    public static final String ENVIRONMENT_CATEGORY =
            XACML1EnvironmentNamespace.getInstance().uri;

    // "urn:oasis:names:tc:xacml:1.0:environment-category:access-environment";
    public static final String ENVIRONMENT_CATEGORY_ACCESS =
            XACML1EnvironmentCategoryNamespace.getInstance().ACCESS_ENVIRONMENT.uri;

    public static final String FEDORA_ROLE_KEY = "fedoraRole";

    public void reloadPolicies(Context context) throws Exception;

    public void enforceAddDatastream(Context context,
                                     String pid,
                                     String dsId,
                                     String[] altIDs,
                                     String MIMEType,
                                     String formatURI,
                                     String dsLocation,
                                     String controlGroup,
                                     String dsState,
                                     String checksumType,
                                     String checksum) throws AuthzException;

    public void enforceExport(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String exportEncoding) throws AuthzException;

    @Deprecated
    public void enforceExportObject(Context context,
                                    String pid,
                                    String format,
                                    String exportContext,
                                    String exportEncoding)
            throws AuthzException;

    public void enforceGetDatastream(Context context,
                                     String pid,
                                     String datastreamId,
                                     Date asOfDateTime) //x
            throws AuthzException;

    public void enforceGetDatastreamHistory(Context context,
                                            String pid,
                                            String datastreamId)
            throws AuthzException;

    public void enforceGetDatastreams(Context context,
                                      String pid,
                                      Date asOfDate,
                                      String state) throws AuthzException;

    public void enforceGetNextPid(Context context,
                                  String namespace,
                                  int nNewPids) throws AuthzException;

    public void enforceGetObjectXML(Context context,
                                    String pid,
                                    String objectXmlEncoding)
            throws AuthzException;

    public void enforceIngest(Context context,
                              String pid,
                              String format,
                              String ingestEncoding) throws AuthzException;

    @Deprecated
    public void enforceIngestObject(Context context,
                                    String pid,
                                    String format,
                                    String ingestEncoding)
            throws AuthzException;

    public void enforceListObjectInFieldSearchResults(Context context,
                                                      String pid)
            throws AuthzException;

    public void enforceListObjectInResourceIndexResults(Context context,
                                                        String pid)
            throws AuthzException;

    public void enforceModifyDatastreamByReference(Context context,
                                                   String pid,
                                                   String datastreamId,
                                                   String[] altIDs,
                                                   String mimeType,
                                                   String formatURI,
                                                   String datastreamNewLocation,
                                                   String checksumType,
                                                   String checksum)
            throws AuthzException;

    public void enforceModifyDatastreamByValue(Context context,
                                               String pid,
                                               String datastreamId,
                                               String[] altIDs,
                                               String mimeType,
                                               String formatURI,
                                               String checksumType,
                                               String checksum)
            throws AuthzException;

    public void enforceModifyObject(Context context,
                                    String pid,
                                    String objectState,
                                    String ownerId) throws AuthzException;

    public void enforcePurgeDatastream(Context context,
                                       String pid,
                                       String datastreamId,
                                       Date endDT) throws AuthzException;

    public void enforcePurgeObject(Context context, String pid)
            throws AuthzException;

    public void enforceSetDatastreamState(Context context,
                                          String pid,
                                          String datastreamId,
                                          String datastreamNewState)
            throws AuthzException;

    public void enforceSetDatastreamVersionable(Context context,
                                                String pid,
                                                String datastreamId,
                                                boolean versionableNewState)
            throws AuthzException;

    public void enforceCompareDatastreamChecksum(Context context,
                                                 String pid,
                                                 String datastreamId,
                                                 Date versionDate)
            throws AuthzException;

    public void enforceGetRelationships(Context context,
                                        String pid,
                                        String predicate) throws AuthzException;

    public void enforceAddRelationship(Context context,
                                       String pid,
                                       String predicate,
                                       String object,
                                       boolean isLiteral,
                                       String datatype) throws AuthzException;

    public void enforcePurgeRelationship(Context context,
                                         String pid,
                                         String predicate,
                                         String object,
                                         boolean isLiteral,
                                         String datatype) throws AuthzException;

    //APIA

    public void enforceDescribeRepository(Context context)
            throws AuthzException;

    public void enforceFindObjects(Context context) throws AuthzException;

    public void enforceRIFindObjects(Context context) throws AuthzException;

    public void enforceGetDatastreamDissemination(Context context,
                                                  String pid,
                                                  String datastreamId,
                                                  Date asOfDate)
            throws AuthzException;

    public void enforceGetDissemination(Context context,
                                        String pid,
                                        String sDefPID,
                                        String methodName,
                                        Date asOfDate,
                                        String authzAux_objState,
                                        String authzAux_sdefState,
                                        String authzAux_sDepPID,
                                        String authzAux_sDepState,
                                        String authzAux_dissState)
            throws AuthzException;

    public void enforceGetObjectHistory(Context context, String pid)
            throws AuthzException;

    public void enforceGetObjectProfile(Context context,
                                        String pid,
                                        Date asOfDate) throws AuthzException;

    public void enforceListDatastreams(Context context,
                                       String pid,
                                       Date asOfDate) throws AuthzException;

    public void enforceListMethods(Context context, String pid, Date ofAsDate)
            throws AuthzException;

    public void enforceServerStatus(Context context) throws AuthzException;

    public void enforceOAIRespond(Context context) throws AuthzException;

    public void enforceUpload(Context context) throws AuthzException;

    public void enforce_Internal_DSState(Context context,
                                         String PID,
                                         String state) throws AuthzException;

    public void enforceResolveDatastream(Context context, Date ticketDateTime)
            throws AuthzException;

    public void enforceReloadPolicies(Context context) throws AuthzException;

    public void enforceRetrieveFile(Context context, String fileURI)
            throws AuthzException;

    public void enforceValidate(Context context, String pid, Date asOfDateTime) throws AuthzException;
}
