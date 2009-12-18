/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters.pubcookie;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fedora.server.security.servletfilters.BaseCaching;
import fedora.server.security.servletfilters.CacheElement;

/**
 * @author Bill Niebel
 */
public class FilterPubcookie
        extends BaseCaching {

    protected static Log log = LogFactory.getLog(FilterPubcookie.class);

    private static final Map NO_REQUEST_PARAMETERS = new HashMap();

    private static final Cookie[] NO_COOKIES = new Cookie[0];

    public static final String PUBCOOKIE_NAME_KEY = "pubcookie-name";

    public static final String PUBCOOKIE_LOGINPAGE_URL_KEY =
            "pubcookie-loginpage-url";

    public static final String PUBCOOKIE_LOGINPAGE_FORM_NAME_KEY =
            "pubcookie-loginpage-form-name";

    public static final String PUBCOOKIE_LOGINPAGE_INPUT_NAME_USERID_KEY =
            "pubcookie-loginpage-input-name-userid";

    public static final String PUBCOOKIE_LOGINPAGE_INPUT_NAME_PASSWORD_KEY =
            "pubcookie-loginpage-input-name-password";

    public static final String TRUSTSTORE_LOCATION_KEY =
            "javax.net.ssl.trustStore";

    public static final String TRUSTSTORE_PASSWORD_KEY =
            "javax.net.ssl.trustStorePassword";

    public static final String KEYSTORE_LOCATION_KEY = "javax.net.ssl.keyStore";

    public static final String KEYSTORE_PASSWORD_KEY =
            "javax.net.ssl.keyStorePassword";

    private String PUBCOOKIE_NAME = "";

    private String PUBCOOKIE_LOGINPAGE_URL = "";

    private String PUBCOOKIE_LOGINPAGE_FORM_NAME = "";

    private String PUBCOOKIE_LOGINPAGE_INPUT_NAME_USERID = "";

    private String PUBCOOKIE_LOGINPAGE_INPUT_NAME_PASSWORD = "";

    private String TRUSTSTORE_LOCATION = null;

    private String TRUSTSTORE_PASSWORD = null;

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

    @Override
    protected void initThisSubclass(String key, String value) {
        String method = "initThisSubclass()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        boolean setLocally = false;
        if (PUBCOOKIE_NAME_KEY.equals(key)) {
            PUBCOOKIE_NAME = value;
            setLocally = true;
        } else if (PUBCOOKIE_LOGINPAGE_URL_KEY.equals(key)) {
            PUBCOOKIE_LOGINPAGE_URL = value;
            setLocally = true;
        } else if (PUBCOOKIE_LOGINPAGE_FORM_NAME_KEY.equals(key)) {
            PUBCOOKIE_LOGINPAGE_FORM_NAME = value;
            setLocally = true;
        } else if (PUBCOOKIE_LOGINPAGE_INPUT_NAME_USERID_KEY.equals(key)) {
            PUBCOOKIE_LOGINPAGE_INPUT_NAME_USERID = value;
            setLocally = true;
        } else if (PUBCOOKIE_LOGINPAGE_INPUT_NAME_PASSWORD_KEY.equals(key)) {
            PUBCOOKIE_LOGINPAGE_INPUT_NAME_PASSWORD = value;
            setLocally = true;
        } else if (TRUSTSTORE_LOCATION_KEY.equals(key)) {
            TRUSTSTORE_LOCATION = value;
            setLocally = true;
        } else if (TRUSTSTORE_PASSWORD_KEY.equals(key)) {
            TRUSTSTORE_PASSWORD = value;
            setLocally = true;
        } else {
            if (log.isErrorEnabled()) {
                log.error(format(method, "deferring to super"));
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

    private final String getAction(Node parent,
                                   String pubcookieLoginpageFormName) {
        String method = "getAction()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        String action = "";
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String tag = child.getNodeName();
            if ("form".equalsIgnoreCase(tag)) {
                NamedNodeMap attributes = child.getAttributes();
                Node nameNode = attributes.getNamedItem("name");
                String name = nameNode.getNodeValue();
                Node actionNode = attributes.getNamedItem("action");
                if (pubcookieLoginpageFormName.equalsIgnoreCase(name)
                        && actionNode != null) {
                    action = actionNode.getNodeValue();
                    break;
                }
            }
            action = getAction(child, pubcookieLoginpageFormName);
            if (!"".equals(action)) {
                break;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
        return action;
    }

    //initial, setup call
    private final Map getFormFields(Node parent) {
        String method = "getFormFields(Node parent)";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        Map formfields = new Hashtable();
        getFormFields(parent, formfields);
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
        return formfields;
    }

    //inner, recursive call
    private final void getFormFields(Node parent, Map formfields) {
        String method = "getFormFields(Node parent, Map formfields)";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String tag = child.getNodeName();
            if ("input".equalsIgnoreCase(tag)) {
                NamedNodeMap attributes = child.getAttributes();
                Node typeNode = attributes.getNamedItem("type");
                String type = typeNode.getNodeValue();
                Node nameNode = attributes.getNamedItem("name");
                String name = nameNode.getNodeValue();
                Node valueNode = attributes.getNamedItem("value");
                String value = "";
                if (valueNode != null) {
                    value = valueNode.getNodeValue();
                }
                if ("hidden".equalsIgnoreCase(type) && value != null) {
                    if (log.isDebugEnabled()) {
                        log
                                .debug(format("capturing hidden fields",
                                              name,
                                              value));
                    }
                    formfields.put(name, value);
                }
            }
            getFormFields(child, formfields);
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    public void populateCacheElement(CacheElement cacheElement, String password) {
        String method = "populateCacheElement()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        Boolean authenticated = null;
        ConnectPubcookie tidyConnect = new ConnectPubcookie();
        if (log.isDebugEnabled()) {
            log.debug(format(method, "b4 first connect()", "tidyConnect"));
            log.debug(tidyConnect);
            log.debug(format(method,
                             null,
                             "PUBCOOKIE_LOGINPAGE_URL",
                             PUBCOOKIE_LOGINPAGE_URL));
        }
        tidyConnect.connect(PUBCOOKIE_LOGINPAGE_URL,
                            NO_REQUEST_PARAMETERS,
                            NO_COOKIES,
                            TRUSTSTORE_LOCATION,
                            TRUSTSTORE_PASSWORD);
        if (!tidyConnect.completedFully()) {
            if (log.isInfoEnabled()) {
                log.info(format(method, "form page did not load"));
            }
        } else {
            Cookie[] formpageCookies = tidyConnect.getResponseCookies();
            Node formpageDocument = tidyConnect.getResponseDocument();
            String action =
                    getAction(formpageDocument, PUBCOOKIE_LOGINPAGE_FORM_NAME);
            if (log.isDebugEnabled()) {
                log.debug(format(method, "action", action));
            }
            Map formpageFields = getFormFields(formpageDocument);
            Iterator iter = null;

            if (log.isDebugEnabled()) {
                iter = formpageFields.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    log.debug(format(method, null, key, (String) formpageFields
                            .get(key)));
                }
            }

            formpageFields.put(PUBCOOKIE_LOGINPAGE_INPUT_NAME_USERID,
                               cacheElement.getUserid());
            formpageFields.put(PUBCOOKIE_LOGINPAGE_INPUT_NAME_PASSWORD,
                               password);

            if (log.isDebugEnabled()) {
                iter = formpageFields.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    log.debug(format(method,
                                     " form field after",
                                     key,
                                     (String) formpageFields.get(key)));
                }
            }

            tidyConnect = new ConnectPubcookie();
            if (log.isDebugEnabled()) {
                log.debug(format(method, "b4 second connect()"));
            }
            tidyConnect.connect(action,
                                formpageFields,
                                formpageCookies,
                                TRUSTSTORE_LOCATION,
                                TRUSTSTORE_PASSWORD);
            if (!tidyConnect.completedFully()) {
                if (log.isDebugEnabled()) {
                    log.debug(format(method, "result page did not load"));
                }
            } else {
                Cookie[] resultpageCookies = tidyConnect.getResponseCookies();
                if (log.isDebugEnabled()) {
                    log.debug(format(method, " cookies receieved", "n", Integer
                            .toString(resultpageCookies.length)));
                }
                for (Cookie cookie : resultpageCookies) {
                    if (log.isDebugEnabled()) {
                        log.debug(format(method,
                                         "another cookie",
                                         "cookie name" + cookie.getName()));
                        log.debug(format(method,
                                         "another cookie",
                                         "length",
                                         Integer.toString(cookie.getName()
                                                 .length())));
                    }
                    if (PUBCOOKIE_NAME.equals(cookie.getName())) {
                        if (log.isInfoEnabled()) {
                            log.debug(format(method,
                                             " found pubcookie login cookie"));
                        }
                        authenticated = Boolean.TRUE;
                        break;
                    }
                }
                if (authenticated == null) {
                    authenticated = Boolean.FALSE;
                } else {
                    if (!authenticated.booleanValue()) {
                        if (log.isDebugEnabled()) {
                            log
                                    .debug(format(method,
                                                  "didn't find a pubcookie login cookie"));
                        }
                    }
                }
            }
            cacheElement.populate(authenticated, null, null, null);
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

}
