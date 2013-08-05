/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;


/**
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class XmlTransformUtility {

    private static final Map<String, Long> XSL_MODIFIED =
            new HashMap<String, Long>();
    
    private static final Map<String, Templates> TEMPLATES_CACHE =
            new HashMap<String, Templates>();
    /**
     * Convenience method to get a new instance of a TransformerFactory.
     * If the {@link #TransformerFactory} is an instance of
     * net.sf.saxon.TransformerFactoryImpl, the attribute
     * {@link #FeatureKeys.VERSION_WARNING} will be set to false in order to
     * suppress the warning about using an XSLT1 stylesheet with an XSLT2
     * processor.
     *
     * @return a new instance of TransformerFactory
     */
    public static TransformerFactory getTransformerFactory() {
        TransformerFactory factory = TransformerFactory.newInstance();
        if (factory.getClass().getName().equals("net.sf.saxon.TransformerFactoryImpl")) {
            factory.setAttribute("http://saxon.sf.net/feature/version-warning", Boolean.FALSE);
        }
        return factory;
    }
    
    /**
     * Try to cache parsed Templates, but check for changes on disk
     * @param src
     * @return
     */
    public static Templates getTemplates(File src) throws TransformerException {
        String key = src.getAbsolutePath();
        if (XSL_MODIFIED.containsKey(key)) {
            // check to see if it has changed
            if (src.lastModified() <= XSL_MODIFIED.get(key).longValue()) {
                return TEMPLATES_CACHE.get(key);
            }
        }
        Templates template = getTransformerFactory().newTemplates(new StreamSource(src));
        TEMPLATES_CACHE.put(key, template);
        XSL_MODIFIED.put(key, src.lastModified());
        return template;
    }
}
