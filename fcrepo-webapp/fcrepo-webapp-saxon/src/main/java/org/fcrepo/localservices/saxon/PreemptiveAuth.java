package org.fcrepo.localservices.saxon;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;


public class PreemptiveAuth implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, HttpContext context)
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

        authState.update(authScheme, creds);
    }

}