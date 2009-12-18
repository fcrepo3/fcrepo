/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.remote;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.rpc.ServiceException;

import fedora.client.FedoraClient;
import fedora.client.utility.validate.InvalidContentModelException;
import fedora.client.utility.validate.ObjectSource;
import fedora.client.utility.validate.ObjectSourceException;
import fedora.client.utility.validate.types.BasicContentModelInfo;
import fedora.client.utility.validate.types.BasicObjectInfo;
import fedora.client.utility.validate.types.ContentModelInfo;
import fedora.client.utility.validate.types.DatastreamInfo;
import fedora.client.utility.validate.types.DsCompositeModelDoc;
import fedora.client.utility.validate.types.ObjectInfo;
import fedora.client.utility.validate.types.RelationshipInfo;

import fedora.server.access.FedoraAPIA;
import fedora.server.management.FedoraAPIM;
import fedora.server.search.FieldSearchQuery;
import fedora.server.types.gen.Datastream;
import fedora.server.types.gen.MIMETypedStream;

import static fedora.client.utility.validate.types.ContentModelInfo.DS_COMPOSITE_MODEL;
import static fedora.client.utility.validate.types.ContentModelInfo.DS_COMPOSITE_MODEL_FORMAT;

/**
 * An {@link ObjectSource} that is based on a {@link FedoraClient} link to a
 * remote server.
 * 
 * @author Jim Blake
 */
public class RemoteObjectSource
        implements ObjectSource {

    private final FedoraAPIA apia;

    private final FedoraAPIM apim;

    public RemoteObjectSource(ServiceInfo serviceInfo)
            throws ServiceException, IOException {
        FedoraClient fc =
                new FedoraClient(serviceInfo.getBaseUrlString(), serviceInfo
                        .getUsername(), serviceInfo.getPassword());
        apia = fc.getAPIA();
        apim = fc.getAPIM();
    }

    /**
     * Get a series of PIDs, representing all digital objects in the repository
     * that satisfy the specified query.
     */
    public Iterator<String> findObjectPids(FieldSearchQuery query)
            throws ObjectSourceException {
        return new RemotePidIterator(apia, query);
    }

    /**
     * {@inheritDoc}
     */
    public ObjectInfo getValidationObject(String pid)
            throws ObjectSourceException {
        List<RelationshipInfo> relations = getRelationships(pid);
        Set<DatastreamInfo> dsDefs = getDatastreams(pid);
        return new BasicObjectInfo(pid, relations, dsDefs);
    }

    private Set<DatastreamInfo> getDatastreams(String pid)
            throws ObjectSourceException {
        try {
            Datastream[] datastreams = apim.getDatastreams(pid, null, null);
            return TypeUtility
                    .convertGenDatastreamArrayToDatastreamInfoSet(datastreams);
        } catch (RemoteException e) {
            throw new ObjectSourceException(e);
        }
    }

    private List<RelationshipInfo> getRelationships(String pid)
            throws ObjectSourceException {
        try {
            fedora.server.types.gen.RelationshipTuple[] tuples =
                    apim.getRelationships(pid, null);
            return TypeUtility
                    .convertGenRelsTupleArrayToRelationshipInfoList(tuples);
        } catch (RemoteException e) {
            throw new ObjectSourceException(e);
        }
    }

    /**
     * A content model must exist as an object. If must have a (@link
     * DS_COMPOSITE_MODEL} datastream.
     */
    public ContentModelInfo getContentModelInfo(String pid)
            throws ObjectSourceException, InvalidContentModelException {
        try {
            ObjectInfo object = getValidationObject(pid);
            if (object == null) {
                return null;
            }

            DatastreamInfo dsInfo =
                    object.getDatastreamInfo(DS_COMPOSITE_MODEL);
            if (dsInfo == null) {
                throw new InvalidContentModelException(pid,
                                                       "Content model has no '"
                                                               + DS_COMPOSITE_MODEL
                                                               + "' datastream.");
            }

            if (!DS_COMPOSITE_MODEL_FORMAT.equals(dsInfo.getFormatUri())) {
                throw new InvalidContentModelException(pid, "Datastream '"
                        + DS_COMPOSITE_MODEL + "' has incorrect format URI: '"
                        + dsInfo.getFormatUri() + "'.");
            }

            MIMETypedStream ds =
                    apia.getDatastreamDissemination(pid,
                                                    DS_COMPOSITE_MODEL,
                                                    null);
            DsCompositeModelDoc model =
                    new DsCompositeModelDoc(pid, ds.getStream());
            return new BasicContentModelInfo(object, model.getTypeModels());
        } catch (RemoteException e) {
            throw new ObjectSourceException("Problem fetching '"
                                                    + DS_COMPOSITE_MODEL
                                                    + "' datastream for pid='"
                                                    + pid + "'",
                                            e);
        }
    }
}
