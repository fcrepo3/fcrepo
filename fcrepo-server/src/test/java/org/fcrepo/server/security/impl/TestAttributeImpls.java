package org.fcrepo.server.security.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.fcrepo.server.Context;
import org.fcrepo.server.security.Attribute;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.ctx.Subject;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TestAttributeImpls {

    @Mock
    private AttributeFinder mockFinder;
    @Mock
    private Context mockContext;
    @Mock
    private Subject mockSubject;

    
    @Before
    public void setUp() throws ParsingException{
    }
    
    @Test
    public void testBasicRoundtrip() throws ParsingException {
        URI id = URI.create("lol:wut");
        String issuer = null;
        BasicAttribute  expected = new BasicAttribute(id, issuer, null, StringAttribute.getInstance("foo"));
        String encoded = expected.encode();
        ByteArrayInputStream input = new ByteArrayInputStream(encoded.getBytes());
        Attribute actual = BasicAttribute.getInstance(InputParser.parseInput(input, "Attribute"));
        assertEquals(expected, actual);
    }

    @Test
    public void testSingletonRoundtrip() throws ParsingException {
        URI id = URI.create("lol:wut");
        String issuer = null;
        Attribute  expected = new SingletonAttribute(id, issuer, null, StringAttribute.getInstance("foo"));
        String encoded = expected.encode();
        ByteArrayInputStream input = new ByteArrayInputStream(encoded.getBytes());
        Attribute actual = BasicAttribute.getInstance(InputParser.parseInput(input, "Attribute"));
        assertEquals(expected, actual);
    }
    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestAttributeImpls.class);
    }

}
