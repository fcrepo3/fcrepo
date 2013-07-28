/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test.api;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import org.w3c.dom.Document;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.test.FedoraServerTestCase;


/**
 * @author Edwin Shin
 */
public class TestAPIMLite
        extends FedoraServerTestCase {
    
    private FedoraClient client;

    @Before
    public void setUp() throws Exception {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("management", "http://www.fedora.info/definitions/1/0/management/");
        NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
        XMLUnit.setXpathNamespaceContext(ctx);
        client = getFedoraClient();
    }

    @After
    public void tearDown() {
        client.shutdown();
        XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
    }


    @Test
    public void testGetNextPID() throws Exception {
        Document result;
        result = getXMLQueryResult(client, "/management/getNextPID?xml=true");
        assertXpathEvaluatesTo("1", "count(//management:pid)", result);

        result =
                getXMLQueryResult(client, "/management/getNextPID?numpids=10&namespace=demo&xml=true");
        assertXpathEvaluatesTo("10", "count(//management:pid)", result);
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestAPIMLite.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestAPIMLite.class);
    }
}
