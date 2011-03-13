/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;

import org.w3c.dom.Document;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.fcrepo.test.FedoraServerTestCase;


/**
 * @author Edwin Shin
 */
public class TestAPIMLite
        extends FedoraServerTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite("APIMLite TestSuite");
        suite.addTestSuite(TestAPIMLite.class);
        return suite;
    }

    @Override
    public void setUp() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("management", "http://www.fedora.info/definitions/1/0/management/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Override
    @After
    public void tearDown() {
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }


    public void testGetNextPID() throws Exception {
        Document result;
        result = getXMLQueryResult("/management/getNextPID?xml=true");
        assertXpathEvaluatesTo("1", "count(//management:pid)", result);

        result =
                getXMLQueryResult("/management/getNextPID?numpids=10&namespace=demo&xml=true");
        assertXpathEvaluatesTo("10", "count(//management:pid)", result);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAPIMLite.class);
    }
}
