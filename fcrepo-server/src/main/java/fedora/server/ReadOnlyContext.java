/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.HTTPConstants;

import org.apache.log4j.Logger;

import fedora.common.Constants;

import fedora.server.security.servletfilters.ExtendedHttpServletRequest;
import fedora.server.utilities.DateUtility;

/**
 * Context that is read-only.
 *
 * @author Chris Wilper
 * @version $Id$
 */
public class ReadOnlyContext
        implements Context {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(ReadOnlyContext.class.getName());

    public static ReadOnlyContext EMPTY =
            new ReadOnlyContext(null, null, null, "", true);
    static {
        EMPTY.setActionAttributes(null);
        EMPTY.setResourceAttributes(null);
    }

    private final Date now = new Date();

    private MultiValueMap m_environmentAttributes;

    public final MultiValueMap getEnvironmentAttributes() {
        return m_environmentAttributes;
    }

    private MultiValueMap m_subjectAttributes;

    private MultiValueMap m_actionAttributes;

    private MultiValueMap m_resourceAttributes;

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
                            MultiValueMap environmentAttributes,
                            MultiValueMap subjectAttributes,
                            String password,
                            boolean noOp) {
        //super(parameters);
        m_environmentAttributes = environmentAttributes;
        if (m_environmentAttributes == null) {
            m_environmentAttributes = new MultiValueMap();
        }
        m_environmentAttributes.lock();
        m_subjectAttributes = subjectAttributes;
        if (m_subjectAttributes == null) {
            LOG.debug("subject map parm is null");
            m_subjectAttributes = new MultiValueMap();
        }
        m_subjectAttributes.lock();
        LOG.debug("subject attributes in readonlycontext constructor == "
                + m_subjectAttributes);
        m_actionAttributes = new MultiValueMap();
        m_actionAttributes.lock();
        m_resourceAttributes = new MultiValueMap();
        m_resourceAttributes.lock();
        if (password == null) {
            password = "";
        }
        this.password = password;
        this.noOp = noOp;
        if (request instanceof ExtendedHttpServletRequest) {
            extendedHttpServletRequest = (ExtendedHttpServletRequest) request;
        }
    }

    public Iterator environmentAttributes() {
        return m_environmentAttributes.names();
    }

    public int nEnvironmentValues(String name) {
        return m_environmentAttributes.length(name);
    }

    public String getEnvironmentValue(String name) {
        return m_environmentAttributes.getString(name);
    }

    public String[] getEnvironmentValues(String name) {
        return m_environmentAttributes.getStringArray(name);
    }

    public Iterator subjectAttributes() {
        return m_subjectAttributes.names();
    }

    public int nSubjectValues(String name) {
        int n = m_subjectAttributes.length(name);
        LOG.debug("N SUBJECT VALUES without == " + n);
        if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            n++;
        }
        LOG.debug("N SUBJECT VALUES with == " + n);
        return n;
    }

    public String getSubjectValue(String name) {
        String value = null;
        if (m_subjectAttributes.length(name) == 1) {
            value = m_subjectAttributes.getString(name);
            LOG.debug("SINGLE SUBJECT VALUE from map == " + value);
        } else if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            value = "";
            LOG.debug("SINGLE SUBJECT VALUE from iuir() == " + value);
        }
        return value;
    }

    public String[] getSubjectValues(String name) {
        int n = m_subjectAttributes.length(name);
        if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            n++;
        }
        String[] values = new String[n];
        String[] temp = m_subjectAttributes.getStringArray(name);
        for (int i = 0; i < temp.length; i++) {
            values[i] = temp[i];
        }
        if (extendedHttpServletRequest != null
                && extendedHttpServletRequest.isUserInRole(name)) {
            values[n - 1] = "";
        }
        if (values == null) {
            LOG.debug("INNER RETURNING NO VALUES FOR " + name);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("INNER RETURNING " + values.length + " VALUES FOR "
                    + name + " ==");
            for (int i = 0; i < values.length; i++) {
                sb.append(" " + values[i]);
            }
            LOG.debug(sb.toString());
        }
        return values;
    }

    public void setActionAttributes(MultiValueMap actionAttributes) {
        m_actionAttributes = actionAttributes;
        if (m_actionAttributes == null) {
            m_actionAttributes = new MultiValueMap();
        }
        m_actionAttributes.lock();
    }

    public Iterator actionAttributes() {
        return m_actionAttributes.names();
    }

    public int nActionValues(String name) {
        return m_actionAttributes.length(name);
    }

    public String getActionValue(String name) {
        return m_actionAttributes.getString(name);
    }

    public String[] getActionValues(String name) {
        return m_actionAttributes.getStringArray(name);
    }

    public Iterator resourceAttributes() {
        return m_resourceAttributes.names();
    }

    public void setResourceAttributes(MultiValueMap resourceAttributes) {
        m_resourceAttributes = resourceAttributes;
        if (m_resourceAttributes == null) {
            m_resourceAttributes = new MultiValueMap();
        }
        m_resourceAttributes.lock();
    }

    public int nResourceValues(String name) {
        return m_resourceAttributes.length(name);
    }

    public String getResourceValue(String name) {
        return m_resourceAttributes.getString(name);
    }

    public String[] getResourceValues(String name) {
        return m_resourceAttributes.getStringArray(name);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("READ-ONLY CONTEXT:\n");
        buffer.append(m_environmentAttributes);
        buffer.append(m_subjectAttributes);
        buffer.append(m_actionAttributes);
        buffer.append(m_resourceAttributes);
        buffer.append("(END-READ ONLY CONTEXT)\n");
        return buffer.toString();
    }

    public Date now() {
        return now;
    }

    private static final MultiValueMap beginEnvironmentMap(String messageProtocol)
            throws Exception {
        MultiValueMap environmentMap = new MultiValueMap();
        environmentMap.set(Constants.HTTP_REQUEST.MESSAGE_PROTOCOL.uri,
                           messageProtocol);
        Date now = new Date();
        environmentMap.set(Constants.ENVIRONMENT.CURRENT_DATE_TIME.uri,
                           DateUtility.convertDateToString(now));
        environmentMap.set(Constants.ENVIRONMENT.CURRENT_DATE.uri, DateUtility
                .convertDateToDateString(now));
        environmentMap.set(Constants.ENVIRONMENT.CURRENT_TIME.uri, DateUtility
                .convertDateToTimeString(now));
        return environmentMap;
    }

    //will need fixup for noOp
    public static Context getSoapContext() {
        HttpServletRequest req =
                (HttpServletRequest) MessageContext.getCurrentContext()
                        .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        return ReadOnlyContext.getContext(Constants.HTTP_REQUEST.SOAP.uri, req);
    }

    /*
     * i don't see any references. needed? let's see . . . public static final
     * ReadOnlyContext getContext(Context existingContext, String subjectId,
     * String password, String[] roles) { return
     * getContext(existingContext.getEnvironmentAttributes(), subjectId,
     * password, roles, existingContext.getNoOp()); }
     */

    private static final Class STRING_ARRAY_CLASS;
    static {
        String[] temp = {""};
        STRING_ARRAY_CLASS = temp.getClass();
    }

    private static final ReadOnlyContext getContext(HttpServletRequest request,
                                                    MultiValueMap environmentMap,
                                                    String subjectId,
                                                    String password, /*
                                                     * String[]
                                                     * roles,
                                                     */
                                                    Map auxSubjectRoles,
                                                    boolean noOp) {
        MultiValueMap subjectMap = new MultiValueMap();
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
                LOG.debug("IN CONTEXT auxSubjectRoles == null");
            } else {
                LOG.debug("IN CONTEXT processing auxSubjectRoles=="
                        + auxSubjectRoles);
                LOG.debug("IN CONTEXT processing auxSubjectRoles.keySet()=="
                        + auxSubjectRoles.keySet());
                LOG
                        .debug("IN CONTEXT processing auxSubjectRoles.keySet().isEmpty()=="
                                + auxSubjectRoles.keySet().isEmpty());
                Iterator auxSubjectRoleKeys =
                        auxSubjectRoles.keySet().iterator();
                LOG.debug("IN CONTEXT processing auxSubjectRoleKeys=="
                        + auxSubjectRoleKeys);
                while (auxSubjectRoleKeys.hasNext()) {
                    Object name = auxSubjectRoleKeys.next();
                    LOG.debug("IN CONTEXT name==" + name);
                    if (name instanceof String) {
                        LOG.debug("IN CONTEXT name is string==" + name);
                        Object value = auxSubjectRoles.get(name);
                        if (value instanceof String
                                || value instanceof String[]) {
                            LOG.debug("IN CONTEXT value is string([])");
                            if (value instanceof String) {
                                LOG.debug("IN CONTEXT value is string=="
                                        + (String) value);
                            }
                            if (value instanceof String[]) {
                                LOG.debug("IN CONTEXT value is string[]");
                                for (int z = 0; z < ((String[]) value).length; z++) {
                                    LOG.debug("IN CONTEXT this value=="
                                            + ((String[]) value)[z]);
                                }
                            }
                            subjectMap.set((String) name, value);
                        } else if (value instanceof Set) {
                            String temp[] = new String[((Set) value).size()];
                            int i = 0;
                            for (Iterator setIterator =
                                    ((Set) value).iterator(); setIterator
                                    .hasNext();) {
                                String singleValue =
                                        (String) setIterator.next();
                                LOG.debug("IN CONTEXT singleValue is string=="
                                        + singleValue);
                                temp[i++] = singleValue;
                            }
                            subjectMap.set((String) name, temp);
                        }
                    }
                }
                LOG.debug("IN CONTEXT after while");
            }
        } catch (Exception e) {
            LOG.error("caught exception building subjectMap " + e.getMessage(),
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

    /*
     * new; can we do without? private static final ReadOnlyContext
     * getContext(MultiValueMap environmentMap, String subjectId, String
     * password, String[] roles, Map auxSubjectRoles, boolean noOp) { return
     * getContext(null, environmentMap, subjectId, password, roles,
     * auxSubjectRoles, noOp); }
     */

    // needed only for rebuild
    public static final ReadOnlyContext getContext(String messageProtocol,
                                                   String subjectId,
                                                   String password, /*
                                                    * String[]
                                                    * roles,
                                                    */
                                                   boolean noOp)
            throws Exception {
        MultiValueMap environmentMap = beginEnvironmentMap(messageProtocol);
        environmentMap.lock(); // no request to grok more from
        return getContext(null, environmentMap, subjectId, password, /* roles, */
        null, noOp);
    }

    public static final ReadOnlyContext getContext(String messageProtocol,
                                                   HttpServletRequest request /*
     * ,
     * String[]
     * overrideRoles
     */) {
        MultiValueMap environmentMap = null;
        try {
            environmentMap = beginEnvironmentMap(messageProtocol);

            environmentMap.set(Constants.HTTP_REQUEST.SECURITY.uri, request
                    .isSecure() ? Constants.HTTP_REQUEST.SECURE.uri
                    : Constants.HTTP_REQUEST.INSECURE.uri);
            environmentMap.set(Constants.HTTP_REQUEST.SESSION_STATUS.uri,
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
                environmentMap.set(Constants.HTTP_REQUEST.CONTENT_LENGTH.uri,
                                   "" + request.getContentLength());
            }
            if (request.getLocalPort() > -1) {
                environmentMap.set(Constants.HTTP_REQUEST.SERVER_PORT.uri, ""
                        + request.getLocalPort());
            }

            if (request.getProtocol() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.PROTOCOL.uri, request
                        .getProtocol());
            }
            if (request.getScheme() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.SCHEME.uri, request
                        .getScheme());
            }
            if (request.getAuthType() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.AUTHTYPE.uri, request
                        .getAuthType());
            }
            if (request.getMethod() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.METHOD.uri, request
                        .getMethod());
            }
            if (sessionEncoding != null) {
                environmentMap.set(Constants.HTTP_REQUEST.SESSION_ENCODING.uri,
                                   sessionEncoding);
            }
            if (request.getContentType() != null) {
                environmentMap.set(Constants.HTTP_REQUEST.CONTENT_TYPE.uri,
                                   request.getContentType());
            }
            if (request.getLocalAddr() != null) {
                environmentMap
                        .set(Constants.HTTP_REQUEST.SERVER_IP_ADDRESS.uri,
                             request.getLocalAddr());
            }
            if (request.getRemoteAddr() != null) {
                environmentMap
                        .set(Constants.HTTP_REQUEST.CLIENT_IP_ADDRESS.uri,
                             request.getRemoteAddr());
            }

            if (request.getRemoteHost() != null) {
                if (!request.getRemoteHost()
                        .matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    environmentMap.set(Constants.HTTP_REQUEST.CLIENT_FQDN.uri,
                                       request.getRemoteHost().toLowerCase());
                }
            }
            if (request.getLocalName() != null) {
                if (!request.getLocalName()
                        .matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    environmentMap.set(Constants.HTTP_REQUEST.SERVER_FQDN.uri,
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
            if(request instanceof ExtendedHttpServletRequest){
                password = ((ExtendedHttpServletRequest) request).getPassword();
            }
        } catch (Throwable th) {
            LOG.error("in context, can't grok password from extended request "
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
            noOp =
                    (new Boolean(request.getParameter(NOOP_PARAMETER_NAME)))
                            .booleanValue();
            LOG.debug("NOOP_PARAMETER_NAME=" + NOOP_PARAMETER_NAME);
            LOG.debug("request.getParameter(NOOP_PARAMETER_NAME)="
                    + request.getParameter(NOOP_PARAMETER_NAME));
            LOG.debug("noOp=" + noOp);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        Map auxSubjectRoles = null;
        Object testFedoraAuxSubjectAttributes =
                request.getAttribute(FEDORA_AUX_SUBJECT_ATTRIBUTES);
        if (testFedoraAuxSubjectAttributes != null
                && testFedoraAuxSubjectAttributes instanceof Map) {
            auxSubjectRoles = (Map) testFedoraAuxSubjectAttributes;
        }
        return getContext(request, environmentMap, subjectId, password, /* overrideRoles, */
        auxSubjectRoles, noOp);
    }

    public String getPassword() {
        return password;
    }

    public boolean getNoOp() {
        return noOp;
    }

}
