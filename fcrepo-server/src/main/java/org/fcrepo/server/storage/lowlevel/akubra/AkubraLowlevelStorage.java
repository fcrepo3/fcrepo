/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.lowlevel.akubra;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import org.akubraproject.Blob;
import org.akubraproject.BlobStore;
import org.akubraproject.BlobStoreConnection;
import org.akubraproject.DuplicateBlobException;
import org.akubraproject.MissingBlobException;
import org.apache.commons.io.IOUtils;
import org.fcrepo.common.Constants;
import org.fcrepo.common.FaultException;
import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.PID;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ObjectAlreadyInLowlevelStorageException;
import org.fcrepo.server.errors.ObjectNotInLowlevelStorageException;
import org.fcrepo.server.storage.lowlevel.IListable;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.storage.lowlevel.ISizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Akubra-backed implementation of ILowlevelStorage.
 * <p>
 * This implementation uses two Akubra <code>BlobStore</code>s; one for
 * objects and another for datastreams.
 *
 * @author Chris Wilper
 */
public class AkubraLowlevelStorage
        implements ILowlevelStorage, IListable, ISizable {

    private static final Logger logger =
            LoggerFactory.getLogger(AkubraLowlevelStorage.class);

    private final BlobStore objectStore;

    private final BlobStore datastreamStore;

    private final boolean forceSafeObjectOverwrites;

    private final boolean forceSafeDatastreamOverwrites;

    /**
     * Creates an instance using the given blob stores.
     * <p>
     * The blob stores <b>MUST</b>:
     * <ul>
     *   <li> be <i>non-transactional</i></li>
     *   <li> be able to accept <code>info:fedora/</code> URIs as blob ids.
     * </ul>
     * <p>
     * The blob stores <b>MAY</b>:
     * <ul>
     *   <li> support atomic overwrites natively. If not,
     *        <code>forceSafe..Overwrites</code> MUST be given as
     *        <code>true</code> and the blob store MUST support
     *        {@link org.akubraproject.core.Blob#renameTo}
     *   </li>
     * </ul>
     *
     * @param objectStore the store for serialized objects.
     * @param datastreamStore the store for datastream content.
     * @param forceSafeObjectOverwrites if true, replaceObject calls will
     *        be done in a way that ensures the old content is not deleted
     *        until the new content is safely written. If the objectStore
     *        already does this, this should be given as false.
     * @param forceSafeDatastreamOverwrites same as above, but for
     *        replaceDatastream calls.
     */
    public AkubraLowlevelStorage(BlobStore objectStore,
                                 BlobStore datastreamStore,
                                 boolean forceSafeObjectOverwrites,
                                 boolean forceSafeDatastreamOverwrites) {
        this.objectStore = objectStore;
        this.datastreamStore = datastreamStore;
        this.forceSafeObjectOverwrites = forceSafeObjectOverwrites;
        this.forceSafeDatastreamOverwrites = forceSafeDatastreamOverwrites;
    }

    //
    // ILowlevelStorage methods
    //

    public long addDatastream(String dsKey, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        return add(datastreamStore, dsKey, content, hints);
    }

    public long addDatastream(String pid, InputStream content) throws LowlevelStorageException {
        return this.addDatastream(pid, content, null);
    }

    public void addObject(String objectKey, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        add(objectStore, objectKey, content, hints);
    }

    public void addObject(String pid, InputStream content) throws LowlevelStorageException {
        this.addObject(pid, content, null);
    }

    @Override
    public void auditDatastream() throws LowlevelStorageException {
        audit(datastreamStore);
    }

    @Override
    public void auditObject() throws LowlevelStorageException {
        audit(objectStore);
    }

    @Override
    public void rebuildDatastream() throws LowlevelStorageException {
        rebuild(datastreamStore);
    }

    @Override
    public void rebuildObject() throws LowlevelStorageException {
        rebuild(objectStore);
    }

    @Override
    public void removeDatastream(String dsKey)
            throws LowlevelStorageException {
        remove(datastreamStore, dsKey);
    }

    @Override
    public void removeObject(String objectKey)
            throws LowlevelStorageException {
        remove(objectStore, objectKey);
    }

    public long replaceDatastream(String dsKey, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        return replace(datastreamStore, dsKey, content, forceSafeDatastreamOverwrites, hints);
    }
    
    public long replaceDatastream(String pid, InputStream content) throws LowlevelStorageException {
        return this.replaceDatastream(pid, content, null);
    }

    public void replaceObject(String objectKey, InputStream content, Map<String, String> hints)
            throws LowlevelStorageException {
        replace(objectStore, objectKey, content, forceSafeObjectOverwrites, hints);
    }
    public void replaceObject(String pid, InputStream content) throws LowlevelStorageException {
        this.replaceObject(pid, content, null);
    }

    @Override
    public InputStream retrieveDatastream(String dsKey)
            throws LowlevelStorageException {
        return retrieve(datastreamStore, dsKey);
    }

    @Override
    public InputStream retrieveObject(String objectKey)
            throws LowlevelStorageException {
        return retrieve(objectStore, objectKey);
    }

    //
    // IListable methods
    //

    @Override
    public Iterator<String> listDatastreams() {
        return list(datastreamStore);
    }

    @Override
    public Iterator<String> listObjects() {
        return list(objectStore);
    }


    //
    // ISizable methods
    //
    @Override
    public long getDatastreamSize(String dsKey) throws LowlevelStorageException {
        return getSize(datastreamStore, dsKey);
    }

    //
    // Private implementation methods
    //

    private static long add(BlobStore store,
                            String key,
                            InputStream content, Map<String, String> hints)
            throws ObjectAlreadyInLowlevelStorageException {
        BlobStoreConnection connection = null;
        try {
            URI blobId = getBlobId(key);
            connection = getConnection(store, hints);
            Blob blob = getBlob(connection, blobId, hints);
            OutputStream out = openOutputStream(blob, -1, false);
            copy(content, out);
            try {
                return blob.getSize();
            } catch (MissingBlobException e) { // should never happen
                throw new RuntimeException("Missing blob after adding the blob: " +e.getMessage(), e);
            } catch (IOException e) { // should also never happen
                throw new RuntimeException("Error reading blob size: " +e.getMessage(), e);
            }
        } catch (DuplicateBlobException e) {
            throw new ObjectAlreadyInLowlevelStorageException(key, e);
        } finally {
            closeConnection(connection);
        }
    }

    private static void audit(BlobStore store) {
        // N/A: Akubra does not trigger consistency checks of a store's
        // internal index. If necessary, such a check must be done out-of-band.
    }

    private static void rebuild(BlobStore store) {
        // N/A: Akubra does not trigger rebuilds of a store's internal index.
        // If necessary, such a rebuild must be done out-of-band.
    }

    private static void remove(BlobStore store,
                               String key)
            throws ObjectNotInLowlevelStorageException {
        BlobStoreConnection connection = null;
        try {
            URI blobId = getBlobId(key);
            connection = getConnection(store, null);
            Blob blob = getBlob(connection, blobId, null);
            if (exists(blob)) {
                delete(blob);
            } else {
                throw new ObjectNotInLowlevelStorageException("Object not found in low-level storage: " + key);
            }
        } finally {
            closeConnection(connection);
        }
    }

    private static long replace(BlobStore store,
                                String key,
                                InputStream content,
                                boolean forceSafeOverwrite,
                                Map<String, String> hints)
            throws LowlevelStorageException {
        BlobStoreConnection connection = null;
        try {
            URI blobId = getBlobId(key);
            connection = getConnection(store, hints);
            Blob blob = getBlob(connection, blobId, null);
            if (exists(blob)) {
                if (forceSafeOverwrite) {
                    safeOverwrite(blob, content);
                } else {
                    // leave it to the store impl to ensure atomicity
                    OutputStream out = openOutputStream(blob, -1, true);
                    copy(content, out);
                }
            } else {
                throw new ObjectNotInLowlevelStorageException("Object not found in low-level storage: " + key);
            }
            try {
                return blob.getSize();
            } catch (MissingBlobException e) { // should never happen
                throw new RuntimeException("Missing blob after replcaing the blob: " +e.getMessage(), e);
            } catch (IOException e) { // should also never happen
                throw new RuntimeException("Error reading blob size: " +e.getMessage(), e);
            }
        } catch (DuplicateBlobException wontHappen) {
            throw new FaultException(wontHappen);
        } finally {
            closeConnection(connection);
        }
    }

    private static Iterator<String> list(BlobStore store) {
        BlobStoreConnection connection = null;
        boolean successful = false;
        try {
            connection = getConnection(store, null);
            Iterator<URI> blobIds = listBlobIds(connection);
            successful = true;
            return new ConnectionClosingKeyIterator(connection, blobIds);
        } finally {
            if (!successful) {
                closeConnection(connection);
            }
        }
    }

    /**
     * Overwrites the content of the given blob in a way that guarantees the
     * original content is not destroyed until the replacement is successfully
     * put in its place.
     */
    private static void safeOverwrite(Blob origBlob, InputStream content) {
        BlobStoreConnection connection = origBlob.getConnection();
        String origId = origBlob.getId().toString();

        // write new content to origId/new
        Blob newBlob = null;
        try {
            newBlob = connection.getBlob(new URI(origId + "/new"), null);
            copy(content, newBlob.openOutputStream(-1, false));
        } catch (Throwable th) {
            // any error or exception here is an unrecoverable fault
            throw new FaultException(th);
        }

        // At this point, we have origId (with old content) and origId/new

        // rename origId to origId/old
        Blob oldBlob = null;
        try {
            oldBlob = rename(origBlob, origId + "/old");
        } finally {
            if (oldBlob == null) {
                // rename failed; attempt recovery before throwing the fault
                try {
                    delete(newBlob);
                } catch (Throwable th) {
                    logger.error("Failed to delete " + newBlob.getId() + " while"
                              + " recovering from rename failure during safe"
                              + " overwrite", th);
                }
            }
        }

        // At this point, we have origId/old and origId/new

        // rename origId/new to origId
        boolean successful = false;
        try {
            rename(newBlob, origId);
            successful = true;
        } finally {
            if (!successful) {
                // rename failed; attempt recovery before throwing the fault
                try {
                    rename(oldBlob, origId);
                } catch (Throwable th) {
                    logger.error("Failed to rename " + oldBlob.getId() + " to "
                              + origId + " while recovering from rename"
                              + " failure during safe overwrite", th);
                }
                try {
                    newBlob.delete();
                } catch (Throwable th) {
                    logger.error("Failed to delete " + newBlob.getId()
                              + " while recovering from rename"
                              + " failure during safe overwrite", th);
                }
            }
        }

        // At this point, we have origId (with new content) and origId/old

        // remove origId/old; we don't need it anymore
        try {
            delete(oldBlob);
        } catch (Throwable th) {
            logger.error("Failed to delete " + oldBlob.getId()
                    + " while cleaning up after committed"
                    + " safe overwrite", th);
        }
    }

    private static Blob rename(Blob blob, String newId) {
        try {
            return blob.moveTo(new URI(newId), null);
        } catch (IOException e) {
            throw new FaultException(e);
        } catch (URISyntaxException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    private static InputStream retrieve(BlobStore store,
                                        String key)
            throws ObjectNotInLowlevelStorageException {
        BlobStoreConnection connection = null;
        InputStream content = null;
        boolean successful = false;
        try {
            URI blobId = getBlobId(key);
            connection = getConnection(store, null);
            Blob blob = getBlob(connection, blobId, null);
            content = openInputStream(blob);
            successful = true;
            return new ConnectionClosingInputStream(connection, content);
        } catch (MissingBlobException e) {
            throw new ObjectNotInLowlevelStorageException("Object not found in low-level storage: " + key);
        } finally {
            if (!successful) {
                IOUtils.closeQuietly(content);
                closeConnection(connection);
            }
        }
    }

    private static long getSize(BlobStore store,
                                        String key)
            throws ObjectNotInLowlevelStorageException {
        BlobStoreConnection connection = null;
        boolean successful = false;
        try {
            URI blobId = getBlobId(key);
            connection = getConnection(store, null);
            Blob blob = getBlob(connection, blobId, null);
            return blob.getSize();
        } catch (MissingBlobException e) {
            throw new ObjectNotInLowlevelStorageException("Object not found in low-level storage: " + key);
        } catch (IOException e) { // should never happen
            throw new RuntimeException("Error reading blob size: " +e.getMessage(), e);
        } finally {
            if (!successful) {
                closeConnection(connection);
            }
        }
    }


    private static BlobStoreConnection getConnection(BlobStore store, Map<String, String> hints) {
        try {
            return store.openConnection(null, hints);
        } catch (IOException e) {
            throw new FaultException(
                    "System error getting blob store connection", e);
        }
    }

    private static void closeConnection(BlobStoreConnection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Throwable th) {
            logger.warn("Unexpected error closing blob store connection", th);
        }
    }

    private static Blob getBlob(BlobStoreConnection connection,
                                URI blobId,
                                Map<String, String> hints) {
        try {
            return connection.getBlob(blobId, hints);
        } catch (Exception e) {
            logger.error(e.toString(),e);
            throw new FaultException("System error getting blob handle", e);
        }
    }

    private static InputStream openInputStream(Blob blob)
            throws MissingBlobException {
        try {
            return blob.openInputStream();
        } catch (MissingBlobException e) { // subclass of IOException
            throw e;
        } catch (IOException e) {
            logger.error(e.toString(),e);
            throw new FaultException("System error opening input stream", e);
        }
    }

    private static OutputStream openOutputStream(Blob blob,
                                                 long estimatedSize,
                                                 boolean overwrite)
            throws DuplicateBlobException {
        try {
            return blob.openOutputStream(estimatedSize, overwrite);
        } catch (DuplicateBlobException e) { // subclass of IOException
            throw e;
        } catch (IOException e) {
            logger.error(e.toString(),e);
            throw new FaultException("System error opening output stream", e);
        }
    }

    private static boolean exists(Blob blob) {
        try {
            return blob.exists();
        } catch (IOException e) {
            logger.error(e.toString(),e);
            throw new FaultException(
                    "System error determining existence of blob", e);
        }
    }

    private static void delete(Blob blob) {
        try {
            if (blob.exists()){
                blob.delete();
            }
            else {
                logger.warn("Attempted to delete non-existent blob " + blob.getCanonicalId());
            }
        } catch (IOException e) {
            logger.error(e.toString(),e);
            throw new FaultException("System error deleting blob", e);
        }
    }

    private static Iterator<URI> listBlobIds(BlobStoreConnection connection) {
        try {
            return connection.listBlobIds(null); // all
        } catch (IOException e) {
            logger.error(e.toString(),e);
            throw new FaultException("System error listing blob ids", e);
        }
    }

    private static long copy(InputStream source, OutputStream sink) {
        try {
            return IOUtils.copyLarge(source, sink);
        } catch (IOException e) {
            logger.error(e.toString(),e);
            throw new FaultException("System error copying stream", e);
        } finally {
            IOUtils.closeQuietly(source);
            IOUtils.closeQuietly(sink);
        }
    }

    /**
     * Converts a token to a token-as-blobId.
     * <p>
     * Object tokens are simply prepended with <code>info:fedora/</code>,
     * whereas datastream tokens are additionally converted such that
     * <code>ns:id+dsId+dsVersionId</code> becomes
     * <code>info:fedora/ns:id/dsId/dsVersionId</code>, with the dsId
     * and dsVersionId segments URI-percent-encoded with UTF-8 character
     * encoding.
     *
     * @param token the token to convert.
     * @return the blob id.
     * @throws IllegalArgumentException if the token is not a well-formed
     *         pid or datastream token.
     */
    private static URI getBlobId(String token) {
        try {
            int i = token.indexOf('+');
            if (i == -1) {
                return new URI(new PID(token).toURI());
            } else {
                String[] dsParts = token.substring(i + 1).split("\\+");
                if (dsParts.length != 2) {
                    throw new IllegalArgumentException(
                            "Malformed datastream token: " + token);
                }
                return new URI(Constants.FEDORA.uri
                             + token.substring(0, i) + "/"
                             + uriEncode(dsParts[0]) + "/"
                             + uriEncode(dsParts[1]));
            }
        } catch (MalformedPIDException e) {
            throw new IllegalArgumentException(
                    "Malformed object token: " + token, e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Malformed object or datastream token: " + token, e);
        }
    }

    /**
     * Converts a token-as-blobId back to a token.
     *
     * @param blobId the blobId to convert.
     * @return the resulting object or datastream token.
     */
    private static String getToken(URI blobId) {
        String[] parts = blobId.getSchemeSpecificPart().split("/");
        if (parts.length == 2) {
            return parts[1];
        } else if (parts.length == 4) {
            return parts[1] + "+"  + uriDecode(parts[2]) + "+"
                    + uriDecode(parts[3]);
        } else {
            throw new IllegalArgumentException("Malformed token-as-blobId: "
                    + blobId);
        }
    }

    private static String uriEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    private static String uriDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    /**
     * Closes the stream and connection automatically when closed or finalized.
     */
    static class ConnectionClosingInputStream extends FilterInputStream {

        private final BlobStoreConnection connection;

        public ConnectionClosingInputStream(BlobStoreConnection connection,
                                            InputStream wrapped) {
            super(wrapped);
            this.connection = connection;
        }

        @Override
        public void close() {
            if (!connection.isClosed()) {
                try {
                    super.close();
                } catch (IOException e) {
                    throw new FaultException("System error closing stream", e);
                } finally {
                    connection.close();
                }
            }
        }

        @Override
        protected void finalize() {
            close();
        }

    }

    /**
     * Converts a blob id iterator to a key iterator and closes the
     * connection automatically when exhausted or finalized.
     */
    static class ConnectionClosingKeyIterator implements Iterator<String> {

        private final BlobStoreConnection connection;
        private final Iterator<URI> blobIds;

        public ConnectionClosingKeyIterator(BlobStoreConnection connection,
                                            Iterator<URI> blobIds) {
            this.connection = connection;
            this.blobIds = blobIds;
        }

        @Override
        public boolean hasNext() {
            if (!blobIds.hasNext()) {
                connection.close();
                return false;
            }
            return true;
        }

        @Override
        public String next() {
            return getToken(blobIds.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void finalize() {
            connection.close();
        }

    }


}
