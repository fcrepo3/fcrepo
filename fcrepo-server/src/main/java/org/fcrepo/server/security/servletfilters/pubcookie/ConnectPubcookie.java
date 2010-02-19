/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters.pubcookie;

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

import org.w3c.dom.Node;

import org.w3c.tidy.Tidy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
public class ConnectPubcookie {

    private static final Logger logger =
            LoggerFactory.getLogger(ConnectPubcookie.class);

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
        logger.debug(this.getClass().getName() + ".getResponseCookies() "
                + "cookies are:");
        for (Cookie element : responseCookies) {
            logger.debug(this.getClass().getName() + ".getResponseCookies() "
                    + "cookie==" + element);
        }
        return responseCookies;
    }

    private static final HttpMethodBase setup(HttpClient client,
                                              URL url,
                                              Map requestParameters,
                                              Cookie[] requestCookies) {
        logger.debug("Entered setup()");
        HttpMethodBase method = null;
        if (requestParameters == null) {
            logger.debug("Using GetMethod; requestParameters == null");
            method = new GetMethod(url.toExternalForm());
        } else {
            logger.debug("Using PostMethod; requestParameters specified");
            method = new PostMethod(url.toExternalForm()); // "http://localhost:8080/"

            Part[] parts = new Part[requestParameters.size()];
            Iterator iterator = requestParameters.keySet().iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                String fieldName = (String) iterator.next();
                String fieldValue = (String) requestParameters.get(fieldName);
                StringPart stringPart = new StringPart(fieldName, fieldValue);
                parts[i] = stringPart;
                logger.debug("Adding Post parameter {} = {}", fieldName, fieldValue);
                ((PostMethod) method).addParameter(fieldName, fieldValue); //old way
            }
        }
        HttpState state = client.getState();
        for (Cookie cookie : requestCookies) {
            state.addCookie(cookie);
        }
        return method;
    }

    public final void connect(String urlString,
                              Map requestParameters,
                              Cookie[] requestCookies,
                              String truststoreLocation,
                              String truststorePassword) {
        if (logger.isDebugEnabled()) {
            logger.debug("Entered .connect() " + " url=="
                    + urlString + " requestParameters==" + requestParameters
                    + " requestCookies==" + requestCookies);
        }
        responseCookies2 = null;
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            logger.error("Malformed url: " + urlString, mue);
        }

        if (urlString.startsWith("https:") && null != truststoreLocation
                && !"".equals(truststoreLocation) && null != truststorePassword
                && !"".equals(truststorePassword)) {
            logger.debug("setting " + FilterPubcookie.TRUSTSTORE_LOCATION_KEY
                    + " to " + truststoreLocation);
            System.setProperty(FilterPubcookie.TRUSTSTORE_LOCATION_KEY,
                               truststoreLocation);
            logger.debug("setting " + FilterPubcookie.TRUSTSTORE_PASSWORD_KEY
                    + " to " + truststorePassword);
            System.setProperty(FilterPubcookie.TRUSTSTORE_PASSWORD_KEY,
                               truststorePassword);

            logger.debug("setting " + FilterPubcookie.KEYSTORE_LOCATION_KEY
                    + " to " + truststoreLocation);
            System.setProperty(FilterPubcookie.KEYSTORE_LOCATION_KEY,
                               truststoreLocation);
            logger.debug("setting " + FilterPubcookie.KEYSTORE_PASSWORD_KEY
                    + " to " + truststorePassword);
            System.setProperty(FilterPubcookie.KEYSTORE_PASSWORD_KEY,
                               truststorePassword);

            System.setProperty("javax.net.debug",
                               "ssl,handshake,data,trustmanager");

        } else {
            logger.debug("DIAGNOSTIC urlString==" + urlString);
            logger.debug("didn't set " + FilterPubcookie.TRUSTSTORE_LOCATION_KEY
                    + " to " + truststoreLocation);
            logger.debug("didn't set " + FilterPubcookie.TRUSTSTORE_PASSWORD_KEY
                    + " to " + truststorePassword);
        }

        HttpClient client = new HttpClient();
        logger.debug(".connect() requestCookies==" + requestCookies);
        HttpMethodBase method =
                setup(client, url, requestParameters, requestCookies);
        int statusCode = 0;
        try {
            client.executeMethod(method);
            statusCode = method.getStatusCode();
        } catch (Exception e) {
            logger.error("failed original connect, url==" + urlString, e);
        }

        logger.debug("status code==" + statusCode);

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
                        logger.error(".connect() malformed redirect url: " + urlString);
                    }
                    statusCode = 0;
                    try {
                        client.executeMethod(method);
                        statusCode = method.getStatusCode();
                        logger.debug(".connect() (on redirect) statusCode==" + statusCode);
                    } catch (Exception e) {
                        logger.error(".connect() "
                                + "failed redirect connect");
                    }
                }
            }
        }
        if (statusCode == 200) { // this is either the original, non-302, status code or the status code after redirect
            String content = null;
            try {
                content = method.getResponseBodyAsString();
            } catch (IOException e) {
                logger.error("Error getting content", e);
                return;
            }
            if (content == null) {
                logger.error("Content is null");
                return;
            } else {
                Tidy tidy = null;
                try {
                    tidy = new Tidy();
                } catch (Throwable t) {
                    logger.error("Error creating Tidy instance?!", t);
                }
                byte[] inputBytes = content.getBytes();
                ByteArrayInputStream inputStream =
                        new ByteArrayInputStream(inputBytes);
                responseDocument = tidy.parseDOM(inputStream, null); //use returned root node as only output
            }
            HttpState state = client.getState();
            try {
                responseCookies2 = method.getRequestHeaders();
                if (logger.isDebugEnabled()) {
                    for (Header element : responseCookies2) {
                        logger.debug("Header: {}={}", element.getName(), element.getValue());
                    }
                }
                responseCookies = state.getCookies();
                logger.debug(this.getClass().getName()
                        + ".connect() responseCookies==" + responseCookies);
            } catch (Throwable t) {
                logger.error(this.getClass().getName() + ".connect() exception=="
                        + t.getMessage());
                if (t.getCause() != null) {
                    logger.error(this.getClass().getName() + ".connect() cause=="
                            + t.getCause().getMessage());
                }
            }
            completedFully = true;
            logger.debug(this.getClass().getName() + ".connect() completedFully=="
                    + completedFully);
        }
    }

}
