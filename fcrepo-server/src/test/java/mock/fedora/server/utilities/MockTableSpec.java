/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package mock.fedora.server.utilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides TableSpecs for unit/integration testing.
 *
 * @author Andrew Woods
 */
public class MockTableSpec {

    public static InputStream getTableSpecStream() throws IOException {
        String dbSpec =
                "fedora/server/storage/resources/DefaultDOManager.dbspec";

        InputStream specIn =
                MockTableSpec.class.getClassLoader()
                        .getResourceAsStream(dbSpec);
        if (specIn == null) {
            throw new IOException("Cannot find required resource: " + dbSpec);
        }
        return specIn;
    }

}
