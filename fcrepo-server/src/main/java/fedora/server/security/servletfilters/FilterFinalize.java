/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Niebel
 */
public class FilterFinalize
        extends FilterSetup {

    protected static Log log = LogFactory.getLog(FilterFinalize.class);

    private static final boolean AUTHENTICATION_REQUIRED_DEFAULT = true;

    private boolean AUTHENTICATION_REQUIRED = AUTHENTICATION_REQUIRED_DEFAULT;

    private static final String AUTHENTICATION_REQUIRED_KEY =
            "authentication-required";

    private static final String REQUEST_ATTRIBUTE_INPUT_NAME_DEFAULT =
            "FEDORA_AUX_SUBJECT_ATTRIBUTES";

    private String REQUEST_ATTRIBUTE_INPUT_NAME =
            REQUEST_ATTRIBUTE_INPUT_NAME_DEFAULT;

    private static final String REQUEST_ATTRIBUTE_INPUT_NAME_KEY =
            "request-attribute-input-key";

    private static final String REQUEST_ATTRIBUTE_INPUT_AUTHORITY_DEFAULT =
            "auxsubject";

    private String REQUEST_ATTRIBUTE_INPUT_AUTHORITY =
            REQUEST_ATTRIBUTE_INPUT_AUTHORITY_DEFAULT;

    private static final String REQUEST_ATTRIBUTE_INPUT_AUTHORITY_KEY =
            "request-attribute-input-authority";

    private static final String DELIVERY_NAME_DEFAULT =
            REQUEST_ATTRIBUTE_INPUT_NAME_DEFAULT;

    private final String DELIVERY_NAME = DELIVERY_NAME_DEFAULT;

    private static final String[] URLS_DEFAULT = {"/.*"};

    private String[] URLS = URLS_DEFAULT;

    private static final String URLS_KEY = "authentication-urls";

    @Override
    protected void initThisSubclass(String key, String value) {
        log.debug("FAT.iTS");
        String method = "initThisSubclass() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        boolean setLocally = false;
        if (AUTHENTICATION_REQUIRED_KEY.equals(key)) {
            try {
                AUTHENTICATION_REQUIRED = booleanValue(value);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(format(method, "bad value", key, value));
                }
                initErrors = true;
            }
            setLocally = true;
        } else if (REQUEST_ATTRIBUTE_INPUT_NAME_KEY.equals(key)) {
            REQUEST_ATTRIBUTE_INPUT_NAME = value;
            setLocally = true;
        } else if (REQUEST_ATTRIBUTE_INPUT_AUTHORITY_KEY.equals(key)) {
            REQUEST_ATTRIBUTE_INPUT_AUTHORITY = value;
            setLocally = true;
        } else if (URLS_KEY.equals(key)) {
            String temp = value;
            URLS = temp.split(",");
            setLocally = true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(format(method, "deferring to super"));
            }
            super.initThisSubclass(key, value);
        }
        if (setLocally) {
            if (log.isInfoEnabled()) {
                log.info(format(method, "known parameter", key, value));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest request,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.doThisSubclass(request, response);
        request.lockWrapper();

        if (REQUEST_ATTRIBUTE_INPUT_NAME != null) {
            Object testFedoraAuxSubjectAttributes =
                    request.getAttribute(REQUEST_ATTRIBUTE_INPUT_NAME);
            if (testFedoraAuxSubjectAttributes == null) {
                if (log.isDebugEnabled()) {
                    log
                            .debug(format(method,
                                          "no aux subject attributes found"));
                }
            } else if (!(testFedoraAuxSubjectAttributes instanceof Map)) {
                if (log.isErrorEnabled()) {
                    log
                            .error(format(method,
                                          "aux subject attributes found, but not a Map"));
                }
            } else {
                boolean errorsInMap = false;
                Map auxSubjectRoles = (Map) testFedoraAuxSubjectAttributes;
                Iterator auxSubjectRoleKeys =
                        auxSubjectRoles.keySet().iterator();
                while (auxSubjectRoleKeys.hasNext()) {
                    Object name = auxSubjectRoleKeys.next();
                    if (!(name instanceof String)) {
                        if (log.isErrorEnabled()) {
                            log
                                    .error(format(method, "key not a String "
                                            + name));
                        }
                        errorsInMap = true;
                        break;
                    } else {
                        Object value = auxSubjectRoles.get(name);
                        if (!(value instanceof String[])) {
                            if (log.isErrorEnabled()) {
                                log.error(format(method, "value not a String"
                                        + value));
                            }
                            errorsInMap = true;
                            break;
                        }
                    }
                }
                if (errorsInMap) {
                    if (log.isDebugEnabled()) {
                        log.debug(format(method, "errors in map"));
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(format(method, "no errors in map"));
                    }
                    request.addAttributes(REQUEST_ATTRIBUTE_INPUT_AUTHORITY,
                                          auxSubjectRoles);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(format(method, "before stashing"));
        }
        request.audit();

        Map subjectAttributesMap = new Hashtable();
        subjectAttributesMap.putAll(request.getAllAttributes());

        for (Iterator it = subjectAttributesMap.keySet().iterator(); it
                .hasNext();) {
            String name = (String) it.next();
            Object value = subjectAttributesMap.get(name);
            log.debug("IN FILTER MAP HAS ATTRIBUTE " + name + "==" + value
                    + " " + value.getClass().getName());
        }
        log.debug("IN FILTER ROLE eduPersonAffiliation?=="
                + request.isUserInRole("eduPersonAffiliation"));

        request.setAttribute(DELIVERY_NAME, subjectAttributesMap);
        return false; // i.e., don't signal to terminate servlet filter chain
    }

    @Override
    public void destroy() {
        String method = "destroy()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.destroy();
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

}
