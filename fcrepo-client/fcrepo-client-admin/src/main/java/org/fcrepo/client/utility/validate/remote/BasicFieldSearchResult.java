/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.client.utility.validate.remote;

import java.util.Date;
import java.util.List;

import org.fcrepo.server.search.FieldSearchResult;
import org.fcrepo.server.search.ObjectFields;


/**
 * An simple instantiation of {@link FieldSearchResult} to correspond to the
 * WSDL-style
 * {@link org.fcrepo.server.types.gen.FieldSearchResult FieldSearchResult}.
 * 
 * @author Jim Blake
 */
class BasicFieldSearchResult
        implements FieldSearchResult {

    private final long completeListSize;

    private final long cursor;

    private final Date expirationDate;

    private final String token;

    private final List<ObjectFields> objectFields;

    public BasicFieldSearchResult(long completeListSize,
                                  long cursor,
                                  Date expirationDate,
                                  String token,
                                  List<ObjectFields> objectFields) {
        this.completeListSize = completeListSize;
        this.cursor = cursor;
        this.expirationDate = expirationDate;
        this.token = token;
        this.objectFields = objectFields;
    }

    public long getCompleteListSize() {
        return completeListSize;
    }

    public long getCursor() {
        return cursor;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getToken() {
        return token;
    }

    public List<ObjectFields> objectFieldsList() {
        return objectFields;
    }

}
