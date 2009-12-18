/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fedora.common.xml.namespace.XMLNamespace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class NamespaceContextImplTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link fedora.utilities.NamespaceContextImpl#NamespaceContextImpl()}.
     */
    @Test
    public void testNamespaceContextImpl() {
        NamespaceContextImpl nsCtx = new NamespaceContextImpl();
        assertEquals(XMLNamespace.NULL_NS_URI, nsCtx.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        assertEquals(XMLConstants.XML_NS_URI, nsCtx.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, nsCtx.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE));
    }

    /**
     * Test method for {@link fedora.utilities.NamespaceContextImpl#NamespaceContextImpl(java.util.Map)}.
     */
    @Test
    public void testNamespaceContextImplMapOfStringString() {
        //NamespaceContextImpl nsCtx;
        Map<String, String> map = new HashMap<String, String>();
        map.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        try {
            new NamespaceContextImpl(map);
            fail("Added a mapping for " + XMLConstants.XML_NS_URI);
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().endsWith("not allowed."));
        }

    }

    /**
     * Test method for {@link fedora.utilities.NamespaceContextImpl#getNamespaceURI(java.lang.String)}.
     */
    @Test
    public void testGetNamespaceURI() {
        NamespaceContextImpl nsCtx = new NamespaceContextImpl();
        assertEquals(XMLNamespace.NULL_NS_URI, nsCtx.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        assertEquals(XMLConstants.XML_NS_URI, nsCtx.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, nsCtx.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE));
    }

    /**
     * Test method for {@link fedora.utilities.NamespaceContextImpl#getPrefix(java.lang.String)}.
     */
    @Test
    public void testGetPrefix() {
        NamespaceContextImpl nsCtx = new NamespaceContextImpl();

        assertEquals(XMLConstants.XML_NS_PREFIX, nsCtx.getPrefix(XMLConstants.XML_NS_URI));
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, nsCtx.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
    }

    /**
     * Test method for {@link fedora.utilities.NamespaceContextImpl#getPrefixes(java.lang.String)}.
     */
    @Test
    public void testGetPrefixes() {
        NamespaceContextImpl nsCtx = new NamespaceContextImpl();
        String prefix = "foo";
        String ns = "http://www.example.org/foo";
        nsCtx.addNamespace(prefix, ns);

        Iterator<String> it = nsCtx.getPrefixes(ns);
        assertNotNull(it);
        assertTrue(it.hasNext());
        assertEquals(prefix, it.next());
        try {
            it.remove();
            fail("remove() succeeded on what should be an unmodifiable Iterator.");
        } catch(UnsupportedOperationException e) {}
        assertFalse(it.hasNext());

        it = nsCtx.getPrefixes("noMapping");
        assertNotNull(it);
        assertFalse(it.hasNext());
    }

    /**
     * Test method for {@link fedora.utilities.NamespaceContextImpl#addNamespace(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testAddNamespace() {
        NamespaceContextImpl nsCtx = new NamespaceContextImpl();
        String prefix = "foo";
        String ns = "http://www.example.org/foo";
        nsCtx.addNamespace(prefix, ns);
        assertEquals(ns, nsCtx.getNamespaceURI(prefix));
        assertEquals(prefix, nsCtx.getPrefix(ns));
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(NamespaceContextImplTest.class);
    }
}
