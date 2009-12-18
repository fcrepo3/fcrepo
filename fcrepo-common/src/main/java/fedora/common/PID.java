/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URI;
import java.net.URISyntaxException;

import fedora.common.rdf.SimpleURIReference;

/**
 * A persistent identifier for Fedora digital objects.
 *
 * <p>The following describes the syntactic constraints for PIDs in normalized
 * form. The only differences with non-normalized PIDs are that the colon
 * delimiter may be encoded as "%3a" or "%3A", and hex-digits may use lowercase
 * [a-f].
 *
 * <pre>
 * PID:
 *   Length : maximum 64
 *   Syntax : namespace-id ":" object-id
 *
 * namespace-id:
 *   Syntax : ( [A-Z] / [a-z] / [0-9] / "-" / "." ) 1+
 *
 * object-id:
 *   Syntax : ( [A-Z] / [a-z] / [0-9] / "-" / "." / "~" / "_" / escaped-octet ) 1+
 *
 * escaped-octet:
 *   Syntax : "%" hex-digit hex-digit
 *
 * hex-digit:
 *   Syntax : [0-9] / [A-F]
 * </pre>
 *
 * @author Chris Wilper
 */
public class PID {

    /** The maximum length of a PID is 64. */
    public static final int MAX_LENGTH = 64;

    /** The reserved handle namespace id * */
    public static final String NS_HANDLE = "hdl";

    private final String m_normalized;

    private final String m_namespaceId;

    private final String m_objectId;

    private String m_filename;

    /**
     * Construct a PID from a string, throwing a MalformedPIDException if it's
     * not well-formed.
     */
    public PID(String pidString)
            throws MalformedPIDException {
        if (pidString.startsWith(Constants.FEDORA.uri)) {
            pidString = pidString.substring(Constants.FEDORA.uri.length());
        }
        m_normalized = normalize(pidString);
        String[] split = m_normalized.split(":");
        m_namespaceId = split[0];
        m_objectId = split[1];
    }

    /**
     * Alternate constructor that throws an unchecked exception if it's not
     * well-formed.
     */
    public static PID getInstance(String pidString) {
        try {
            return new PID(pidString);
        } catch (MalformedPIDException e) {
            throw new FaultException("Malformed PID: " + e.getMessage(), e);
        }
    }

    /**
     * Construct a PID given a filename of the form produced by toFilename(),
     * throwing a MalformedPIDException if it's not well-formed.
     */
    public static PID fromFilename(String filenameString)
            throws MalformedPIDException {
        String decoded = filenameString.replaceFirst("_", ":");
        if (decoded.endsWith("%")) {
            decoded = decoded.substring(0, decoded.length() - 1) + ".";
        }
        return new PID(decoded);
    }

    /**
     * Return the normalized form of the given pid string, or throw a
     * MalformedPIDException.
     */
    public static String normalize(String pidString)
            throws MalformedPIDException {
        if (pidString == null) {
            throw new MalformedPIDException("PID is null.");
        }

        // Then normalize while checking syntax
        StringBuffer out = new StringBuffer();
        boolean inObjectID = false;
        for (int i = 0; i < pidString.length(); i++) {
            char c = pidString.charAt(i);
            if (!inObjectID) {
                if (c == ':') {
                    out.append(':');
                    inObjectID = true;
                } else if (c == '%') {
                    // next 2 chars MUST be 3[aA]
                    if (pidString.length() >= i + 3) {
                        i++;
                        if (pidString.charAt(i) == '3') {
                            i++;
                            c = pidString.charAt(i);
                            if (c == 'a' || c == 'A') {
                                out.append(":");
                                inObjectID = true;
                            } else {
                                throw new MalformedPIDException("Error in PID after first '%': expected '3a' or '3A', but saw '3"
                                        + c + "'.");
                            }
                        } else {
                            throw new MalformedPIDException("Error in PID after first '%': expected '3a' or '3A', but saw '"
                                    + pidString.substring(i, i + 2) + "'.");
                        }
                    } else {
                        throw new MalformedPIDException("Error in PID after first '%': expected '3a' or '3A', but saw '"
                                + pidString.substring(i + 1) + "'.");
                    }
                } else if (isAlphaNum(c) || c == '-' || c == '.') {
                    out.append(c);
                } else {
                    // invalid char for namespace-id
                    throw new MalformedPIDException("PID namespace-id cannot contain '"
                            + c + "' character.");
                }
            } else if (isAlphaNum(c) || c == '-' || c == '.' || c == '~'
                    || c == '_') {
                out.append(c);
            } else if (c == '%') {
                // next 2 chars MUST be [0-9][a-f][A-F]
                if (pidString.length() >= i + 3) {
                    char h1 = getNormalizedHexChar(pidString.charAt(++i));
                    char h2 = getNormalizedHexChar(pidString.charAt(++i));
                    out.append("%" + h1 + h2);
                } else {
                    throw new MalformedPIDException("PID object-id ended early: need at least 2 chars after '%'.");
                }
            } else {
                throw new MalformedPIDException("PID object-id cannot contain '"
                        + c + "' character.");
            }
        }

        if (!inObjectID) {
            throw new MalformedPIDException("PID delimiter (:) is missing.");
        }
        String outString = out.toString();
        if (outString.startsWith(":")) {
            throw new MalformedPIDException("PID namespace-id cannot be empty.");
        }
        if (outString.length() < 3) {
            throw new MalformedPIDException("PID object-id cannot be empty.");
        }
        if (outString.length() > MAX_LENGTH) {
            throw new MalformedPIDException("PID length exceeds " + MAX_LENGTH
                    + ".");
        }

        // If we got here, it's well-formed, so return it.
        return outString;
    }

    private static boolean isAlphaNum(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A'
                && c <= 'Z';
    }

    private static char getNormalizedHexChar(char c)
            throws MalformedPIDException {
        if (c >= '0' && c <= '9') {
            return c;
        }
        c = ("" + c).toUpperCase().charAt(0);
        if (c >= 'A' && c <= 'F') {
            return c;
        }
        throw new MalformedPIDException("Bad hex-digit in PID object-id: " + c);
    }

    /**
     * Return the normalized form of this PID.
     */
    @Override
    public String toString() {
        return m_normalized;
    }

    /**
     * Return the URI form of this PID. This is just the PID, prepended with
     * "info:fedora/".
     */
    public String toURI() {
        return Constants.FEDORA.uri + m_normalized;
    }

    /**
     * Return the URI form of some PID string, assuming it is well-formed.
     */
    public static String toURI(String pidString) {
        return Constants.FEDORA.uri + pidString;
    }

    /**
     * Return a URIReference of some PID string, assuming it is well-formed.
     */
    public static SimpleURIReference toURIReference(String pidString) {
        SimpleURIReference ref = null;
        try {
            ref = new SimpleURIReference(new URI(toURI(pidString)));
        } catch (URISyntaxException e) {
            // assumes pid is well-formed
            throw new Error(e);
        }
        return ref;
    }

    /**
     * Return a string representing this PID that can be safely used as a
     * filename on any OS.
     * <ul>
     * <li> The colon (:) is replaced with an underscore (_).</li>
     * <li> Trailing dots are encoded as percents (%).</li>
     * </ul>
     */
    public String toFilename() {
        if (m_filename == null) { // lazily convert, since not always needed
            m_filename = m_normalized.replaceAll(":", "_");
            if (m_filename.endsWith(".")) {
                m_filename =
                        m_filename.substring(0, m_filename.length() - 1) + "%";
            }
        }
        return m_filename;
    }

    public String getNamespaceId() {
        return m_namespaceId;
    }

    public String getObjectId() {
        return m_objectId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof PID && m_normalized.equals(((PID) o).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_normalized.hashCode();
    }

    /**
     * Command-line interactive tester. If one arg given, prints normalized form
     * of that PID and exits. If no args, enters interactive mode.
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            PID p = new PID(args[0]);
            System.out.println("Normalized    : " + p.toString());
            System.out.println("To filename   : " + p.toFilename());
            System.out.println("From filename : "
                    + PID.fromFilename(p.toFilename()).toString());
        } else {
            System.out.println("--------------------------------------");
            System.out.println("PID Syntax Checker - Interactive mode");
            System.out.println("--------------------------------------");
            boolean done = false;
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(System.in));
            while (!done) {
                try {
                    System.out.print("Enter a PID (ENTER to exit): ");
                    String line = reader.readLine();
                    if (line.equals("")) {
                        done = true;
                    } else {
                        PID p = new PID(line);
                        System.out.println("Normalized    : " + p.toString());
                        System.out.println("To filename   : " + p.toFilename());
                        System.out.println("From filename : "
                                + PID.fromFilename(p.toFilename()).toString());
                    }
                } catch (MalformedPIDException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
        }
    }

}
