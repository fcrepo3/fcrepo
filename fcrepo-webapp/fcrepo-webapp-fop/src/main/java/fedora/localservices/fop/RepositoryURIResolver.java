/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

package fedora.localservices.fop;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * This class is a URIResolver implementation that provides access to resources
 * in the repository (or any external Web resource). Note: This should be
 * revised to handle back-end security but the older code did not do so this
 * code is a placeholder.
 */
public class RepositoryURIResolver
        implements URIResolver {

    /** {@inheritDoc} */
    public Source resolve(String href, String base) throws TransformerException {

        return resolveRepositoryURI(href);

    }

    /**
     * Resolves the "repository" URI.
     * 
     * @param path
     *        the href to the resource
     * @return the resolved Source or null if the resource was not found
     * @throws TransformerException
     *         if no URL can be constructed from the path
     */
    protected Source resolveRepositoryURI(String path)
            throws TransformerException {

        Source resolvedSource = null;

        try {
            if (path != null) {
                URL url = new URL(path);
                InputStream in = url.openStream();
                if (in != null) {
                    resolvedSource = new StreamSource(in);
                }
            } else {
                throw new TransformerException("Resource does not exist. \""
                        + path + "\" is not accessible.");
            }
        } catch (MalformedURLException mfue) {
            throw new TransformerException("Error accessing resource using servlet context: "
                                         + path,
                                           mfue);
        } catch (IOException ioe) {
            throw new TransformerException("Unable to access resource at: "
                                         + path,
                                         ioe);
        }
        return resolvedSource;
    }
}
