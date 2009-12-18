/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.utilities;

import org.xml.sax.helpers.DefaultHandler;

public class ParserUtilityHandler
        extends DefaultHandler {

    protected Object parm1 = null;

    protected Object parm2 = null;

    protected Object parm3 = null;

    protected Object parm4 = null;

    protected Object result = null;

    public ParserUtilityHandler() {
    }

    public ParserUtilityHandler(Object p1) {
        parm1 = p1;
    }

    public ParserUtilityHandler(Object p1, Object p2) {
        parm1 = p1;
        parm2 = p2;
    }

    public ParserUtilityHandler(Object p1, Object p2, Object p3) {
        parm1 = p1;
        parm2 = p2;
        parm3 = p3;
    }

    public ParserUtilityHandler(Object p1, Object p2, Object p3, Object p4) {
        parm1 = p1;
        parm2 = p2;
        parm3 = p3;
        parm4 = p4;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(String string) {
        result = string;
    }

}
