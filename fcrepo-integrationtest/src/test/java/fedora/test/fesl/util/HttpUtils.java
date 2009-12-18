
package fedora.test.fesl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class HttpUtils {

    private static final Logger log = Logger.getLogger(HttpUtils.class);

    private DefaultHttpClient client = null;

    private HttpHost httpHost = null;

    private BasicHttpContext httpContext = null;

    private BasicScheme basicAuth = null;

    public HttpUtils(String baseURL, String username, String password)
            throws Exception {
        try {
            URL url = new URL(baseURL);

            client = new DefaultHttpClient();
            basicAuth = new BasicScheme();
            httpContext = new BasicHttpContext();
            httpHost =
                    new HttpHost(url.getHost(), url.getPort(), url
                            .getProtocol());

            if (username != null && password != null) {
                httpContext.setAttribute("preemptive-auth", basicAuth);

                // Add as the first request interceptor
                client.addRequestInterceptor(new PreemptiveAuth(), 0);
                AuthScope authScope =
                        new AuthScope(url.getHost(),
                                      url.getPort(),
                                      AuthScope.ANY_REALM);
                Credentials credentials =
                        new UsernamePasswordCredentials(username, password);
                client.getCredentialsProvider().setCredentials(authScope,
                                                               credentials);
            }
        } catch (Exception e) {
            log.error("Failed to instantiate HttpUtils.");
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

        if (log.isDebugEnabled()) {
            log.debug("getting url: " + url);
        }

        // add headers to request
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (log.isDebugEnabled()) {
                    log.debug("adding header: " + header + " = " + value);
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

        if (log.isDebugEnabled()) {
            log.debug("getting url: " + url);
        }

        // add headers to request if necessary
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (log.isDebugEnabled()) {
                    log.debug("adding header: " + header + " = " + value);
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

        if (log.isDebugEnabled()) {
            log.debug("getting url: " + url);
        }

        // add headers to request if necessary
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (log.isDebugEnabled()) {
                    log.debug("adding header: " + header + " = " + value);
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

        if (log.isDebugEnabled()) {
            log.debug("getting url: " + url);
        }

        // add headers to request
        if (headers != null && headers.size() > 0) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                request.addHeader(header, value);

                if (log.isDebugEnabled()) {
                    log.debug("adding header: " + header + " = " + value);
                }
            }
        }

        return process(request);
    }

    private String process(HttpRequest request) throws IOException,
            AuthorizationDeniedException, ClientProtocolException {
        if (log.isDebugEnabled()) {
            log.debug("request line: " + request.getRequestLine());
        }

        HttpResponse response = client.execute(httpHost, request, httpContext);
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

            body = new String(res.toByteArray());
            if (body.contains("Fedora: 403")) {
                throw new AuthorizationDeniedException("Authorization Denied");
            }
        }

        if (sc < 200 || sc >= 400) {
            throw new ClientProtocolException("Error [Status Code = " + sc
                    + "]" + ": " + phrase);
        }

        return body;
    }

    private class PreemptiveAuth
            implements HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context)
                throws HttpException, IOException {
            AuthState authState =
                    (AuthState) context
                            .getAttribute(ClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() != null) {
                return;
            }

            AuthScheme authScheme =
                    (AuthScheme) context.getAttribute("preemptive-auth");
            if (authScheme == null) {
                return;
            }

            CredentialsProvider credsProvider =
                    (CredentialsProvider) context
                            .getAttribute(ClientContext.CREDS_PROVIDER);
            HttpHost targetHost =
                    (HttpHost) context
                            .getAttribute(ExecutionContext.HTTP_TARGET_HOST);

            Credentials creds =
                    credsProvider.getCredentials(new AuthScope(targetHost
                            .getHostName(), targetHost.getPort()));
            if (creds == null) {
                return;
            }

            authState.setAuthScheme(authScheme);
            authState.setCredentials(creds);
        }
    }
}
