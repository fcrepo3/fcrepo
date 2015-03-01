/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.util.concurrent.Callable;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.http.HttpInputStream;

public class GetCallable implements Callable<HttpInputStream> {
    private final FedoraClient m_client;
    private final String m_uri;
    public String lastType = null;
    public long lastLength = -1;
    public GetCallable(FedoraClient client, String uri) {
        m_client = client;
        m_uri = uri;
    }

    @Override
    public HttpInputStream call() throws Exception {
        HttpInputStream in = m_client.get(m_uri, true);
        lastType = in.getContentType();
        lastLength = in.getContentLength();
        if (lastLength == -1) {
            lastLength = 0;
            while(in.read() != -1) lastLength++;
        }
        in.close();
        return in;
    }

}
