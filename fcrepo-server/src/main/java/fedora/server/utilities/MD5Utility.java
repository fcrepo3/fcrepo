/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import com.twmacinta.util.MD5;

/**
 * Static methods for creating evenly-distributed hashes.
 * 
 * @author Chris Wilper
 */
public abstract class MD5Utility {

    static {
        MD5.initNativeLibrary(true); // don't attempt to use the native libs, ever.
    }

    /**
     * Get hash of the given String in hex.
     */
    public static String getBase16Hash(String in) {
        return MD5.asHex(new MD5(in).Final());
    }

}
