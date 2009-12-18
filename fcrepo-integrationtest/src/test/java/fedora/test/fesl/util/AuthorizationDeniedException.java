
package fedora.test.fesl.util;

public class AuthorizationDeniedException
        extends Exception {

    private static final long serialVersionUID = 2666972149770352182L;

    public AuthorizationDeniedException() {
        super();
    }

    public AuthorizationDeniedException(Exception e) {
        super(e);
    }

    public AuthorizationDeniedException(String msg) {
        super(msg);
    }
}
