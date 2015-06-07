/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.Module;
import org.fcrepo.server.MultiValueMap;
import org.fcrepo.server.Server;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.utilities.status.ServerState;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Authorization module, protecting access to Fedora's API-A and API-M
 * endpoints.
 * <p>
 * The following attributes are available for use in authorization policies
 * during any enforce call.
 * </p>
 * <p>
 * subject attributes</p>
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:subject:loginId (available only if user
 * has authenticated)</li>
 * <li>urn:fedora:names:fedora:2.1:subject:<i>x</i> (available if
 * authenticated user has attribute <i>x</i>)</li>
 * </ul>
 * <p>
 * environment attributes derived from HTTP request</p>
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:security
 * <ul>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:security-secure(i.e.,
 * request is HTTPS/SSL)</li>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:security-insecure(i.e.,
 * request is HTTP/non-SSL)</li>
 * </ul>
 * </li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol
 * <ul>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-soap(i.e.,
 * request is over SOAP/CXF)</li>
 * <li>==
 * urn:fedora:names:fedora:2.1:environment:httpRequest:messageProtocol-rest(i.e.,
 * request is over non-SOAP/CXF ("REST") HTTP call)</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * environment attributes directly from HTTP request</p>
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:authType</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:clientFqdn</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:clientIpAddress</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:contentLength</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:contentType</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:method</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:protocol</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:scheme</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:serverFqdn</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:serverIpAddress</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:serverPort</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:sessionEncoding</li>
 * <li>urn:fedora:names:fedora:2.1:environment:httpRequest:sessionStatus</li>
 * </ul>
 * <p>
 * other environment attributes</p>
 * <ul>
 * <li>urn:fedora:names:fedora:2.1:currentDateTime</li>
 * <li>urn:fedora:names:fedora:2.1:currentDate</li>
 * <li>urn:fedora:names:fedora:2.1:currentTime</li>
 * </ul>
 *
 * @see <a
 *      href="http://java.sun.com/products/servlet/2.2/javadoc/javax/servlet/http/HttpServletRequest.html">HttpServletRequest
 *      interface documentation</a>
 */
public class DefaultAuthorization
        extends Module
        implements Authorization {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultAuthorization.class);

    @SuppressWarnings("unused")
    private static final String REPOSITORY_POLICY_GUITOOL_DIRECTORY_KEY =
            "REPOSITORY-POLICY-GUITOOL-POLICIES-DIRECTORY";

    private PolicyEnforcementPoint xacmlPep;

    boolean enforceListObjectInFieldSearchResults = true;

    boolean enforceListObjectInResourceIndexResults = true;

    private String m_ownerIdSeparator = ResourceAttributeFinderModule.DEFAULT_OWNER_ID_SEPARATOR;

    /**
     * Creates and initializes the Access Module. When the server is starting
     * up, this is invoked as part of the initialization process.
     *
     * @param moduleParameters A pre-loaded Map of name-value pairs comprising the intended
     *                         configuration of this Module.
     * @param server           The <code>Server</code> instance.
     * @param role             The role this module fulfills, a java class name.
     * @throws ModuleInitializationException If initilization values are invalid or initialization fails for
     *                                       some other reason.
     */
    public DefaultAuthorization(Map<String,String> moduleParameters, Server server, String role)
            throws ModuleInitializationException {
        super(moduleParameters, server, role);
        if (moduleParameters.containsKey(ResourceAttributeFinderModule.OWNER_ID_SEPARATOR_CONFIG_KEY)) {
            m_ownerIdSeparator = moduleParameters.get(ResourceAttributeFinderModule.OWNER_ID_SEPARATOR_CONFIG_KEY);
            logger.debug("resourceAttributeFinder just set ownerIdSeparator ==[{}]",
                    m_ownerIdSeparator);
        }
    }

    @Override
    public void initModule() throws ModuleInitializationException {
    }

    @Override
    public void postInitModule() throws ModuleInitializationException {
        try {
            getServer().getStatusFile()
                    .append(ServerState.STARTING,
                            "Initializing XACML Authorization Module");
            xacmlPep = getServer().getBean(PolicyEnforcementPoint.class.getName(), PolicyEnforcementPoint.class);
            xacmlPep.newPdp();
        } catch (Throwable e1) {
            throw new ModuleInitializationException(e1.getMessage(),
                                                    getRole(),
                                                    e1);
        }
    }

    @Override
    public void reloadPolicies(Context context) throws Exception {
        enforceReloadPolicies(context);
        xacmlPep.newPdp();
    }

    private final String extractNamespace(String pid) {
        String namespace = "";
        int colonPosition = pid.indexOf(':');
        if (-1 < colonPosition) {
            namespace = pid.substring(0, colonPosition);
        }
        return namespace;
    }

    /**
     * Enforce authorization for adding a datastream to an object. Provide
     * attributes for the authorization decision and wrap that xacml decision.
     * <p>
     * The following attributes are available for use in authorization policies
     * during a call to this method.
     * </p>
     * <p>
     * action attributes</p>
     * <ul>
     * <li>urn:fedora:names:fedora:2.1:action:id ==
     * urn:fedora:names:fedora:2.1:action:id-addDatastream</li>
     * <li>urn:fedora:names:fedora:2.1:action:api ==
     * urn:fedora:names:fedora:2.1:action:api-m</li>
     * </ul>
     * 
     * <p>
     * resource attributes of object to which datastream would be added</p>
     * <ul>
     * <li>urn:fedora:names:fedora:2.1:resource:object:pid</li>
     * <li>urn:fedora:names:fedora:2.1:resource:object:namespace (if pid is
     * "x:y", namespace is "x")</li>
     * </ul>
     * <p>
     * resource attributes of datastream which would be added</p>
     * <ul>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:mimeType</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:formatUri</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:state</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:id</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:location</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:controlGroup</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:altIds</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:checksumType</li>
     * <li>urn:fedora:names:fedora:2.1:resource:datastream:checksum</li>
     * </ul>
     */
    @Override
    public final void enforceAddDatastream(Context context,
                                           String pid,
                                           String dsId,
                                           String[] altIDs,
                                           String MIMEType,
                                           String formatURI,
                                           String dsLocation,
                                           String controlGroup,
                                           String dsState,
                                           String checksumType,
                                           String checksum)
            throws AuthzException {
        try {
            logger.debug("Entered enforceAddDatastream");
            String target = Constants.ACTION.ADD_DATASTREAM.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.MIME_TYPE.attributeId;
                resourceAttributes.set(name, MIMEType);
                name = Constants.DATASTREAM.FORMAT_URI.attributeId;
                resourceAttributes.set(name, formatURI);
                name = Constants.DATASTREAM.STATE.attributeId;
                resourceAttributes.set(name, dsState);
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, dsId);
                name = Constants.DATASTREAM.LOCATION.attributeId;
                resourceAttributes.set(name, dsLocation);
                name = Constants.DATASTREAM.CONTROL_GROUP.attributeId;
                resourceAttributes.set(name, controlGroup);
                name = Constants.DATASTREAM.ALT_IDS.attributeId;
                resourceAttributes.set(name, altIDs);
                name = Constants.DATASTREAM.CHECKSUM_TYPE.attributeId;
                resourceAttributes.set(name, checksumType);
                name = Constants.DATASTREAM.CHECKSUM.attributeId;
                resourceAttributes.set(name, checksum);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceAddDatastream");
        }
    }

    @Override
    public final void enforceExport(Context context,
                                    String pid,
                                    String format,
                                    String exportContext,
                                    String exportEncoding)
            throws AuthzException {
        try {
            logger.debug("Entered enforceExport");
            String target = Constants.ACTION.EXPORT.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.OBJECT.FORMAT_URI.attributeId;
                resourceAttributes.set(name, format);
                name = Constants.OBJECT.CONTEXT.attributeId;
                resourceAttributes.set(name, exportContext);
                name = Constants.OBJECT.ENCODING.attributeId;
                resourceAttributes.set(name, exportEncoding);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceExport");
        }
    }

    /**
     * @deprecated in Fedora 3.0, use enforceExport() instead
     */
    @Override
    @Deprecated
    public final void enforceExportObject(Context context,
                                          String pid,
                                          String format,
                                          String exportContext,
                                          String exportEncoding)
            throws AuthzException {
        enforceExport(context, pid, format, exportContext, exportEncoding);
    }

    @Override
    public final void enforceGetNextPid(Context context,
                                        String namespace,
                                        int nNewPids) throws AuthzException {
        try {
            logger.debug("Entered enforceGetNextPid");
            String target = Constants.ACTION.GET_NEXT_PID.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                String nNewPidsAsString = Integer.toString(nNewPids);
                resourceAttributes.set(Constants.OBJECT.N_PIDS.attributeId,
                                       nNewPidsAsString);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.OBJECT.N_PIDS.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             "",
                             namespace,
                             context);
        } finally {
            logger.debug("Exiting enforceGetNextPid");
        }
    }

    @Override
    public final void enforceGetDatastream(Context context,
                                           String pid,
                                           String datastreamId,
                                           Date asOfDateTime)
            throws AuthzException {
        try {
            logger.debug("Entered enforceGetDatastream");
            String target = Constants.ACTION.GET_DATASTREAM.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.DATASTREAM.AS_OF_DATETIME.attributeId;
                resourceAttributes.set(name, ensureDate(asOfDateTime, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetDatastream");
        }
    }

    @Override
    public final void enforceGetDatastreamHistory(Context context,
                                                  String pid,
                                                  String datastreamId)
            throws AuthzException {
        try {
            logger.debug("Entered enforceGetDatastreamHistory");
            String target = Constants.ACTION.GET_DATASTREAM_HISTORY.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.DATASTREAM.ID.attributeId,
                                       datastreamId);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.DATASTREAM.ID.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetDatastreamHistory");
        }
    }

    private final String ensureDate(Date date, Context context)
            throws AuthzOperationalException {
        if (date == null) {
            date = context.now();
        }
        String dateAsString;
        try {
            dateAsString = dateAsString(date);
        } catch (Throwable t) {
            throw new AuthzOperationalException("couldn't make date a string",
                                                t);
        }
        return dateAsString;
    }

    @Override
    public final void enforceGetDatastreams(Context context,
                                            String pid,
                                            Date asOfDate,
                                            String datastreamState)
            throws AuthzException {
        try {
            logger.debug("Entered enforceGetDatastreams");
            String target = Constants.ACTION.GET_DATASTREAMS.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.STATE.attributeId;
                resourceAttributes.set(name, datastreamState);
                name = Constants.RESOURCE.AS_OF_DATETIME.attributeId;
                resourceAttributes.set(name, ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context
                            .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetDatastreams");
        }
    }

    @Override
    public final void enforceGetObjectXML(Context context,
                                          String pid,
                                          String objectXmlEncoding)
            throws AuthzException {
        try {
            logger.debug("Entered enforceGetObjectXML");
            String target = Constants.ACTION.GET_OBJECT_XML.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes
                        .set(Constants.OBJECT.ENCODING.attributeId,
                             objectXmlEncoding);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.OBJECT.ENCODING.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetObjectXML");
        }
    }

    @Override
    public final void enforceIngest(Context context,
                                    String pid,
                                    String format,
                                    String ingestEncoding)
            throws AuthzException {
        try {
            logger.debug("Entered enforceIngest");
            String target = Constants.ACTION.INGEST.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.OBJECT.FORMAT_URI.attributeId;
                resourceAttributes.set(name, format);
                name = Constants.OBJECT.ENCODING.attributeId;
                resourceAttributes.set(name, ingestEncoding);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context
                            .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceIngest");
        }
    }

    /**
     * @deprecated in Fedora 3.0, use enforceIngest() instead
     */
    @Override
    @Deprecated
    public final void enforceIngestObject(Context context,
                                          String pid,
                                          String format,
                                          String ingestEncoding)
            throws AuthzException {
        enforceIngest(context, pid, format, ingestEncoding);
    }

    @Override
    public final void enforceListObjectInFieldSearchResults(Context context,
                                                            String pid)
            throws AuthzException {
        try {
            logger.debug("Entered enforceListObjectInFieldSearchResults");
            String target =
                    Constants.ACTION.LIST_OBJECT_IN_FIELD_SEARCH_RESULTS.uri;
            if (enforceListObjectInFieldSearchResults) {
                context.setActionAttributes(null);
                context.setResourceAttributes(null);
                xacmlPep
                        .enforce(context
                                .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                                 target,
                                 Constants.ACTION.APIA.uri,
                                 pid,
                                 extractNamespace(pid),
                                 context);
            }
        } finally {
            logger.debug("Exiting enforceListObjectInFieldSearchResults");
        }
    }

    @Override
    public final void enforceListObjectInResourceIndexResults(Context context,
                                                              String pid)
            throws AuthzException {
        try {
            logger.debug("Entered enforceListObjectInResourceIndexResults");
            String target =
                    Constants.ACTION.LIST_OBJECT_IN_RESOURCE_INDEX_RESULTS.uri;
            if (enforceListObjectInResourceIndexResults) {
                context.setActionAttributes(null);
                context.setResourceAttributes(null);
                xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                                 target,
                                 Constants.ACTION.APIA.uri,
                                 pid,
                                 extractNamespace(pid),
                                 context);
            }
        } finally {
            logger.debug("Exiting enforceListObjectInResourceIndexResults");
        }
    }

    @Override
    public final void enforceModifyDatastreamByReference(Context context,
                                                         String pid,
                                                         String datastreamId,
                                                         String[] datastreamNewAltIDs,
                                                         String datastreamNewMimeType,
                                                         String datastreamNewFormatURI,
                                                         String datastreamNewLocation,
                                                         String datastreamNewChecksumType,
                                                         String datastreamNewChecksum)
            throws AuthzException {
        try {
            logger.debug("Entered enforceModifyDatastreamByReference");
            String target = Constants.ACTION.MODIFY_DATASTREAM_BY_REFERENCE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.DATASTREAM.NEW_MIME_TYPE.attributeId;
                resourceAttributes.set(name, datastreamNewMimeType);
                name = Constants.DATASTREAM.NEW_FORMAT_URI.attributeId;
                resourceAttributes.set(name, datastreamNewFormatURI);
                name = Constants.DATASTREAM.NEW_LOCATION.attributeId;
                resourceAttributes.set(name, datastreamNewLocation);
                name = Constants.DATASTREAM.NEW_ALT_IDS.attributeId;
                resourceAttributes.set(name, datastreamNewAltIDs);
                name = Constants.DATASTREAM.NEW_CHECKSUM_TYPE.attributeId;
                resourceAttributes.set(name, datastreamNewChecksumType);
                name = Constants.DATASTREAM.NEW_CHECKSUM.attributeId;
                resourceAttributes.set(name, datastreamNewChecksum);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceModifyDatastreamByReference");
        }
    }

    @Override
    public final void enforceModifyDatastreamByValue(Context context,
                                                     String pid,
                                                     String datastreamId,
                                                     String[] newDatastreamAltIDs,
                                                     String newDatastreamMimeType,
                                                     String newDatastreamFormatURI,
                                                     String newDatastreamChecksumType,
                                                     String newDatastreamChecksum)
            throws AuthzException {
        try {
            logger.debug("Entered enforceModifyDatastreamByValue");
            String target = Constants.ACTION.MODIFY_DATASTREAM_BY_VALUE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.DATASTREAM.NEW_MIME_TYPE.attributeId;
                resourceAttributes.set(name, newDatastreamMimeType);
                name = Constants.DATASTREAM.NEW_FORMAT_URI.attributeId;
                resourceAttributes.set(name, newDatastreamFormatURI);
                name = Constants.DATASTREAM.NEW_ALT_IDS.attributeId;
                resourceAttributes.set(name, newDatastreamAltIDs);
                name = Constants.DATASTREAM.NEW_CHECKSUM_TYPE.attributeId;
                resourceAttributes.set(name, newDatastreamChecksumType);
                name = Constants.DATASTREAM.NEW_CHECKSUM.attributeId;
                resourceAttributes.set(name, newDatastreamChecksum);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceModifyDatastreamByValue");
        }
    }

    @Override
    public final void enforceModifyObject(Context context,
                                          String pid,
                                          String objectNewState,
                                          String objectNewOwnerId)
            throws AuthzException {
        try {
            logger.debug("Entered enforceModifyObject");
            String target = Constants.ACTION.MODIFY_OBJECT.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.OBJECT.NEW_STATE.attributeId;
                resourceAttributes.set(name, objectNewState);
                if (objectNewOwnerId != null){
                    name = Constants.OBJECT.OWNER.attributeId;
                    resourceAttributes.set(name, objectNewOwnerId.split(m_ownerIdSeparator));
                }
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep
                    .enforce(context
                            .getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceModifyObject");
        }
    }

    @Override
    public final void enforcePurgeDatastream(Context context,
                                             String pid,
                                             String datastreamId,
                                             Date endDT) throws AuthzException {
        try {
            logger.debug("Entered enforcePurgeDatastream");
            String target = Constants.ACTION.PURGE_DATASTREAM.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.RESOURCE.AS_OF_DATETIME.attributeId;
                resourceAttributes.set(name, ensureDate(endDT, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforcePurgeDatastream");
        }
    }

    @Override
    public final void enforcePurgeObject(Context context, String pid)
            throws AuthzException {
        try {
            logger.debug("Entered enforcePurgeObject");
            String target = Constants.ACTION.PURGE_OBJECT.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforcePurgeObject");
        }
    }

    @Override
    public final void enforceSetDatastreamState(Context context,
                                                String pid,
                                                String datastreamId,
                                                String datastreamNewState)
            throws AuthzException {
        try {
            logger.debug("Entered enforceSetDatastreamState");
            String target = Constants.ACTION.SET_DATASTREAM_STATE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.DATASTREAM.NEW_STATE.attributeId;
                resourceAttributes.set(name, datastreamNewState);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceSetDatastreamState");
        }
    }

    @Override
    public final void enforceSetDatastreamVersionable(Context context,
                                                      String pid,
                                                      String datastreamId,
                                                      boolean datastreamNewVersionable)
            throws AuthzException {
        try {
            logger.debug("Entered enforceSetDatastreamVersionable");
            String target = Constants.ACTION.SET_DATASTREAM_VERSIONABLE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.DATASTREAM.NEW_VERSIONABLE.attributeId;
                resourceAttributes.set(name,
                                       new Boolean(datastreamNewVersionable).toString());
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceSetDatastreamVersionable");
        }
    }

    @Override
    public final void enforceCompareDatastreamChecksum(Context context,
                                                       String pid,
                                                       String datastreamId,
                                                       Date versionDate)
            throws AuthzException {
        try {
            logger.debug("Entered enforceCompareDatastreamChecksum");
            String target = Constants.ACTION.COMPARE_DATASTREAM_CHECKSUM.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;

            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.RESOURCE.AS_OF_DATETIME.attributeId;
                resourceAttributes.set(name, ensureDate(versionDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceCompareDatastreamChecksum");
        }
    }

    @Override
    public void enforceDescribeRepository(Context context)
            throws AuthzException {
        try {
            logger.debug("Entered enforceDescribeRepository");
            String target = Constants.ACTION.DESCRIBE_REPOSITORY.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceDescribeRepository");
        }
    }

    @Override
    public void enforceFindObjects(Context context) throws AuthzException {
        try {
            logger.debug("Entered enforceFindObjects");
            String target = Constants.ACTION.FIND_OBJECTS.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceFindObjects");
        }
    }

    @Override
    public void enforceRIFindObjects(Context context) throws AuthzException {
        try {
            logger.debug("Entered enforceRIFindObjects");
            String target = Constants.ACTION.RI_FIND_OBJECTS.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceRIFindObjects");
        }
    }

    @Override
    public void enforceGetDatastreamDissemination(Context context,
                                                  String pid,
                                                  String datastreamId,
                                                  Date asOfDate)
            throws AuthzException {
        try {
            logger.debug("Entered enforceGetDatastreamDissemination");
            String target = Constants.ACTION.GET_DATASTREAM_DISSEMINATION.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.DATASTREAM.ID.attributeId;
                resourceAttributes.set(name, datastreamId);
                name = Constants.RESOURCE.AS_OF_DATETIME.attributeId;
                resourceAttributes.set(name, ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetDatastreamDissemination");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void enforceGetDissemination(Context context,
                                        String pid,
                                        String sDefPid,
                                        String methodName,
                                        Date asOfDate,
                                        String objectState,
                                        String sDefState,
                                        String sDepPid,
                                        String sDepState,
                                        String dissState) throws AuthzException {
        try {
            logger.debug("Entered enforceGetDissemination");
            String target = Constants.ACTION.GET_DISSEMINATION.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            URI name = null;
            try {
                name = Constants.SDEF.PID.attributeId;
                resourceAttributes.set(name, sDefPid);
                name = Constants.SDEF.NAMESPACE.attributeId;
                resourceAttributes.set(name, extractNamespace(sDefPid));
                name = Constants.DISSEMINATOR.METHOD.attributeId;
                resourceAttributes.set(name, methodName);
                name = Constants.SDEP.PID.attributeId;
                resourceAttributes.set(name, sDepPid);
                name = Constants.SDEP.NAMESPACE.attributeId;
                resourceAttributes.set(name, extractNamespace(sDepPid));
                name = Constants.OBJECT.STATE.attributeId;
                resourceAttributes.set(name, objectState);
                name = Constants.DISSEMINATOR.STATE.attributeId;
                resourceAttributes.set(name, dissState);
                name = Constants.SDEF.STATE.attributeId;
                resourceAttributes.set(name, sDefState);
                name = Constants.SDEP.STATE.attributeId;
                resourceAttributes.set(name, sDepState);
                name = Constants.RESOURCE.AS_OF_DATETIME.attributeId;
                resourceAttributes.set(name, ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + name, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetDissemination");
        }
    }

    @Override
    public void enforceGetObjectHistory(Context context, String pid)
            throws AuthzException {
        try {
            logger.debug("Entered enforceGetObjectHistory");
            String target = Constants.ACTION.GET_OBJECT_HISTORY.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetObjectHistory");
        }
    }

    @Override
    public void enforceGetObjectProfile(Context context,
                                        String pid,
                                        Date asOfDate) throws AuthzException {
        try {
            logger.debug("Entered enforceGetObjectProfile");
            String target = Constants.ACTION.GET_OBJECT_PROFILE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.RESOURCE.AS_OF_DATETIME.attributeId,
                                   ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.RESOURCE.AS_OF_DATETIME.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetObjectProfile");
        }
    }

    @Override
    public void enforceListDatastreams(Context context,
                                       String pid,
                                       Date asOfDate) throws AuthzException {
        try {
            logger.debug("Entered enforceListDatastreams");
            String target = Constants.ACTION.LIST_DATASTREAMS.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.RESOURCE.AS_OF_DATETIME.attributeId,
                                   ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.RESOURCE.AS_OF_DATETIME.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceListDatastreams");
        }
    }

    @Override
    public void enforceListMethods(Context context, String pid, Date asOfDate)
            throws AuthzException {
        try {
            logger.debug("Entered enforceListMethods");
            String target = Constants.ACTION.LIST_METHODS.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.RESOURCE.AS_OF_DATETIME.attributeId,
                                   ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.RESOURCE.AS_OF_DATETIME.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceListMethods");
        }
    }

    @Override
    public void enforceServerStatus(Context context) throws AuthzException {
        try {
            logger.debug("Entered enforceServerStatus");
            String target = Constants.ACTION.SERVER_STATUS.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceServerStatus");
        }
    }

    @Override
    public void enforceOAIRespond(Context context) throws AuthzException {
        try {
            logger.debug("Entered enforceOAIRespond");
            String target = Constants.ACTION.OAI.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceOAIRespond");
        }
    }

    @Override
    public void enforceUpload(Context context) throws AuthzException {
        try {
            logger.debug("Entered enforceUpload");
            String target = Constants.ACTION.UPLOAD.uri;
            context.setActionAttributes(null);
            context.setResourceAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceUpload");
        }
    }

    @Override
    public void enforce_Internal_DSState(Context context,
                                         String id,
                                         String state) throws AuthzException {
        try {
            logger.debug("Entered enforce_Internal_DSState");
            String target = Constants.ACTION.INTERNAL_DSSTATE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes
                        .set(Constants.DATASTREAM.ID.attributeId, id);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.DATASTREAM.ID.uri, e);
            }
            try {
                resourceAttributes
                        .set(Constants.DATASTREAM.STATE.attributeId, state);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.DATASTREAM.STATE.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIA.uri,
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforce_Internal_DSState");
        }
    }

    @Override
    public void enforceResolveDatastream(Context context,
                                         Date ticketIssuedDateTime)
            throws AuthzException {
        try {
            logger.debug("Entered enforceResolveDatastream");
            String target = Constants.ACTION.RESOLVE_DATASTREAM.uri;
            context.setResourceAttributes(null);
            MultiValueMap<URI> actionAttributes = new MultiValueMap<URI>();
            try {
                String ticketIssuedDateTimeString =
                        DateUtility.convertDateToString(ticketIssuedDateTime);
                actionAttributes
                        .set(Constants.RESOURCE.TICKET_ISSUED_DATETIME.attributeId,
                                   ticketIssuedDateTimeString);
            } catch (Exception e) {
                context.setActionAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.RESOURCE.TICKET_ISSUED_DATETIME.uri, e);
            }
            context.setActionAttributes(actionAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceResolveDatastream");
        }
    }

    @Override
    public void enforceReloadPolicies(Context context) throws AuthzException {
        try {
            logger.debug("Entered enforceReloadPolicies");
            String target = Constants.ACTION.RELOAD_POLICIES.uri;
            context.setResourceAttributes(null);
            context.setActionAttributes(null);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             "",
                             "",
                             "",
                             context);
        } finally {
            logger.debug("Exiting enforceReloadPolicies");
        }
    }

    public static final String dateAsString(Date date) throws Exception {
        return DateUtility.convertDateToString(date, false);
    }

    @Override
    public void enforceGetRelationships(Context context,
                                        String pid,
                                        String predicate) throws AuthzException {
        try {
            logger.debug("Entered enforceGetRelationships");
            String target = Constants.ACTION.GET_RELATIONSHIPS.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.OBJECT.PID.attributeId,
                                                    pid);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.OBJECT.PID.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceGetRelationships");
        }
    }

    @Override
    public void enforceAddRelationship(Context context,
                                       String pid,
                                       String predicate,
                                       String object,
                                       boolean isLiteral,
                                       String datatype) throws AuthzException {
        try {
            logger.debug("Entered enforceAddRelationship");
            String target = Constants.ACTION.ADD_RELATIONSHIP.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.OBJECT.PID.attributeId,
                                                    pid);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.OBJECT.PID.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforceAddRelationship");
        }
    }

    @Override
    public void enforcePurgeRelationship(Context context,
                                         String pid,
                                         String predicate,
                                         String object,
                                         boolean isLiteral,
                                         String datatype) throws AuthzException {
        try {
            logger.debug("Entered enforcePurgeRelationship");
            String target = Constants.ACTION.PURGE_RELATIONSHIP.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes.set(Constants.OBJECT.PID.attributeId,
                                                    pid);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.OBJECT.PID.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);
        } finally {
            logger.debug("Exiting enforcePurgeRelationship");
        }
    }

    @Override
    public void enforceRetrieveFile(Context context, String fileURI) throws AuthzException {
        try {
            logger.debug("Entered enforceRetrieveFile for {}", fileURI);
            String target = Constants.ACTION.RETRIEVE_FILE.uri;
            context.setActionAttributes(null);
            try {
                MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
                resourceAttributes.set(Constants.DATASTREAM.FILE_URI.attributeId, fileURI);
                context.setResourceAttributes(resourceAttributes);
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't be set " + Constants.DATASTREAM.FILE_URI.uri, e);
            }
            xacmlPep.enforce(context
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
    public void enforceValidate(Context context, String pid, Date asOfDate)
            throws AuthzException {
        try {
            logger.debug("Entered enforceValidate");
            String target = Constants.ACTION.VALIDATE.uri;
            context.setActionAttributes(null);
            MultiValueMap<URI> resourceAttributes = new MultiValueMap<URI>();
            try {
                resourceAttributes
                        .set(Constants.RESOURCE.AS_OF_DATETIME.attributeId,
                                   ensureDate(asOfDate, context));
            } catch (Exception e) {
                context.setResourceAttributes(null);
                throw new AuthzOperationalException(target + " couldn't set "
                                                    + Constants.RESOURCE.AS_OF_DATETIME.uri, e);
            }
            context.setResourceAttributes(resourceAttributes);
            xacmlPep.enforce(context.getSubjectValue(Constants.SUBJECT.LOGIN_ID.uri),
                             target,
                             Constants.ACTION.APIM.uri,
                             pid,
                             extractNamespace(pid),
                             context);

        } finally {
            logger.debug("Exiting enforceValidate");
        }
    }
}
