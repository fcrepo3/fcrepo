/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Edwin Shin
 * @version $Id$
 */
public class ProxyFactoryTest {

    private TargetInterface target;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        target = new TargetImpl();
    }

    /**
     * Test method for
     * {@link fedora.server.proxy.ProxyFactory#getProxy(java.lang.Object, java.lang.Object[])}.
     */
    @Test
    public void testGetProxy() throws Exception {
        Object[] chainA =
                {new FlipBooleanHandler(), new ReverseStringHandler()};
        Object[] chainB = {new ReverseStringHandler(), new PrefixStringWithXHandler()};
        Object[] chainC = {new PrefixStringWithXHandler(), new ReverseStringHandler()};
        
        TargetInterface proxy =
                (TargetInterface) ProxyFactory.getProxy(target,
                                                        chainA);
        assertTrue(proxy.bar(false));
        assertEquals("olleh", proxy.baz("hello"));
        
        proxy =
            (TargetInterface) ProxyFactory.getProxy(target,
                                                    chainB);
        assertEquals("ollehX", proxy.baz("hello"));
        
        proxy =
            (TargetInterface) ProxyFactory.getProxy(target,
                                                    chainC);
        assertEquals("Xolleh", proxy.baz("hello"));
        }

    interface TargetInterface {

        public void foo();

        public boolean bar(boolean b);

        public String baz(String c);

        public List<String> quux(List<String> d);
    }

    class TargetImpl
            implements TargetInterface {

        public void foo() {
        }

        public boolean bar(boolean x) {
            return x;
        }

        public String baz(String y) {
            return y;
        }

        public List<String> quux(List<String> z) {
            return z;
        }
    }

    class FlipBooleanHandler
            extends AbstractInvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Boolean) {
                    Boolean b = (Boolean) args[i];
                    args[i] = !b.booleanValue();
                }
            }
            Object returnValue = method.invoke(target, args);
            return returnValue;
        }
    }

    class ReverseStringHandler
            extends AbstractInvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof String) {
                    if (args[i] != null && !args[i].equals("")) {
                        StringBuilder sb = new StringBuilder((String) args[i]);
                        args[i] = sb.reverse().toString();
                    }
                }
            }

            Object returnValue = method.invoke(target, args);
            return returnValue;
        }
    }

    class PrefixStringWithXHandler
            extends AbstractInvocationHandler {

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof String) {
                    if (args[i] == null) {
                        args[i] = "X";
                    } else {
                        args[i] = "X" + args[i];
                    }
                }
            }

            Object returnValue = method.invoke(target, args);
            return returnValue;
        }
    }
}
