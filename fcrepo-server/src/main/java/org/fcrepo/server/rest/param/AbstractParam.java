/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.rest.param;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * <p>An abstract class for JAX-RS parameter handlers, providing a single
 * String constructor class for annotated QueryParams and PathParams.</p>
 *
 * <p>Errors during parameter parsing result in a WebApplicationException with a
 * 400 Bad Request status code.</p>
 *
 * @author Coda Hale
 * @author Edwin Shin
 * @version $Id$
 * @see "http://codahale.com/what-makes-jersey-interesting-parameter-classes/"
 */
public abstract class AbstractParam<V> {

    private final V value;

    private final String originalParam;

    public AbstractParam(String param)
            throws WebApplicationException {
        this.originalParam = param;
        try {
            this.value = parse(param);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new WebApplicationException(onError(param, e));
        }
    }

    /**
     * Get the parsed value of the param.
     *
     * @return the parsed value of param.
     */
    public V getValue() {
        return value;
    }

    /**
     * Get the original constructor parameter.
     *
     * @return the original constructor parameter.
     */
    public String getOriginalParam() {
        return originalParam;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    protected abstract V parse(String param) throws Throwable;

    /**
     * Generates an HTTP 400 (Bad Request)
     *
     * @param param the original parameter
     * @param e the original error
     * @return HTTP 400 Bad Request
     */
    protected Response onError(String param, Throwable e) {
        return Response.status(Status.BAD_REQUEST)
                .entity(getErrorMessage(param, e)).build();
    }

    protected String getErrorMessage(String param, Throwable e) {
        return String.format("Invalid parameter: \"%s\". %s", param, e.getMessage());
    }
}