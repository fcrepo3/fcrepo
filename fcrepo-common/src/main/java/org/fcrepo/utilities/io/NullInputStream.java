/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.io;

import java.io.IOException;
import java.io.InputStream;

    /**
     * CXF needs a non-null entity to preserve content-type and content-length
     * as per 2.7.7
     * @author armintor@gmail.com
     *
     */
public class NullInputStream extends InputStream {

    public static final NullInputStream NULL_STREAM =
            new NullInputStream();
    
    @Override
    public int read() throws IOException {
        return -1;
    }
    
    @Override
    public long skip(long n) {
        return n;
    }
}