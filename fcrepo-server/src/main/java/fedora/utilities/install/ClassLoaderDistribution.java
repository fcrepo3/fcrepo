/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

public class ClassLoaderDistribution
        extends Distribution {

    private final ClassLoader _cl;

    public ClassLoaderDistribution() {
        _cl = this.getClass().getClassLoader();
    }

    public ClassLoaderDistribution(ClassLoader cl) {
        _cl = cl;
    }

    @Override
    public boolean contains(String path) {
        return _cl.getResource(rewritePath(path)) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * Note: requested resources will automatically be prefixed with "resources/".
     */
    @Override
    public InputStream get(String path) throws IOException {
        InputStream stream = _cl.getResourceAsStream(rewritePath(path));
        if (stream == null) {
            throw new FileNotFoundException("Not found in classpath: " + path);
        } else {
            return stream;
        }
    }

    @Override
    public URL getURL(String path) {
        return _cl.getResource(rewritePath(path));
    }

    /**
     * Rewrites the requested path to remove leading slashes and prefix with 
     * "resources/"
     * 
     * Note: we don't check for backtracking.
     * 
     * @param path the requested path (e.g. "/foo/bar")
     * @return the rewritten path (e.g. "resources/foo/bar")
     */
    private static String rewritePath(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        // Note, ClassLoader paths are always absolute, so , so no leading slash
        return "resources/" + path;
    }
}
