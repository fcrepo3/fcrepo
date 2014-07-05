package org.fcrepo.server.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.security.Attribute;
import org.fcrepo.server.security.RequestCtx;
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
public class TestBasicRequestCtx {

    @Mock
    private AttributeFinder mockFinder;
    @Mock
    private Context mockContext;

    private Subject fixtureSubject;

    private BasicRequestCtx test;
    
    @Before
    public void setUp() throws ParsingException{
        fixtureSubject = new Subject(Collections.emptyList());
        List<Subject> subjects = Collections.singletonList(fixtureSubject);
        List<Attribute> action = Collections.emptyList();
        List<Attribute> environment = Collections.emptyList();
        List<Attribute> resource = Arrays.asList(new Attribute[]{
            AbstractPolicyEnforcementPoint.RESOURCE_ATTRIBUTE,
            new SingletonAttribute(Constants.OBJECT.PID.attributeId, null, null, new StringAttribute("lol:wut"))
        });
        test = new BasicRequestCtx(subjects, resource, action, environment);
    }
    
    @Test
    public void testRoundtrip() throws ParsingException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        test.encode(buf);
        ByteArrayInputStream encoded = new ByteArrayInputStream(buf.toByteArray());
        RequestCtx actual = BasicRequestCtx.getInstance(encoded);
        assertEquals(toString(test), toString(actual));
        assertTrue(test.equals(actual));
    }
    
    @Test
    public void testEquals() throws ParsingException {
        List<Subject> subjects = Collections.singletonList(fixtureSubject);
        List<Attribute> action = Collections.emptyList();
        List<Attribute> environment = Collections.emptyList();
        List<Attribute> resource = Arrays.asList(new Attribute[]{
            AbstractPolicyEnforcementPoint.RESOURCE_ATTRIBUTE,
            new SingletonAttribute(Constants.OBJECT.PID.attributeId, null, null, new StringAttribute("foo:bar"))
        });
        RequestCtx actual = new BasicRequestCtx(subjects, resource, action, environment);
        assertFalse(test.equals(actual));
        resource = Arrays.asList(new Attribute[]{
            AbstractPolicyEnforcementPoint.RESOURCE_ATTRIBUTE,
            new SingletonAttribute(Constants.OBJECT.PID.attributeId, null, null, new StringAttribute("lol:wut"))
        });
        actual = new BasicRequestCtx(subjects, resource, action, environment);
        assertTrue(test.equals(actual));        
    }
    
    private static String toString(RequestCtx req) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        req.encode(buf);
        return new String(buf.toByteArray());
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestBasicRequestCtx.class);
    }

}
