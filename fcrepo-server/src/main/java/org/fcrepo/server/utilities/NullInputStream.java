package org.fcrepo.server.utilities;

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
    
}