/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import javax.xml.transform.TransformerFactory;

import net.sf.saxon.FeatureKeys;

/**
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class XmlTransformUtility {

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
            factory.setAttribute(FeatureKeys.VERSION_WARNING, Boolean.FALSE);
        }
        return factory;
    }
}
