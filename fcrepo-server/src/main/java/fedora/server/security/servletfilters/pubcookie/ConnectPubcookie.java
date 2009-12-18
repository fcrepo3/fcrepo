/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters.pubcookie;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Node;

import org.w3c.tidy.Tidy;

//import fedora.server.security.servletfilters.HttpTidyConnect;

/**
 * @author Bill Niebel
 */
public class ConnectPubcookie {

    private final Log log = LogFactory.getLog(ConnectPubcookie.class);

    private boolean completedFully = false;

    private Node responseDocument = null;

    private Cookie[] responseCookies = null;

    Header[] responseCookies2 = null;

    public final boolean completedFully() {
        return completedFully;
    }

    public final Node getResponseDocument() {
        return responseDocument;
    }

    public final Cookie[] getResponseCookies() {
        log.debug(this.getClass().getName() + ".getResponseCookies() "
                + "cookies are:");
        for (Cookie element : responseCookies) {
            log.debug(this.getClass().getName() + ".getResponseCookies() "
                    + "cookie==" + element);
        }
        return responseCookies;
    }

    private static final HttpMethodBase setup(HttpClient client,
                                              URL url,
                                              Map requestParameters,
                                              Cookie[] requestCookies) {
        LogFactory.getLog(ConnectPubcookie.class).debug(ConnectPubcookie.class
                .getName()
                + ".setup()");
        HttpMethodBase method = null;
        if (requestParameters == null) {
            LogFactory.getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()"
                            + " requestParameters == null");
            method = new GetMethod(url.toExternalForm());
            //GetMethod is superclass to ExpectContinueMethod, so we don't require method.setUseExpectHeader(false);
            LogFactory.getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()"
                            + " after getting method");
        } else {
            LogFactory.getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()"
                            + " requestParameters != null");
            method = new PostMethod(url.toExternalForm()); // "http://localhost:8080/"
            LogFactory.getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()"
                            + " after getting method");

            //XXX method.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, false); //new way
            //XXX method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, 10000);            
            //XXX method.getParams().setVersion(HttpVersion.HTTP_0_9); //or HttpVersion.HTTP_1_0 HttpVersion.HTTP_1_1

            LogFactory.getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()"
                            + " after setting USE_EXPECT_CONTINUE");

            //PostMethod is subclass of ExpectContinueMethod, so we require here:            
            //((PostMethod)method).setUseExpectHeader(false);
            //client.setTimeout(30000); // increased from 10000 as temp fix; 2005-03-17 wdn5e
            //HttpClientParams httpClientParams = new HttpClientParams();
            //httpClientParams.setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true); //old way
            //httpClientParams.setIntParameter(HttpMethodParams.SO_TIMEOUT, 30000);

            LogFactory
                    .getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()" + " A");

            Part[] parts = new Part[requestParameters.size()];
            Iterator iterator = requestParameters.keySet().iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                String fieldName = (String) iterator.next();
                String fieldValue = (String) requestParameters.get(fieldName);
                StringPart stringPart = new StringPart(fieldName, fieldValue);
                parts[i] = stringPart;
                LogFactory.getLog(ConnectPubcookie.class)
                        .debug(ConnectPubcookie.class.getName() + ".setup()"
                                + " part[" + i + "]==" + fieldName + "="
                                + fieldValue);

                ((PostMethod) method).addParameter(fieldName, fieldValue); //old way
            }

            LogFactory
                    .getLog(ConnectPubcookie.class)
                    .debug(ConnectPubcookie.class.getName() + ".setup()" + " B");

            //XXX MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(parts, method.getParams());
            // ((PostMethod)method).setRequestEntity(multipartRequestEntity); //new way            
        }
        //method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        HttpState state = client.getState();
        for (Cookie cookie : requestCookies) {
            state.addCookie(cookie);
        }
        //method.setFollowRedirects(true); this is disallowed at runtime, so redirect won't be honored

        LogFactory.getLog(ConnectPubcookie.class).debug(ConnectPubcookie.class
                .getName()
                + ".setup()" + " C");
        LogFactory.getLog(ConnectPubcookie.class).debug(ConnectPubcookie.class
                .getName()
                + ".setup()" + " method==" + method);
        LogFactory.getLog(ConnectPubcookie.class).debug(ConnectPubcookie.class
                .getName()
                + ".setup()" + " method==" + method.toString());
        return method;
    }

    public final void connect(String urlString,
                              Map requestParameters,
                              Cookie[] requestCookies,
                              String truststoreLocation,
                              String truststorePassword) {
        log.debug(this.getClass().getName() + ".connect() " + " url=="
                + urlString + " requestParameters==" + requestParameters
                + " requestCookies==" + requestCookies);
        responseCookies2 = null;
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            log.error(this.getClass().getName() + ".connect() "
                    + "bad configured url==" + urlString);
        }

        if (urlString.startsWith("https:") && null != truststoreLocation
                && !"".equals(truststoreLocation) && null != truststorePassword
                && !"".equals(truststorePassword)) {
            log.debug("setting " + FilterPubcookie.TRUSTSTORE_LOCATION_KEY
                    + " to " + truststoreLocation);
            System.setProperty(FilterPubcookie.TRUSTSTORE_LOCATION_KEY,
                               truststoreLocation);
            log.debug("setting " + FilterPubcookie.TRUSTSTORE_PASSWORD_KEY
                    + " to " + truststorePassword);
            System.setProperty(FilterPubcookie.TRUSTSTORE_PASSWORD_KEY,
                               truststorePassword);

            log.debug("setting " + FilterPubcookie.KEYSTORE_LOCATION_KEY
                    + " to " + truststoreLocation);
            System.setProperty(FilterPubcookie.KEYSTORE_LOCATION_KEY,
                               truststoreLocation);
            log.debug("setting " + FilterPubcookie.KEYSTORE_PASSWORD_KEY
                    + " to " + truststorePassword);
            System.setProperty(FilterPubcookie.KEYSTORE_PASSWORD_KEY,
                               truststorePassword);

            System.setProperty("javax.net.debug",
                               "ssl,handshake,data,trustmanager");

        } else {
            log.debug("DIAGNOSTIC urlString==" + urlString);
            log.debug("didn't set " + FilterPubcookie.TRUSTSTORE_LOCATION_KEY
                    + " to " + truststoreLocation);
            log.debug("didn't set " + FilterPubcookie.TRUSTSTORE_PASSWORD_KEY
                    + " to " + truststorePassword);
        }

        /*
         * log.debug("\n-a-"); Protocol easyhttps = null; try { easyhttps = new
         * Protocol("https", (ProtocolSocketFactory) new
         * EasySSLProtocolSocketFactory(), 443); } catch (Throwable t) {
         * log.debug(t); log.debug(t.getMessage()); if (t.getCause() != null)
         * log.debug(t.getCause().getMessage()); } log.debug("\n-b-");
         * Protocol.registerProtocol("https", easyhttps); log.debug("\n-c-");
         */

        HttpClient client = new HttpClient();
        log.debug(this.getClass().getName() + ".connect() "
                + " b4 calling setup");
        log.debug(this.getClass().getName() + ".connect() requestCookies=="
                + requestCookies);
        HttpMethodBase method =
                setup(client, url, requestParameters, requestCookies);
        log.debug(this.getClass().getName() + ".connect() "
                + " after calling setup");
        int statusCode = 0;
        try {
            log.debug(this.getClass().getName() + ".connect() "
                    + " b4 calling executeMethod");
            client.executeMethod(method);
            log.debug(this.getClass().getName() + ".connect() "
                    + " after calling executeMethod");
            statusCode = method.getStatusCode();
            log.debug(this.getClass().getName() + ".connect() "
                    + "(with configured url) statusCode==" + statusCode);
        } catch (Exception e) {
            log.error(this.getClass().getName() + ".connect() "
                    + "failed original connect, url==" + urlString);
            log.error(e);
            log.error(e.getMessage());
            if (e.getCause() != null) {
                log.error(e.getCause().getMessage());
            }
            e.printStackTrace();
        }

        log.debug(this.getClass().getName() + ".connect() " + " status code=="
                + statusCode);

        if (302 == statusCode) {
            Header redirectHeader = method.getResponseHeader("Location");
            if (redirectHeader != null) {
                String redirectString = redirectHeader.getValue();
                if (redirectString != null) {
                    URL redirectURL = null;
                    try {
                        redirectURL = new URL(redirectString);
                        method =
                                setup(client,
                                      redirectURL,
                                      requestParameters,
                                      requestCookies);
                    } catch (MalformedURLException mue) {
                        log.error(this.getClass().getName() + ".connect() "
                                + "bad redirect, url==" + urlString);
                    }
                    statusCode = 0;
                    try {
                        client.executeMethod(method);
                        statusCode = method.getStatusCode();
                        log.debug(this.getClass().getName() + ".connect() "
                                + "(on redirect) statusCode==" + statusCode);
                    } catch (Exception e) {
                        log.error(this.getClass().getName() + ".connect() "
                                + "failed redirect connect");
                    }
                }
            }
        }
        if (statusCode == 200) { // this is either the original, non-302, status code or the status code after redirect
            log.debug(this.getClass().getName() + ".connect() "
                    + "status code 200");
            String content = null;
            try {
                log.debug(this.getClass().getName() + ".connect() "
                        + "b4 gRBAS()");
                content = method.getResponseBodyAsString();
                log.debug(this.getClass().getName() + ".connect() "
                        + "after gRBAS() content==" + content);
            } catch (IOException e) {
                log.error(this.getClass().getName() + ".connect() "
                        + "couldn't get content");
                return;
            }
            if (content == null) {
                log.error(this.getClass().getName()
                        + ".connect() content==null");
                return;
            } else {
                log.debug(this.getClass().getName()
                        + ".connect() content != null, about to new Tidy");
                Tidy tidy = null;
                try {
                    tidy = new Tidy();
                } catch (Throwable t) {
                    log.debug("new Tidy didn't");
                    log.debug(t);
                    log.debug(t.getMessage());
                    if (t != null) {
                        log.debug(t.getCause().getMessage());
                    }
                }
                log.debug(this.getClass().getName()
                        + ".connect() after newing Tidy, tidy==" + tidy);
                byte[] inputBytes = content.getBytes();
                log.debug(this.getClass().getName() + ".connect() A1");
                ByteArrayInputStream inputStream =
                        new ByteArrayInputStream(inputBytes);
                log.debug(this.getClass().getName() + ".connect() A2");
                responseDocument = tidy.parseDOM(inputStream, null); //use returned root node as only output
                log.debug(this.getClass().getName() + ".connect() A3");
            }
            log.debug(this.getClass().getName() + ".connect() "
                    + "b4 getState()");
            HttpState state = client.getState();
            log.debug(this.getClass().getName() + ".connect() state==" + state);
            try {
                responseCookies2 = method.getRequestHeaders();
                log.debug(this.getClass().getName()
                        + ".connect() just got headers");
                for (Header element : responseCookies2) {
                    log.debug(this.getClass().getName() + ".connect() header=="
                            + element);
                }
                responseCookies = state.getCookies();
                log.debug(this.getClass().getName()
                        + ".connect() responseCookies==" + responseCookies);
            } catch (Throwable t) {
                log.error(this.getClass().getName() + ".connect() exception=="
                        + t.getMessage());
                if (t.getCause() != null) {
                    log.error(this.getClass().getName() + ".connect() cause=="
                            + t.getCause().getMessage());
                }
            }
            completedFully = true;
            log.debug(this.getClass().getName() + ".connect() completedFully=="
                    + completedFully);
        }
    }

}
