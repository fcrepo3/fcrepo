
package melcoe.test;

import java.util.HashMap;
import java.util.Map;

import melcoe.xacml.util.RelationshipResolverImpl;

public class TestRelationshipResolverImpl {

    private RelationshipResolverImpl rels = null;

    /**
     * @param args
     */
    public static void main(String[] args) {
        TestRelationshipResolverImpl app = null;
        try {
            app = new TestRelationshipResolverImpl();
            app.test01();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public TestRelationshipResolverImpl()
            throws Exception {
        Map<String, String> options = new HashMap<String, String>();
        options.put("url", "http://localhost:8080/fedora/melcoerisearch");
        options.put("username", "");
        options.put("password", "");
        rels = new RelationshipResolverImpl(options);
    }

    public void test01() throws Exception {
        String result = rels.buildRESTParentHierarchy("changeme:8848");
        System.out.println(result);
    }
}
