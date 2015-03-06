/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.mock.server.utilities;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides TableSpecs for unit/integration testing.
 *
 * @author Andrew Woods
 */
public class MockTableSpec {

    public static InputStream getTableSpecStream(String dbSpec) throws IOException {
        InputStream specIn =
                MockTableSpec.class.getClassLoader()
                        .getResourceAsStream(dbSpec);
        if (specIn == null) {
            throw new IOException("Cannot find required resource: " + dbSpec);
        }
        return specIn;
    }

}
