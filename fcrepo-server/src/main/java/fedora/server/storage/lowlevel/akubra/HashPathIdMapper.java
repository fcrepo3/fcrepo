/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.lowlevel.akubra;

import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.akubraproject.map.IdMapper;

import fedora.server.utilities.MD5Utility;

/**
 * Provides a hash-based <code>file:</code> mapping for any URI.
 * <p>
 * The path component of each internal URI is derived from an MD5 hash of
 * the external URI. The filename component is a reversible encoding of the
 * external URI that is safe to use as a filename on modern filesystems.
 * <p>
 * <h2>Hash Path Patterns</h2>
 * The pattern given at construction time determines how the path component
 * of each internal URI will be composed. Within the pattern, the # character
 * is a stand-in for a hexadecimal [0-f] digit from the MD5 hash of the
 * external id.
 * <p>
 * Patterns:
 * <ul>
 *   <li> must consist only of # and / characters.</li>
 *   <li> must contain between 1 and 32 # characters.</li>
 *   <li> must not begin or end with the / character.</li>
 *   <li> must not contain consecutive / characters.</li>
 * </ul>
 * <p>
 * Example patterns:
 * <ul>
 *   <li> Good: #</li>
 *   <li> Good: ##/#</li>
 *   <li> Good: ##/##/##</li>
 *   <li> Bad: a</li>
 *   <li> Bad: ##/</li>
 *   <li> Bad: ##//##</li>
 * </ul>
 * <p>
 * <h2>Filesystem-Safe Encoding</h2>
 * The last part of the internal URI is a "filesystem-safe" encoding of the
 * external URI. All characters will be UTF-8 percent-encoded ("URI escaped")
 * except for the following: <code>a-z A-Z 0-9 = ( ) [ ] -</code>
 * In addition, <code>.</code> (period) will be escaped as <code>%2E</code> when
 * it occurs as the last character of the URI.
 * <p>
 * <h2>Example Mappings</h2>
 * With pattern <em>#/#</em>:
 * <ul>
 *   <li> <code>urn:example1</code> becomes <code>file:0/8/urn%3Aexample1</code></li>
 *   <li> <code>http://tinyurl.com/cxzzf</code> becomes <code>file:6/2/http%3A%2F%2Ftinyurl.com%2Fcxzzf</code></li>
 * </ul>
 * With pattern <em>##/##</em>:
 * <ul>
 *   <li> <code>urn:example1</code> becomes <code>file:08/86/urn%3Aexample1</code></li>
 *   <li> <code>http://tinyurl.com/cxzzf</code> becomes <code>file:62/ca/http%3A%2F%2Ftinyurl.com%2Fcxzzf</code></li>
 * </ul>
 *
 * @author Chris Wilper
 */
public class HashPathIdMapper
        implements IdMapper {

    private static final String internalScheme = "file";

    private final String pattern;

    /**
     * Creates an instance that will use the given pattern.
     *
     * @param pathPattern the pattern to use, possibly <code>null</code> or "".
     * @throws IllegalArgumentException if the pattern is invalid.
     */
    public HashPathIdMapper(String pattern) {
        this.pattern = validatePattern(pattern);
    }

    //@Override
    public URI getExternalId(URI internalId) throws NullPointerException {
        String fullPath = internalId.toString().substring(
                internalScheme.length() + 1);
        int i = fullPath.lastIndexOf('/');
        String encodedURI;
        if (i == -1)
            encodedURI = fullPath;
        else
            encodedURI = fullPath.substring(i + 1);
        return URI.create(decode(encodedURI));
    }

    //@Override
    public URI getInternalId(URI externalId) throws NullPointerException {
        if (externalId == null) {
            throw new NullPointerException();
        }
        String uri = externalId.toString();
        return URI.create(internalScheme + ":" + getPath(uri) + encode(uri));
    }

    //@Override
    public String getInternalPrefix(String externalPrefix)
            throws NullPointerException {
        if (externalPrefix == null) {
            throw new NullPointerException();
        }
        // we can only do this if pattern is ""
        if (pattern.length() == 0) {
            return internalScheme + ":" + encode(externalPrefix);
        } else {
            return null;
        }
    }

    // gets the path based on the hash of the uri, or "" if the pattern is empty
    private String getPath(String uri) {
        if (pattern.length() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String hash = getHash(uri);
        int hashPos = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '#') {
                builder.append(hash.charAt(hashPos++));
            } else {
                builder.append(c);
            }
        }
        builder.append('/');
        return builder.toString();
    }

    // computes the md5 and returns a 32-char lowercase hex string
    private static String getHash(String uri) {
        return MD5Utility.getBase16Hash(uri);
    }

    private static String encode(String uri) {
        // encode char-by-char because we only want to borrow
        // URLEncoder.encode's behavior for some characters
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c >= 'a' && c <= 'z') {
                out.append(c);
            } else if (c >= '0' && c <= '9') {
                out.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                out.append(c);
            } else if (c == '-' || c == '=' || c == '(' || c == ')'
                    || c == '[' || c == ']' || c == ';') {
                out.append(c);
            } else if (c == ':') {
                out.append("%3A");
            } else if (c == ' ') {
                out.append("%20");
            } else if (c == '+') {
                out.append("%2B");
            } else if (c == '_') {
                out.append("%5F");
            } else if (c == '*') {
                out.append("%2A");
            } else if (c == '.') {
                if (i == uri.length() - 1) {
                    out.append("%2E");
                } else {
                    out.append(".");
                }
            } else {
                try {
                    out.append(URLEncoder.encode("" + c, "UTF-8"));
                } catch (UnsupportedEncodingException wontHappen) {
                    throw new RuntimeException(wontHappen);
                }
            }
        }
        return out.toString();
    }

    private static String decode(String encodedURI) {
        if (encodedURI.endsWith("%2E")) {
            encodedURI = encodedURI.substring(0, encodedURI.length() - 3) + ".";
        }
        try {
            return URLDecoder.decode(encodedURI, "UTF-8");
        } catch (UnsupportedEncodingException wontHappen) {
            throw new RuntimeException(wontHappen);
        }
    }

    private static String validatePattern(String pattern) {
        if (pattern == null) {
            return "";
        }
        int count = 0;
        boolean prevWasSlash = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '#') {
                count++;
                prevWasSlash = false;
            } else if (c == '/') {
                if (i == 0 || i == pattern.length() - 1) {
                    throw new IllegalArgumentException("Pattern must not begin"
                            + " or end with '/'");
                } else if (prevWasSlash) {
                    throw new IllegalArgumentException("Pattern must not"
                            + " contain consecutive '/' characters");
                } else {
                    prevWasSlash = true;
                }
            } else {
                throw new IllegalArgumentException("Illegal character in"
                        + " pattern: " + c);
            }
        }
        if (count > 32) {
            throw new IllegalArgumentException("Pattern must not contain more"
                    + " than 32 '#' characters");
        }
        return pattern;
    }

}
