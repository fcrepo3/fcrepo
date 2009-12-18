
package melcoe.xacml.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestRelationshipResolver {

    @Test
    public void testPidNormalization() throws Exception {
        RelationshipResolverImpl rr = new RelationshipResolverImpl();
        String[] demo1_uris =
                {"demo:1", "info:fedora/demo:1", "info:fedora/demo:1/DS1",
                        "demo:1/DS1", "info:fedora/demo:1/sdef:a/sdep:b/method"};
        for (String candidate : demo1_uris) {
            assertEquals("demo:1", rr.getNormalizedPID(candidate).toString());
        }
    }
}
