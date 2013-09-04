/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fcrepo.common.Constants;
import org.fcrepo.server.ReadOnlyContext;
import org.fcrepo.server.Server;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOManager;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.cond.EvaluationResult;

/**
 * @author Bill Niebel
 */
class ResourceAttributeFinderModule
        extends AttributeFinderModule {

    private static final Logger logger =
            LoggerFactory.getLogger(ResourceAttributeFinderModule.class);

    static final String OWNER_ID_SEPARATOR_CONFIG_KEY = "OWNER-ID-SEPARATOR";

    static final String DEFAULT_OWNER_ID_SEPARATOR = ",";

    @Override
    protected boolean canHandleAdhoc() {
        return false;
    }

    private String ownerIdSeparator = DEFAULT_OWNER_ID_SEPARATOR;

    static private final ResourceAttributeFinderModule singleton =
            new ResourceAttributeFinderModule();

    private ResourceAttributeFinderModule() {
        super();
        try {
            registerAttribute(Constants.OBJECT.STATE);
            registerAttribute(Constants.OBJECT.OBJECT_TYPE);
            registerAttribute(Constants.OBJECT.OWNER);
            registerAttribute(Constants.OBJECT.CREATED_DATETIME);
            registerAttribute(Constants.OBJECT.LAST_MODIFIED_DATETIME);

            registerAttribute(Constants.DATASTREAM.STATE);
            registerAttribute(Constants.DATASTREAM.CONTROL_GROUP);
            registerAttribute(Constants.DATASTREAM.CREATED_DATETIME);
            registerAttribute(Constants.DATASTREAM.INFO_TYPE);
            registerAttribute(Constants.DATASTREAM.LOCATION_TYPE);
            registerAttribute(Constants.DATASTREAM.MIME_TYPE);
            registerAttribute(Constants.DATASTREAM.CONTENT_LENGTH);
            registerAttribute(Constants.DATASTREAM.FORMAT_URI);
            registerAttribute(Constants.DATASTREAM.LOCATION);

            registerAttribute(Constants.MODEL.HAS_MODEL.getURI(),
                              STRING_ATTRIBUTE_TYPE_URI);

            registerSupportedDesignatorType(AttributeDesignator.RESOURCE_TARGET);
            setInstantiatedOk(true);
        } catch (URISyntaxException e1) {
            setInstantiatedOk(false);
        }
    }

    static public final ResourceAttributeFinderModule getInstance() {
        return singleton;
    }

    private DOManager doManager = null;

    public void setDOManager(DOManager doManager) {
        if (this.doManager == null) {
            this.doManager = doManager;
        }
    }

    public void setLegacyConfiguration(ModuleConfiguration authorizationConfiguration) {
        Map<String,String> moduleParameters = authorizationConfiguration.getParameters();
        if (moduleParameters.containsKey(OWNER_ID_SEPARATOR_CONFIG_KEY)) {
            setOwnerIdSeparator(moduleParameters.get(OWNER_ID_SEPARATOR_CONFIG_KEY));
        }
    }

    public void setOwnerIdSeparator(String ownerIdSeparator) {
        this.ownerIdSeparator = ownerIdSeparator;
        logger.debug("resourceAttributeFinder just set ownerIdSeparator ==[{}]",
                this.ownerIdSeparator);
    }

    private final String getDatastreamId(EvaluationCtx context) {
        URI datastreamIdUri = Constants.DATASTREAM.ID.attributeId;

        EvaluationResult attribute =
                context.getResourceAttribute(STRING_ATTRIBUTE_TYPE_URI,
                                             datastreamIdUri,
                                             null);

        Object element = getAttributeFromEvaluationResult(attribute);
        if (element == null) {
            logger.debug("getDatastreamId: " + " exit on "
                    + "can't get resource-id on request callback");
            return null;
        }

        if (!(element instanceof StringAttribute)) {
            logger.debug("getDatastreamId: " + " exit on "
                    + "couldn't get resource-id from xacml request "
                    + "non-string returned");
            return null;
        }

        String datastreamId = ((StringAttribute) element).getValue();

        if (datastreamId == null) {
            logger.debug("getDatastreamId: " + " exit on " + "null resource-id");
            return null;
        }

        if (!validDatastreamId(datastreamId)) {
            logger.debug("invalid resource-id: datastreamId is not valid");
            return null;
        }

        return datastreamId;
    }

    private final boolean validDatastreamId(String datastreamId) {
        if (datastreamId == null) {
            return false;
        }
        // "" is a valid resource id, for it represents a don't-care condition
        if (" ".equals(datastreamId)) {
            return false;
        }
        return true;
    }

    @Override
    protected final Object getAttributeLocally(int designatorType,
                                               URI attributeId,
                                               URI resourceCategory,
                                               EvaluationCtx context) {

        long getAttributeStartTime = (logger.isDebugEnabled()) ? System.currentTimeMillis() : 0;

        try {
            String pid = PolicyFinderModule.getPid(context);
            if ("".equals(pid)) {
                logger.debug("no pid");
                return null;
            }
            logger.debug("getResourceAttribute {}, pid={}", attributeId, pid);
            DOReader reader = null;
            try {
                logger.debug("pid={}", pid);
                reader =
                        doManager.getReader(Server.USE_DEFINITIVE_STORE,
                                            ReadOnlyContext.EMPTY,
                                            pid);
            } catch (ServerException e) {
                logger.debug("couldn't get object reader");
                return null;
            }
            String[] values = null;
            if (Constants.OBJECT.STATE.attributeId.equals(attributeId)) {
                try {
                    values = new String[1];
                    values[0] = reader.GetObjectState();
                    logger.debug("got {}={}", Constants.OBJECT.STATE.uri, values[0]);
                } catch (ServerException e) {
                    logger.debug("failed getting {}", Constants.OBJECT.STATE.uri,e);
                    return null;
                }
            }
            else if (Constants.OBJECT.OWNER.attributeId.equals(attributeId)) {
                try {
                    logger.debug("ResourceAttributeFinder.getAttributeLocally using ownerIdSeparator==[{}]",
                                    ownerIdSeparator);
                    String ownerId = reader.getOwnerId();
                    if (ownerId == null) {
                        values = new String[0];
                    } else {
                        values = reader.getOwnerId().split(ownerIdSeparator);
                    }
                    if (logger.isDebugEnabled()) {
                        String temp = "got " + Constants.OBJECT.OWNER.uri + "=";
                        for (int i = 0; i < values.length; i++) {
                            temp += (" [" + values[i] + "]");
                        }
                        logger.debug(temp);
                    }
                } catch (ServerException e) {
                    logger.debug("failed getting {}", Constants.OBJECT.OWNER.uri,e);
                    return null;
                }
            } else if (Constants.MODEL.HAS_MODEL.attributeId.equals(attributeId)) {
                Set<String> models = new HashSet<String>();
                try {
                    models.addAll(reader.getContentModels());
                } catch (ServerException e) {
                    logger.debug("failed getting {}", Constants.MODEL.HAS_MODEL.uri,e);
                    return null;
                }
                values = models.toArray(new String[0]);
            } else if (Constants.OBJECT.CREATED_DATETIME.attributeId.equals(attributeId)) {
                try {
                    values = new String[1];
                    values[0] =
                            DateUtility.convertDateToString(reader
                                    .getCreateDate());
                    logger.debug("got {}={}", Constants.OBJECT.CREATED_DATETIME.uri,
                            values[0]);
                } catch (ServerException e) {
                    logger.debug("failed getting {}",
                            Constants.OBJECT.CREATED_DATETIME.uri);
                    return null;
                }
            } else if (Constants.OBJECT.LAST_MODIFIED_DATETIME.attributeId
                    .equals(attributeId)) {
                try {
                    values = new String[1];
                    values[0] =
                            DateUtility.convertDateToString(reader
                                    .getLastModDate());
                    logger.debug("got {}={}",
                            Constants.OBJECT.LAST_MODIFIED_DATETIME.uri,
                            values[0]);
                } catch (ServerException e) {
                    logger.debug("failed getting {}",
                            Constants.OBJECT.LAST_MODIFIED_DATETIME.uri);
                    return null;
                }
            } else if (Constants.DATASTREAM.STATE.attributeId.equals(attributeId)
                    || Constants.DATASTREAM.CONTROL_GROUP.attributeId
                            .equals(attributeId)
                    || Constants.DATASTREAM.FORMAT_URI.attributeId.equals(attributeId)
                    || Constants.DATASTREAM.CREATED_DATETIME.attributeId
                            .equals(attributeId)
                    || Constants.DATASTREAM.INFO_TYPE.attributeId.equals(attributeId)
                    || Constants.DATASTREAM.LOCATION.attributeId.equals(attributeId)
                    || Constants.DATASTREAM.LOCATION_TYPE.attributeId
                            .equals(attributeId)
                    || Constants.DATASTREAM.MIME_TYPE.attributeId.equals(attributeId)
                    || Constants.DATASTREAM.CONTENT_LENGTH.attributeId
                            .equals(attributeId)) {
                String datastreamId = getDatastreamId(context);
                if ("".equals(datastreamId)) {
                    logger.debug("no datastreamId");
                    return null;
                }
                logger.debug("datastreamId={}", datastreamId);
                Datastream datastream;
                try {
                    datastream = reader.GetDatastream(datastreamId, new Date()); //right import (above)?
                } catch (ServerException e) {
                    logger.debug("couldn't get datastream");
                    return null;
                }
                if (datastream == null) {
                    logger.debug("got null datastream");
                    return null;
                }

                if (Constants.DATASTREAM.STATE.attributeId.equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSState;
                } else if (Constants.DATASTREAM.CONTROL_GROUP.uri
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSControlGrp;
                } else if (Constants.DATASTREAM.FORMAT_URI.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSFormatURI;
                } else if (Constants.DATASTREAM.CREATED_DATETIME.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] =
                            DateUtility
                                    .convertDateToString(datastream.DSCreateDT);
                } else if (Constants.DATASTREAM.INFO_TYPE.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSInfoType;
                } else if (Constants.DATASTREAM.LOCATION.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSLocation;
                } else if (Constants.DATASTREAM.LOCATION_TYPE.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSLocationType;
                } else if (Constants.DATASTREAM.MIME_TYPE.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = datastream.DSMIME;
                } else if (Constants.DATASTREAM.CONTENT_LENGTH.attributeId
                        .equals(attributeId)) {
                    values = new String[1];
                    values[0] = Long.toString(datastream.DSSize);
                } else {
                    logger.debug("looking for unknown resource attribute={}",
                            attributeId);
                }
            } else {
                logger.debug("looking for unknown resource attribute={}",
                        attributeId);
            }
            return values;
        } finally {
            if (logger.isDebugEnabled()) {
                long dur = System.currentTimeMillis() - getAttributeStartTime;
                logger.debug("Locally getting the '{}' attribute for this resource took {}ms.",
                    attributeId, dur);
            }
        }
    }

}
