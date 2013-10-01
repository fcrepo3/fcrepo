
package org.fcrepo.test.fesl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.fcrepo.common.http.PreemptiveAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

    private static final Logger logger =
            LoggerFactory.getLogger(HttpUtils.class);

    private DefaultHttpClient client = null;

    private HttpHost httpHost = null;

    public HttpUtils(String baseURL, String username, String password)
            throws Exception {

        try {
            URL url = new URL(baseURL);

            httpHost =
                    new HttpHost(url.getHost(), url.getPort(), url
                            .getProtocol());

            if (username != null && password != null) {
                client = new PreemptiveAuth();

                AuthScope authScope =
                     new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
                Credentials credentials =
                        new UsernamePasswordCredentials(username, password);
                client.getCredentialsProvider().setCredentials(authScope,
                                                               credentials);
            } else {
                client = new DefaultHttpClient();
            }
            // default timeouts are zero, so set some
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 1000 * 30); // 60 seconds
            HttpConnectionParams.setSoTimeout(client.getParams(), 1000 * 30); // 60 seconds
        } catch (Exception e) {
            logger.error("Failed to instantiate HttpUtils.", e);
            throw e;
        }
    }

    public String get(String url) throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        return get(url, null);
    }

    public String get(String url, Map<String, String> headers)
            throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        // create request
        HttpGet request = new HttpGet(url);

        if (logger.isDebugEnabled()) {
            logger.debug("getting url: " + url);
        }

        // add headers to request
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (logger.isDebugEnabled()) {
                    logger.debug("adding header: " + header + " = " + value);
                }
            }
        }

        return process(request);
    }

    public String post(String url) throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        return post(url, null, null);
    }

    public String post(String url, Map<String, String> headers)
            throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        return post(url, headers, null);
    }

    public String post(String url, Map<String, String> headers, byte[] data)
            throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        // create request
        HttpPost request = new HttpPost(url);

        // add data to request if necessary
        if (data != null) {
            ByteArrayEntity entity = new ByteArrayEntity(data);
            entity.setChunked(true);
            entity.setContentType("text/xml");
            request.setEntity(entity);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getting url: " + url);
        }

        // add headers to request if necessary
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (logger.isDebugEnabled()) {
                    logger.debug("adding header: " + header + " = " + value);
                }
            }
        }

        return process(request);
    }

    public String put(String url) throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        return put(url, null, null);
    }

    public String put(String url, Map<String, String> headers)
            throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        return put(url, headers, null);
    }

    public String put(String url, Map<String, String> headers, byte[] data)
            throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        // create request
        HttpPut request = new HttpPut(url);

        // add data to request if necessary
        if (data != null) {
            ByteArrayEntity entity = new ByteArrayEntity(data);
            entity.setChunked(true);
            entity.setContentType("text/xml");
            request.setEntity(entity);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getting url: " + url);
        }

        // add headers to request if necessary
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (logger.isDebugEnabled()) {
                    logger.debug("adding header: " + header + " = " + value);
                }
            }
        }

        return process(request);
    }

    public String delete(String url, Map<String, String> headers)
            throws ClientProtocolException, IOException,
            AuthorizationDeniedException {
        // create request
        HttpDelete request = new HttpDelete(url);

        if (logger.isDebugEnabled()) {
            logger.debug("getting url: " + url);
        }

        // add headers to request
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (logger.isDebugEnabled()) {
                    logger.debug("adding header: " + header + " = " + value);
                }
            }
        }

        return process(request);
    }

    private String process(HttpRequest request) throws IOException,
            AuthorizationDeniedException, ClientProtocolException {
        return process(request, httpHost);
    }

    private String process(HttpRequest request, HttpHost host) throws IOException,
            AuthorizationDeniedException, ClientProtocolException {
        if (logger.isDebugEnabled()) {
            logger.debug("request line: " + request.getRequestLine());
        }

        HttpResponse response = client.execute(host, request);
        int sc = response.getStatusLine().getStatusCode();

        String phrase = response.getStatusLine().getReasonPhrase();

        String body = "";
        if (response.getEntity() != null) {
            InputStream is = response.getEntity().getContent();

            ByteArrayOutputStream res = new ByteArrayOutputStream();
            int len = 0;
            byte[] buf = new byte[1024];
            while ((len = is.read(buf)) >= 0) {
                res.write(buf, 0, len);
            }
            // close input stream - with Akubra on Windows failing to close the stream means the object can't be purged
            is.close();

            body = new String(res.toByteArray());
            if (body.contains("Fedora: 403")) {
                throw new AuthorizationDeniedException("Authorization Denied");
            }
        }

        if (sc < 200 || sc >= 400) {
            throw new ClientProtocolException("Error [Status Code = " + sc
                    + "]" + ": " + phrase);
        }

        if (sc == 302) {
            URL redir = new URL(response.getHeaders("Location")[0].getValue());

            // XXX: assume we're just changing the host, port, and/or protocol
            return process(request,
                           new HttpHost(redir.getHost(), redir.getPort(), redir
                                   .getProtocol()));
        }

        return body;
    }

    public void shutdown() {
        client.getConnectionManager().shutdown();
    }
}
