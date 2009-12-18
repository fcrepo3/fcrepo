/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.rest;

public class RestException extends RuntimeException {
    private static final long serialVersionUID = -4755958876044027618L;

    public RestException() {
        super();
    }

    public RestException(String msg, Throwable ex) {
        super(msg, ex);
    }

    public RestException(String msg) {
        super(msg);
    }

    public RestException(Throwable ex) {
        super(ex);
    }
}
