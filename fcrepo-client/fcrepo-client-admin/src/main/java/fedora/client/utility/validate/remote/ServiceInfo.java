/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.client.utility.validate.remote;

import java.net.URL;

/**
 * Holds the information necessary to open an API-M or API-A client.
 * 
 * @author Jim Blake
 */
public class ServiceInfo {

    private final URL baseUrl;

    private final String username;

    private final String password;

    public ServiceInfo(URL baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public String getBaseUrlString() {
        return baseUrl.toExternalForm();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
