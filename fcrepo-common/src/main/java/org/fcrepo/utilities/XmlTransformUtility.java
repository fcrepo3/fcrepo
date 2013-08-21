/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool.impl.StackObjectPool;
import org.fcrepo.utilities.xml.PoolableDocumentBuilderFactory;
import org.fcrepo.utilities.xml.PoolableTransformerFactoryFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class XmlTransformUtility {

    private static final Map<String, TimestampedCacheEntry<Templates>> TEMPLATES_CACHE =
            new HashMap<String, TimestampedCacheEntry<Templates>>();
    
    // A pool of namespace-aware DocumentBuilders
    // Using a Stack pool means that objectsare created on demand after the
    // pool is exhausted
    //TODO how should the default values be configured?
    private static final StackObjectPool DOCUMENT_BUILDERS =
        new StackObjectPool(new PoolableDocumentBuilderFactory(true, false), 10);
    
    private static final StackObjectPool TRANSFORM_FACTORIES =
        new StackObjectPool(new PoolableTransformerFactoryFactory(), 10);
    
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
        try {
            return (TransformerFactory) TRANSFORM_FACTORIES.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void returnTransformerFactory(TransformerFactory factory) {
        try {
            TRANSFORM_FACTORIES.returnObject(factory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Try to cache parsed Templates, but check for changes on disk
     * @param src
     * @return
     */
    public static Templates getTemplates(File src) throws TransformerException {
        String key = src.getAbsolutePath();
        TimestampedCacheEntry<Templates> entry = TEMPLATES_CACHE.get(key);
        // check to see if it is null or has changed
        if (entry == null || entry.timestamp() < src.lastModified()) {
            TransformerFactory factory = getTransformerFactory();
            try {
                Templates template = factory.newTemplates(new StreamSource(src));
                entry = new TimestampedCacheEntry<Templates>(src.lastModified(), template);
            } finally {
                returnTransformerFactory(factory);
            }
            TEMPLATES_CACHE.put(key, entry);
        }
        return entry.value();
    }
    
    public static Templates getTemplates(StreamSource source)
        throws TransformerException {
        TransformerFactory tf = getTransformerFactory();
        Templates result = null;
        try {
            result = tf.newTemplates(source);
        } finally {
            returnTransformerFactory(tf);
        }
        return result;
    }
    
    public static DocumentBuilder borrowDocumentBuilder() {
        try {
            return (DocumentBuilder) DOCUMENT_BUILDERS.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void returnDocumentBuilder(DocumentBuilder object) {
        try {
            DOCUMENT_BUILDERS.returnObject(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Document parseNamespaceAware(File src)
            throws Exception {
        return parseNamespaceAware(new FileInputStream(src));
    }
    
    public static Document parseNamespaceAware(InputStream src)
        throws Exception {
        Document result = null;
        DocumentBuilder builder =
            (DocumentBuilder) DOCUMENT_BUILDERS.borrowObject();
        try {
            result = builder.parse(src);
        } finally {
            DOCUMENT_BUILDERS.returnObject(builder);
        }
        return result;
    }
}
