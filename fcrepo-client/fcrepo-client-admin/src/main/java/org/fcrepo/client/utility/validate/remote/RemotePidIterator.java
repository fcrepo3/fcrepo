/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.client.utility.validate.remote;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import java.math.BigInteger;

import org.fcrepo.client.utility.validate.ObjectSourceException;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.search.FieldSearchQuery;
import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.search.ObjectFields;



/**
 * Iterate over a collection of object PIDs, defined by:
 * <ul>
 * <li> a connection to a remote server</li>
 * <li>a query</li>.
 * </ul>
 *
 * @author Jim Blake
 */
class RemotePidIterator
        implements Iterator<String> {

    public static final String[] OBJECT_RESULT_FIELDS = new String[] {"pid"};

    public static final BigInteger MAX_FIND_RESULTS =
            new BigInteger("500");

    private static final String SEARCH_NOT_STARTED = "SearchNotStartedYet";

    /** The connection to the server. */
    private final FedoraAPIAMTOM apia;

    /** The query. */
    private final FieldSearchQuery query;

    /**
     * Holding area for the PIDs that have been fetched but not yet handed out
     * in calls to {@link #next()}.
     */
    private final List<String> stash = new ArrayList<String>();

    /**
     * This can be in one of three states:
     * <ul>
     * <li>set to {@link #SEARCH_NOT_STARTED}: we need to call
     * <code>findObjects</code></li>
     * <li>set to non-null value: we need to call
     * <code>resumeFindObjects</code></li>
     * <li>set to null: there are no more PIDs to be fetched.</li>
     * </ul>
     */
    private String token = SEARCH_NOT_STARTED;

    RemotePidIterator(FedoraAPIAMTOM apia, FieldSearchQuery query) {
        this.apia = apia;
        this.query = query;
    }

    /**
     * Check to see whether the stash is empty. Can it be refilled?
     */
    @Override
    public boolean hasNext() {
        try {
            refreshStash();
            return !stash.isEmpty();
        } catch (ObjectSourceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the next item from the stash, if there is one. Maybe refill the stash
     * if there's more on the server.
     */
    @Override
    public String next() {
        if (hasNext()) {
            return stash.remove(0);
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Not supported.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Does the stash have anything in it? If it's empty, try to fill it.
     */
    private void refreshStash() throws ObjectSourceException {
        if (!stash.isEmpty()) {
            return;
        }
        try {
            if (SEARCH_NOT_STARTED.equals(token)) {
                beginSearch();
            } else if (token != null) {
                resumeSearch();
            }
        } catch (RemoteException e) {
            throw new ObjectSourceException(e);
        }
    }

    /**
     * We haven't tried searching yet. Do so, and set the stash and token from
     * the results.
     */
    private void beginSearch() throws RemoteException {
        org.fcrepo.server.types.mtom.gen.FieldSearchQuery genFieldSearchQuery =
                TypeUtility.convertFieldSearchQueryToGenFieldSearchQuery(query);
        org.fcrepo.server.types.mtom.gen.FieldSearchResult searchResult =
                apia.findObjects(org.fcrepo.server.utilities.TypeUtility.convertStringtoAOS(OBJECT_RESULT_FIELDS),
                                 MAX_FIND_RESULTS,
                                 genFieldSearchQuery);
        FieldSearchResult fsr =
                TypeUtility
                        .convertGenFieldSearchResultToFieldSearchResult(searchResult);
        for (ObjectFields fields : fsr.objectFieldsList()) {
            stash.add(fields.getPid());
        }
        token = fsr.getToken();
    }

    /**
     * We are already searching. Use the stored token to continue the search,
     * and set the stash and token from the results.
     */
    private void resumeSearch() throws RemoteException {
        org.fcrepo.server.types.mtom.gen.FieldSearchResult searchResult =
                apia.resumeFindObjects(token);
        FieldSearchResult fsr =
                TypeUtility
                        .convertGenFieldSearchResultToFieldSearchResult(searchResult);
        for (ObjectFields fields : fsr.objectFieldsList()) {
            stash.add(fields.getPid());
        }
        token = fsr.getToken();
    }

}