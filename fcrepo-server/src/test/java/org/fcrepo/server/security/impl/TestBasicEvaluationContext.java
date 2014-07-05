package org.fcrepo.server.security.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fcrepo.common.Constants;
import org.fcrepo.server.Context;
import org.fcrepo.server.security.Attribute;
import org.fcrepo.server.security.PolicyFinderModule;
import org.fcrepo.server.security.RequestCtx;
import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.ParsingException;
import org.jboss.security.xacml.sunxacml.attr.AttributeDesignator;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;
import org.jboss.security.xacml.sunxacml.ctx.Subject;
import org.jboss.security.xacml.sunxacml.finder.AttributeFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TestBasicEvaluationContext {

    @Mock
    private AttributeFinder mockFinder;
    @Mock
    private Context mockContext;
    @Mock
    private Subject mockSubject;

    private BasicEvaluationCtx test;
    
    @Before
    public void setUp() throws ParsingException{
        List<Subject> subjects = Collections.singletonList(mockSubject);
        List<Attribute> action = Collections.emptyList();
        List<Attribute> environment = Collections.emptyList();
        List<Attribute> resource = Arrays.asList(new Attribute[]{
            AbstractPolicyEnforcementPoint.RESOURCE_ATTRIBUTE,
            new SingletonAttribute(Constants.OBJECT.PID.attributeId, null, null, new StringAttribute("lol:wut"))
        });
        RequestCtx request = new BasicRequestCtx(subjects, resource, action, environment);
        test = new BasicEvaluationCtx(request, mockFinder, mockContext);
    }
    
    @Test
    public void testRequestCtxAttribute() {
        String pid = PolicyFinderModule.getPid(test);
        assertEquals("lol:wut", pid);
    }

    @Test
    public void testContextAttribute() {
        URI testUri = URI.create("lol:wut");
        when(mockContext.getResourceValues(testUri)).thenReturn(new String[]{"foo:bar"});
        EvaluationResult eval = test.getResourceAttribute(URI.create(StringAttribute.identifier), testUri, null);
        // call again to test cache
        test.getResourceAttribute(URI.create(StringAttribute.identifier), testUri, null);
        verify(mockContext, times(1)).getResourceValues(testUri);
        verify(mockFinder, times(0)).findAttribute(any(URI.class), any(URI.class), any(URI.class), any(URI.class), any(EvaluationCtx.class), any(Integer.class));
        assertEquals("foo:bar", eval.getAttributeValue().getValue());
    }

    @Test
    public void testModuleAttribute() {
        URI testUri = URI.create("lol:wut");
        when(mockFinder.findAttribute(
                any(URI.class), eq(testUri), any(URI.class), any(URI.class),
                any(EvaluationCtx.class), eq(AttributeDesignator.RESOURCE_TARGET)))
            .thenReturn(new EvaluationResult(StringAttribute.getInstance("foo:bar")));
        EvaluationResult eval = test.getResourceAttribute(URI.create(StringAttribute.identifier), testUri, null);
        test.getResourceAttribute(URI.create(StringAttribute.identifier), testUri, null);
        verify(mockContext, times(2)).getResourceValues(testUri);
        verify(mockFinder, times(2)).findAttribute(any(URI.class), any(URI.class), any(URI.class), any(URI.class), any(EvaluationCtx.class), any(Integer.class));
        assertEquals("foo:bar", eval.getAttributeValue().getValue());
    }
    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestBasicEvaluationContext.class);
    }

}
