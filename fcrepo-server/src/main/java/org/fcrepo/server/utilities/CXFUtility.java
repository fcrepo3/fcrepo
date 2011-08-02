/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;

import org.fcrepo.common.FaultException;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.fcrepo.server.errors.authorization.AuthzException;
import org.fcrepo.server.errors.authorization.AuthzOperationalException;
import org.fcrepo.server.errors.authorization.AuthzPermittedException;



/**
 * Utility methods for working with CXF.
 *
 * @author Chris Wilper
 */
public abstract class CXFUtility {

    /**
     * The (SOAP[version-specific] spec-dictated) namespace for fault codes. See
     * http://www.w3.org/TR/SOAP/#_Toc478383510 for SOAPv1.1 and
     * http://www.w3.org/TR/soap12-part1/#faultcodeelement for SOAPv1.2 SOAP
     * v1.2 here.
     */
    public static String SOAP_FAULT_CODE_NAMESPACE =
            "http://www.w3.org/2003/05/soap-envelope";

    /**
     * Similar to above, this is "actor" in soap1_1 and "role" in 1_2. Soap 1.1
     * provides (see http://www.w3.org/TR/SOAP/#_Toc478383499) a special URI for
     * intermediaries, http://schemas.xmlsoap.org/soap/actor/next, and leaves
     * other URIs up to the application. Soap 1.2 provides (see
     * http://www.w3.org/TR/soap12-part1/#soaproles) three special URIs -- one
     * of which is for ultimate receivers, which is the category Fedora falls
     * into. http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver is
     * the URI v1.2 provides.
     */
    public static String SOAP_ULTIMATE_RECEIVER =
            "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";

    public static void throwFault(ServerException se) throws SoapFault {
        throw getFault(se);
    }

    public static SoapFault getFault(ServerException se) {
        String[] details = se.getDetails();
        String detailString = "";
        if (details.length > 0) {
            StringBuffer buf = new StringBuffer();
            for (String element : details) {
                buf.append("<detail>");
                buf.append(element);
                buf.append("</detail>\n");
            }
            detailString = buf.toString();
        }
        SoapFault fault = new SoapFault(detailString, se, SoapFault.FAULT_CODE_CLIENT);
        return fault;
    }

    public static SoapFault getFault(AuthzException e) {
        String reason = "";
        if (e instanceof AuthzOperationalException) {
            reason = AuthzOperationalException.BRIEF_DESC;
        } else if (e instanceof AuthzDeniedException) {
            reason = AuthzDeniedException.BRIEF_DESC;
        } else if (e instanceof AuthzPermittedException) {
            reason = AuthzPermittedException.BRIEF_DESC;
        }
        SoapFault fault = new SoapFault(reason, e, new QName("Authz"));
        return fault;
    }

    public static SoapFault getFault(Throwable th) {
        if (th instanceof ServerException) {
            if (th instanceof AuthzException) {
                return getFault((AuthzException) th);
            } else {
                return getFault((ServerException) th);
            }
        } else {
            if (th instanceof FaultException){
                return new SoapFault(th.toString(), th, SoapFault.FAULT_CODE_CLIENT);
            }
            else {
                return new SoapFault(th.toString(), new Exception("Uncaught exception from Fedora Server",
                                                                  th), SoapFault.FAULT_CODE_CLIENT);
            }
        }
    }

}
