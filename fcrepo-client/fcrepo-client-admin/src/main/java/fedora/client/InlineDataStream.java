/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client;

import java.io.File;

/**
 * @author Chris Wilper
 */
public class InlineDataStream
        extends DataStream {

    public InlineDataStream(File tempDir, String id) {
        super(tempDir, id);
    }

    @Override
    public final int getType() {
        return DataStream.INLINE;
    }

}
