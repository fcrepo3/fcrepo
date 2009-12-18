/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.net.MalformedURLException;
import java.net.URL;

import fedora.server.Context;

/**
 * Simple data transfer object for the content manager. 
 * This should avoid breaking the content manager interface every
 * time the parameters change. 
 * 
 * @version $Id$
 *
 */
public class ContentManagerParams {
    private String url;
    private String mimeType;
    private String username;
    private String password;
    private String protocol;
    private boolean bypassBackend = false;
    private Context context;
    
    
    public ContentManagerParams(){
    }
    
    public ContentManagerParams(String url, String mimeType, String username, String password){
        setUrl(url);
        this.mimeType = mimeType;
        this.username = username;
        this.password = password;
    }

    public ContentManagerParams(String url){
        setUrl(url);
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
        try {
            this.protocol = new URL(url).getProtocol();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public void setBypassBackend(boolean b) {
        bypassBackend = b;
    }
    
    public boolean isBypassBackend() {
        return bypassBackend;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }
}

