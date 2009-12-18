/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.lowlevel;

import java.util.Map;

import fedora.server.Server;
import fedora.server.errors.LowlevelStorageException;
import fedora.server.errors.MalformedPidException;

/**
 * @author Bill Niebel
 */
public abstract class PathAlgorithm {

    public PathAlgorithm(Map<String, ?> configuration) {
    };

    public abstract String get(String pid) throws LowlevelStorageException;

    public static String encode(String unencoded)
            throws LowlevelStorageException {
        try {
            int i = unencoded.indexOf("+");
            if (i != -1) {
                return Server.getPID(unencoded.substring(0, i)).toFilename()
                        + unencoded.substring(i);
            } else {
                return Server.getPID(unencoded).toFilename();
            }
        } catch (MalformedPidException e) {
            throw new LowlevelStorageException(true, e.getMessage(), e);
        }
    }

    public static String decode(String encoded) throws LowlevelStorageException {
        try {
            int i = encoded.indexOf("+");
            if (i != -1) {
                return Server.pidFromFilename(encoded.substring(0, i))
                        .toString()
                        + encoded.substring(i);
            } else {
                return Server.pidFromFilename(encoded).toString();
            }
        } catch (MalformedPidException e) {
            throw new LowlevelStorageException(true, e.getMessage(), e);
        }
    }
}
