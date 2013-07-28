package org.fcrepo.common.http;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;


public class PreemptiveAuth extends DefaultHttpClient {

    public PreemptiveAuth() {
        super();
    }
    
    public PreemptiveAuth(ClientConnectionManager cmgr) {
        super(cmgr);
    }
    
    @Override
    public HttpContext createHttpContext() {
        HttpContext result = super.createHttpContext();
        AuthCache authCache =
            (AuthCache) result.getAttribute(ClientContext.AUTH_CACHE);
        if (authCache == null) {
            final BasicScheme scheme = new BasicScheme();
            final BasicAuthCache basicCache = new BasicAuthCache(){
                @Override
                public AuthScheme get(final HttpHost host) {
                    if (host == null)
                        throw new IllegalArgumentException("null host not allowed.");
                    return scheme;
                }
            };
            result.setAttribute(ClientContext.AUTH_CACHE, basicCache);
        }
        return result;
    }

}
