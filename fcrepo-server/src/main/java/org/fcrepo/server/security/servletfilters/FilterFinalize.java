/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 * @deprecated
 */
@Deprecated
public class FilterFinalize
        extends FilterSetup {

    private static final Logger logger =
            LoggerFactory.getLogger(FilterFinalize.class);

    private static final boolean AUTHENTICATION_REQUIRED_DEFAULT = true;

    protected boolean AUTHENTICATION_REQUIRED = AUTHENTICATION_REQUIRED_DEFAULT;

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

    protected String[] URLS = URLS_DEFAULT.clone();

    private static final String URLS_KEY = "authentication-urls";

    @Override
    protected void initThisSubclass(String key, String value) {
        logger.debug("FAT.iTS");
        String method = "initThisSubclass() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        boolean setLocally = false;
        if (AUTHENTICATION_REQUIRED_KEY.equals(key)) {
            try {
                AUTHENTICATION_REQUIRED = booleanValue(value);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(format(method, "bad value", key, value));
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
            if (logger.isDebugEnabled()) {
                logger.debug(format(method, "deferring to super"));
            }
            super.initThisSubclass(key, value);
        }
        if (setLocally) {
            if (logger.isInfoEnabled()) {
                logger.info(format(method, "known parameter", key, value));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    @Override
    public boolean doThisSubclass(ExtendedHttpServletRequest request,
                                  HttpServletResponse response)
            throws Throwable {
        String method = "doThisSubclass() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.doThisSubclass(request, response);
        request.lockWrapper();

        if (REQUEST_ATTRIBUTE_INPUT_NAME != null) {
            Object testFedoraAuxSubjectAttributes =
                    request.getAttribute(REQUEST_ATTRIBUTE_INPUT_NAME);
            if (testFedoraAuxSubjectAttributes == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(format(method,
                                          "no aux subject attributes found"));
                }
            } else if (!(testFedoraAuxSubjectAttributes instanceof Map)) {
                if (logger.isErrorEnabled()) {
                    logger.error(format(method,
                                          "aux subject attributes found, but not a Map"));
                }
            } else {
                boolean errorsInMap = false;
                @SuppressWarnings("unchecked")
                Map<String, Set<?>> auxSubjectRoles =
                    (Map<String, Set<?>>) testFedoraAuxSubjectAttributes;
                Iterator<String> auxSubjectRoleKeys =
                        auxSubjectRoles.keySet().iterator();
                while (auxSubjectRoleKeys.hasNext()) {
                    String name = auxSubjectRoleKeys.next();
                    Object value = auxSubjectRoles.get(name);
                    if (!(value instanceof String[])) {
                        if (logger.isErrorEnabled()) {
                            logger.error(format(method, "value not a Set"
                                    + value));
                        }
                        errorsInMap = true;
                        break;
                    }
                }
                if (errorsInMap) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(format(method, "errors in map"));
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug(format(method, "no errors in map"));
                    }
                    request.addAttributes(REQUEST_ATTRIBUTE_INPUT_AUTHORITY,
                                          auxSubjectRoles);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(format(method, "before stashing"));
        }
        request.audit();

        Map<String, Set<?>> subjectAttributesMap = new Hashtable<String, Set<?>>();
        subjectAttributesMap.putAll(request.getAllAttributes());

        if (logger.isDebugEnabled()) {
            for (Iterator<String> subjectAttributes = subjectAttributesMap.keySet().iterator();
                    subjectAttributes.hasNext();) {
                String subjectAttribute = subjectAttributes.next();
                Object value = subjectAttributesMap.get(subjectAttribute);
                logger.debug("IN FILTER MAP HAS ATTRIBUTE {}=={} {}",
                        subjectAttribute, value,
                        value.getClass().getName());
            }
            logger.debug("IN FILTER ROLE eduPersonAffiliation?=={}",
                    request.isUserInRole("eduPersonAffiliation"));
        }
        request.setAttribute(DELIVERY_NAME, subjectAttributesMap);
        return false; // i.e., don't signal to terminate servlet filter chain
    }

    @Override
    public void destroy() {
        String method = "destroy()";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.destroy();
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

}
