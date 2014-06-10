/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.fcrepo.common.Constants;
import org.fcrepo.server.security.servletfilters.ExtendedHttpServletRequest;
import org.fcrepo.utilities.DateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Context that is read-only.
 *
 * @author Chris Wilper
 * @version $Id$
 */
public class ReadOnlyContext
        implements Context {

    private static final Logger logger =
            LoggerFactory.getLogger(ReadOnlyContext.class);

    public static ReadOnlyContext EMPTY =
            new ReadOnlyContext(null, null, null, "", true);
    static {
        EMPTY.setActionAttributes(null);
        EMPTY.setResourceAttributes(null);
    }
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final Date now = new Date();

    private MultiValueMap<URI> m_environmentAttributes;

    @Override
    public final MultiValueMap<URI> getEnvironmentAttributes() {
        return m_environmentAttributes;
    }

    private MultiValueMap<String> m_subjectAttributes;

    private MultiValueMap<URI> m_actionAttributes;

    private MultiValueMap<URI> m_resourceAttributes;
    
    private MultiValueMap<String> m_requestHeaders;

    private final String password;

    private static final String NOOP_PARAMETER_NAME = "noOp";

    public static final boolean NO_OP = true;

    public static final boolean DO_OP = false;

    public static final String BACKEND_SERVICE = "backendService";

    private boolean noOp = false;

    private ExtendedHttpServletRequest extendedHttpServletRequest = null;

    /**
     * Creates and initializes the <code>Context</code>.
     *
     * @param parameters
     *        A pre-loaded Map of name-value pairs comprising the context.
     */
    private ReadOnlyContext(HttpServletRequest request,
                            MultiValueMap<URI> environmentAttributes,
                            MultiValueMap<String> subjectAttributes,
                            String password,
                            boolean noOp) {

        setEnvironmentValues(environmentAttributes);
        m_subjectAttributes = subjectAttributes;
        if (m_subjectAttributes == null) {
            logger.debug("subject map parm is null");
            m_subjectAttributes = new MultiValueMap<String>();
        }
        m_subjectAttributes.lock();
        logger.debug("subject attributes in readonlycontext constructor == {}",
                m_subjectAttributes);
        m_actionAttributes = new MultiValueMap<URI>();
        m_actionAttributes.lock();
        m_resourceAttributes = new MultiValueMap<URI>();
        m_resourceAttributes.lock();
        if (password == null) {
            password = "";
        }
        this.password = password;
        this.noOp = noOp;
        if (request instanceof ExtendedHttpServletRequest) {
            extendedHttpServletRequest = (ExtendedHttpServletRequest) request;
        }
        m_requestHeaders = getHeaders(request);
    }

    public void setEnvironmentValues(MultiValueMap<URI> environmentAttributes) {
        m_environmentAttributes = environmentAttributes;
        if (m_environmentAttributes == null) {
            m_environmentAttributes = new MultiValueMap<URI>();
        }
        m_environmentAttributes.lock();
    }

    @Override
    public Iterator<URI> environmentAttributes() {
        return m_environmentAttributes.names();
    }

    @Override
    public int nEnvironmentValues(URI name) {
        return m_environmentAttributes.length(name);
    }

    @Override
    public String getEnvironmentValue(URI name) {
        return m_environmentAttributes.getString(name);
    }

    @Override
    public String[] getEnvironmentValues(URI name) {
        return m_environmentAttributes.getStringArray(name);
    }

    @Override
    public Iterator<String> subjectAttributes() {
        return m_subjectAttributes.names();
    }

    @Override
    public int nSubjectValues(String name) {
        int n = m_subjectAttributes.length(name);
        logger.debug("N SUBJECT VALUES without == {}", n);
        if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            n++;
        }
        logger.debug("N SUBJECT VALUES with == {}", n);
        return n;
    }

    @Override
    public String getSubjectValue(String name) {
        String value = null;
        if (m_subjectAttributes.length(name) == 1) {
            value = m_subjectAttributes.getString(name);
            logger.debug("SINGLE SUBJECT VALUE from map == {}", value);
        } else if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            value = "";
            logger.debug("SINGLE SUBJECT VALUE from iuir() == {}", value);
        }
        return value;
    }

    @Override
    public String[] getSubjectValues(String name) {
        int n = m_subjectAttributes.length(name);
        if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            n++;
        }
        String[] values = new String[n];
        String[] temp = m_subjectAttributes.getStringArray(name);
        System.arraycopy(temp, 0, values, 0, temp.length);
        if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            values[n - 1] = "";
        }
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("INNER RETURNING " + values.length + " VALUES FOR "
                    + name + " ==");
            for (int i = 0; i < values.length; i++) {
                sb.append(" " + values[i]);
            }
            logger.debug(sb.toString());
        }
        return values;
    }

    @Override
    public void setActionAttributes(MultiValueMap<URI> actionAttributes) {
        if (actionAttributes == null) {
            m_actionAttributes = new MultiValueMap<URI>();
        } else {
            m_actionAttributes = actionAttributes;            
        }
        m_actionAttributes.lock();
    }

    @Override
    public Iterator<URI> actionAttributes() {
        return m_actionAttributes.names();
    }

    @Override
    public int nActionValues(URI name) {
        return m_actionAttributes.length(name);
    }

    @Override
    public String getActionValue(URI name) {
        return m_actionAttributes.getString(name);
    }

    @Override
    public String[] getActionValues(URI name) {
        return m_actionAttributes.getStringArray(name);
    }

    @Override
    public Iterator<URI> resourceAttributes() {
        return m_resourceAttributes.names();
    }

    @Override
    public void setResourceAttributes(MultiValueMap<URI> resourceAttributes) {
        if (resourceAttributes == null) {
            m_resourceAttributes = new MultiValueMap<URI>();
        } else {
            m_resourceAttributes = resourceAttributes;
        }
        m_resourceAttributes.lock();
    }

    @Override
    public int nResourceValues(URI name) {
        return m_resourceAttributes.length(name);
    }

    @Override
    public String getResourceValue(URI name) {
        return m_resourceAttributes.getString(name);
    }

    @Override
    public String[] getResourceValues(URI name) {
        return m_resourceAttributes.getStringArray(name);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(256);
        buffer.append("READ-ONLY CONTEXT:\n");
        buffer.append(m_environmentAttributes);
        buffer.append(m_subjectAttributes);
        buffer.append(m_actionAttributes);
        buffer.append(m_resourceAttributes);
        buffer.append("(END-READ ONLY CONTEXT)\n");
        return buffer.toString();
    }

    @Override
    public Date now() {
        return now;
    }

    private static final MultiValueMap<URI> beginEnvironmentMap(String messageProtocol)
            throws Exception {
        MultiValueMap<URI> environmentMap = new MultiValueMap<URI>();
        environmentMap.set(Constants.HTTP_REQUEST.MESSAGE_PROTOCOL.attributeId,
                           messageProtocol);
        Date now = new Date();
        environmentMap.set(Constants.ENVIRONMENT.CURRENT_DATE_TIME.attributeId,
                           DateUtility.convertDateToString(now));
        environmentMap.set(Constants.ENVIRONMENT.CURRENT_DATE.attributeId, DateUtility
                .convertDateToDateString(now));
        environmentMap.set(Constants.ENVIRONMENT.CURRENT_TIME.attributeId, DateUtility
                .convertDateToTimeString(now));
        return environmentMap;
    }

    public static Context getSoapContext(javax.xml.ws.handler.MessageContext ctx) {
    	HttpServletRequest req = (HttpServletRequest) ctx.get(AbstractHTTPDestination.HTTP_REQUEST);
        return ReadOnlyContext.getContext(Constants.HTTP_REQUEST.SOAP.uri, req);
    }

    private static final ReadOnlyContext getContext(HttpServletRequest request,
                                                    MultiValueMap<URI> environmentMap,
                                                    String subjectId,
                                                    String password, /*
                                                     * String[]
                                                     * roles,
                                                     */
                                                    Map<String, ?> auxSubjectRoles,
                                                    boolean noOp) {
        MultiValueMap<String> subjectMap = new MultiValueMap<String>();
        try {
            subjectMap.set(Constants.SUBJECT.LOGIN_ID.uri,
                           subjectId == null ? "" : subjectId);
            /*
             * for (int i = 0; (roles != null) && (i < roles.length); i++) {
             * String[] parts = parseRole(roles[i]); if ((parts != null) &&
             * parts.length == 2) { subjectMap.set(parts[0],parts[1]); //todo:
             * handle multiple values (ldap) } }
             */
            if (auxSubjectRoles == null) {
                logger.debug("IN CONTEXT auxSubjectRoles == null");
            } else {
                logger.debug("IN CONTEXT processing auxSubjectRoles=={}",
                        auxSubjectRoles);
                logger.debug("IN CONTEXT processing auxSubjectRoles.keySet()=={}",
                        auxSubjectRoles.keySet());
                logger.debug("IN CONTEXT processing auxSubjectRoles.keySet().isEmpty()=={}",
                                auxSubjectRoles.keySet().isEmpty());
                Iterator<String> auxSubjectRoleKeys =
                        auxSubjectRoles.keySet().iterator();
                logger.debug("IN CONTEXT processing auxSubjectRoleKeys=={}",
                        auxSubjectRoleKeys);
                while (auxSubjectRoleKeys.hasNext()) {
                    Object name = auxSubjectRoleKeys.next();
                    logger.debug("IN CONTEXT name=={}", name);
                    if (name instanceof String) {
                        logger.debug("IN CONTEXT name is string=={}", name);
                        Object value = auxSubjectRoles.get(name);
                        if (value instanceof String
                                || value instanceof String[]) {
                            logger.debug("IN CONTEXT value is string([])");
                            if (value instanceof String) {
                                logger.debug("IN CONTEXT value is string=={}",value);
                                subjectMap.set((String) name, (String)value);
                            }
                            if (value instanceof String[]) {
                                logger.debug("IN CONTEXT value is string[]");
                                String [] values = (String[]) value;
                                if (logger.isDebugEnabled()) {
                                    for (int z = 0; z < values.length; z++) {
                                        logger.debug("IN CONTEXT this value=={}",
                                                values[z]);
                                    }
                                }
                                subjectMap.set((String) name, values);
                            }
                        } else if (value instanceof Set) {
                            @SuppressWarnings("unchecked")
                            Set<String> values = (Set<String>) value;
                            String temp[] = values.toArray(EMPTY_STRING_ARRAY);
                            if (logger.isDebugEnabled()) {
                                for (String singleValue: temp) {
                                    logger.debug("IN CONTEXT singleValue is string=={}",
                                            singleValue);
                                }
                            }

                            subjectMap.set((String)name, temp);
                        }
                    }
                }
                logger.debug("IN CONTEXT after while");
            }
        } catch (Exception e) {
            logger.error("caught exception building subjectMap " + e.getMessage(),
                      e);
        } finally {
            subjectMap.lock();
        }
        return new ReadOnlyContext(request,
                                   environmentMap,
                                   subjectMap,
                                   password == null ? "" : password,
                                   noOp);
    }

    // needed only for rebuild
    public static final ReadOnlyContext getContext(String messageProtocol,
                                                   String subjectId,
                                                   String password, /*
                                                    * String[]
                                                    * roles,
                                                    */
                                                   boolean noOp)
            throws Exception {
        MultiValueMap<URI> environmentMap = beginEnvironmentMap(messageProtocol);
        environmentMap.lock(); // no request to grok more from
        return getContext(null, environmentMap, subjectId, password, /* roles, */
        null, noOp);
    }

    @SuppressWarnings("unchecked")
    public static final ReadOnlyContext getContext(String messageProtocol,
                                                   HttpServletRequest request /*
     * ,
     * String[]
     * overrideRoles
     */) {
        MultiValueMap<URI> environmentMap = null;
        try {
            environmentMap = beginEnvironmentMap(messageProtocol);

            environmentMap.set(Constants.HTTP_REQUEST.SECURITY.attributeId, request
                    .isSecure() ? Constants.HTTP_REQUEST.SECURE.uri
                    : Constants.HTTP_REQUEST.INSECURE.uri);
            environmentMap.set(Constants.HTTP_REQUEST.SESSION_STATUS.attributeId,
                               request.isRequestedSessionIdValid() ? "valid"
                                       : "invalid");

            String sessionEncoding = null;
            if (request.isRequestedSessionIdFromCookie()) {
                sessionEncoding = "cookie";
            } else if (request.isRequestedSessionIdFromURL()) {
                sessionEncoding = "url";
            }

            if (request.getContextPath() != null){
                environmentMap.set(Constants.FEDORA_APP_CONTEXT_NAME, request.getContextPath().replace("/", ""));
            }

            if (request.getContentLength() > -1) {
                environmentMap.set(Constants.HTTP_REQUEST.CONTENT_LENGTH.attributeId,
                                   Integer.toString(request.getContentLength()));
            }
            if (request.getLocalPort() > -1) {
                environmentMap.set(Constants.HTTP_REQUEST.SERVER_PORT.attributeId,
                        Integer.toString(request.getLocalPort()));
            }

            if (request.getProtocol() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.PROTOCOL.attributeId, request
                        .getProtocol());
            }
            if (request.getScheme() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.SCHEME.attributeId, request
                        .getScheme());
            }
            if (request.getAuthType() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.AUTHTYPE.attributeId, request
                        .getAuthType());
            }
            if (request.getMethod() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.METHOD.attributeId, request
                        .getMethod());
            }
            if (sessionEncoding != null) {
                environmentMap.set(Constants.HTTP_REQUEST.SESSION_ENCODING.attributeId,
                                   sessionEncoding);
            }
            if (request.getContentType() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.CONTENT_TYPE.attributeId,
                                   request.getContentType());
            }
            if (request.getLocalAddr() != null) {
                logger.debug("Request Server IP Address is '{}'", request.getLocalAddr());
                environmentMap
                        .set(Constants.HTTP_REQUEST.SERVER_IP_ADDRESS.attributeId,
                             request.getLocalAddr());
            }
            if (request.getRemoteAddr() != null) {
                logger.debug("Request Client IP Address is '{}'", request.getRemoteAddr());
                environmentMap
                        .set(Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS.attributeId,
                             request.getRemoteAddr());
            }

            if (request.getRemoteHost() != null) {
                if (!request.getRemoteHost()
                        .matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    environmentMap.set(Constants.HTTP_REQUEST.CLIENT_FQDN.attributeId,
                                       request.getRemoteHost().toLowerCase());
                }
            }
            if (request.getLocalName() != null) {
                if (!request.getLocalName()
                        .matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    environmentMap.set(Constants.HTTP_REQUEST.SERVER_FQDN.attributeId,
                                       request.getLocalName().toLowerCase());
                }
            }
        } catch (Exception e) {
        } finally {
            environmentMap.lock();
        }

        String subjectId = request.getRemoteUser();
        String password = null;

        try {
            if (request instanceof ExtendedHttpServletRequest){
                password = ((ExtendedHttpServletRequest) request).getPassword();
            }
        } catch (Throwable th) {
            logger.error("in context, can't grok password from extended request "
                    + th.getMessage());
        }

        if (subjectId == null) {
            subjectId = "";
        }
        if (password == null) {
            password = "";
        }

        boolean noOp = true; //safest approach
        try {
            noOp = Boolean.parseBoolean(request.getParameter(NOOP_PARAMETER_NAME));
            logger.debug("NOOP_PARAMETER_NAME={}", NOOP_PARAMETER_NAME);
            logger.debug("request.getParameter(NOOP_PARAMETER_NAME)={}",
                    request.getParameter(NOOP_PARAMETER_NAME));
            logger.debug("noOp={}", noOp);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        Map<String, ?> auxSubjectRoles = null;
        Object testFedoraAuxSubjectAttributes =
                request.getAttribute(FEDORA_AUX_SUBJECT_ATTRIBUTES);
        if (testFedoraAuxSubjectAttributes != null
                && testFedoraAuxSubjectAttributes instanceof Map) {
            auxSubjectRoles = (Map<String, ?>) testFedoraAuxSubjectAttributes;
        }
        return getContext(request, environmentMap, subjectId, password,
        auxSubjectRoles, noOp);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean getNoOp() {
        return noOp;
    }
    
    @Override
    public MultiValueMap<String> getHeaders() {
        return m_requestHeaders;
    }

    @Override
    public String getHeaderValue(String name) {
        return m_requestHeaders.getString(name.toLowerCase());
    }

    @Override
    public String[] getHeaderValues(String name) {
        return m_requestHeaders.getStringArray(name.toLowerCase());
    }

    private static MultiValueMap<String> getHeaders(HttpServletRequest request) {
        MultiValueMap<String> result = new MultiValueMap<String>();
        if (request == null) return result;
        @SuppressWarnings("unchecked")
        Enumeration<String> names = request.getHeaderNames();
        while(names != null && names.hasMoreElements()) {
            String name = names.nextElement().toLowerCase();
            @SuppressWarnings("unchecked")
            Enumeration<String> values = request.getHeaders(name);
            while(values.hasMoreElements()) {
                String next = values.nextElement();
                if (result.length(name) > 0) {
                    String[] prev = result.getStringArray(name);
                    String[]temp = Arrays.copyOf(prev, prev.length + 1);
                    temp[temp.length - 1] = next;
                    result.set(name, temp);
                } else {
                    result.set(name, next);
                }
            }
        }
        return result;
    }
}
