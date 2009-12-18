/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A representation of a normalized URI, per RFC3986.
 *
 * @author Edwin Shin
 * @since 3.0
 * @see RFC3986
 * @version $Id$
 */
public class NormalizedURI {

    private URI uri;

    /**
     * see http://www.iana.org/assignments/uri-schemes
     * see http://www.iana.org/assignments/port-numbers
     */
    private static final HashMap<String, Integer> defaultPorts = new HashMap<String, Integer>();

    static {
        defaultPorts.put("acap", new Integer(2628));
        defaultPorts.put("afs", new Integer(1483));
        defaultPorts.put("dict", new Integer(674));
        defaultPorts.put("ftp", new Integer(21));
        defaultPorts.put("go", new Integer(1096));
        defaultPorts.put("gopher", new Integer(70));
        defaultPorts.put("http", new Integer(80));
        defaultPorts.put("https", new Integer(443));
        defaultPorts.put("imap", new Integer(143));
        defaultPorts.put("ipp", new Integer(631));
        defaultPorts.put("iris.beep", new Integer(702));
        defaultPorts.put("ldap", new Integer(389));
        defaultPorts.put("telnet", new Integer(23));
        defaultPorts.put("mtqp", new Integer(1038));
        defaultPorts.put("mupdate", new Integer(3905));
        defaultPorts.put("nfs", new Integer(2049));
        defaultPorts.put("nntp", new Integer(119));
        defaultPorts.put("pop", new Integer(110));
        defaultPorts.put("prospero", new Integer(1525));
        defaultPorts.put("rtsp", new Integer(554));
        defaultPorts.put("smtp", new Integer(25));
        defaultPorts.put("sip", new Integer(5060));
        defaultPorts.put("sips", new Integer(5061));
        defaultPorts.put("snmp", new Integer(161));
        defaultPorts.put("soap.beep", new Integer(605));
        defaultPorts.put("soap.beeps", new Integer(605));
        defaultPorts.put("telnet", new Integer(23));
        defaultPorts.put("tftp", new Integer(69));
        defaultPorts.put("vemmi", new Integer(575));
        defaultPorts.put("wais", new Integer(210));
        defaultPorts.put("xmlrpc.beep", new Integer(602));
        defaultPorts.put("xmlrpc.beeps", new Integer(602));
        defaultPorts.put("z39.50r", new Integer(210));
        defaultPorts.put("z39.50s", new Integer(210));
    }

    private static final Pattern PERCENT_ENCODED = Pattern.compile("%([a-z0-9]{2})");

    public NormalizedURI(String uri) throws URISyntaxException {
        this(new URI(uri));
    }

    public NormalizedURI(URI uri) {
        this.uri = uri;
    }

    public void normalize() {
        normalizeSyntax();
        normalizeByScheme();
        normalizeByProtocol();
    }

    /**
     * Performs the following:
     *  Case Normalization
     *  Percent-Encoding Normalization
     *  Path Segment Normalization
     *
     */
    public void normalizeSyntax() {
        normalizeCase();
        normalizePercentEncoding();
        normalizePathSegment();
    }

    /**
     * Case Normalization
     * @see RFC3986 6.2.2.1
     *
     */
    public void normalizeCase() {
        // Scheme and host should be lowercase
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String rURI = toString();
        if (scheme != null) {
            rURI = rURI.replaceFirst(scheme, scheme.toLowerCase());
        }
        if (host != null) {
            rURI = rURI.replaceFirst(host, host.toLowerCase());
        }

        // Percent-encoded characters should be uppercase
        if (rURI.indexOf('%') != -1) {
            Matcher m = PERCENT_ENCODED.matcher(rURI);

            StringBuffer sb = new StringBuffer();
            int lastEnd = 0;
            while(m.find()) {
                sb.append(rURI.substring(lastEnd, m.start()));
                sb.append(m.group().toUpperCase());
                lastEnd = m.end();
            }
            sb.append(rURI.substring(lastEnd, rURI.length()));
            rURI = sb.toString();
        }
        uri = URI.create(rURI);
    }

    /**
     * Percent-Encoding Normalization
     * @see RFC3986 6.2.2.2
     *
     */
    public void normalizePercentEncoding() {
        try {
            uri = new URI(uri.getScheme(),
                          uri.getSchemeSpecificPart(),
                          uri.getFragment());
        } catch (URISyntaxException e) {
            // This should never be reached
            e.printStackTrace();
        }
    }

    /**
     * Path Segment Normalization
     * @see RFC3986 6.2.2.3
     *
     */
    public void normalizePathSegment() {
        uri = uri.normalize();
    }

    /**
     * Scheme-Based Normalization
     * @see RFC3986 6.2.3
     *
     */
    public void normalizeByScheme() {
        String rURI = toString();
        String scheme = uri.getScheme();
        String authority = uri.getAuthority();
        String host = uri.getHost();
        int port = uri.getPort();
        String path = uri.getPath();

        if (port == defaultPort(scheme)) {
            rURI = rURI.replaceFirst(":" + port, "");
            try {
                uri = new URI(rURI);
            } catch (URISyntaxException e) {
                // This should never be reached
                e.printStackTrace();
            }
        }

        if (port == -1 && authority != null && authority.endsWith(":")) {
            rURI = rURI.replaceFirst(authority, authority.substring(0, authority.length() -1));
            try {
                uri = new URI(rURI);
            } catch (URISyntaxException e) {
                // This should never be reached
                e.printStackTrace();
            }
        }

        if (path == null || path.length() == 0) {
            if (host != null) {
                rURI = rURI.replaceFirst(host, host + '/');
            } else {
                rURI = rURI.replaceFirst(authority, authority + '/');
            }
            uri = URI.create(rURI);
        }
    }

    /**
     * Protocol-Based Normalization
     * @see RFC3986 6.2.4
     *
     */
    public void normalizeByProtocol() {
        //TODO noop
    }

    @Override
    public String toString() {
        return uri.toASCIIString();
    }

    public URI toURI() {
        return uri;
    }

    /**
     * Return the default port used by a given scheme.
     *
     * @param the scheme, e.g. http
     * @return the port number, or -1 if unknown
     */
    private final static int defaultPort(String scheme) {
        if (scheme == null) {
            return -1;
        }
        Integer port = defaultPorts.get(scheme.trim().toLowerCase());
        return (port != null) ? port.intValue() : -1;
    }
}
