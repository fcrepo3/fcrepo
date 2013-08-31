package org.fcrepo.server.storage.types;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.HashMap;

import org.junit.Test;


public class RelationshipTupleTests {

    @Test
    public void test() throws URISyntaxException {
        String aliased = "dc:predicate";
        String prefixed = "dcmi:predicate";
        String absolute = "http://dc.org#predicate";
        HashMap<String, String> aliases = new HashMap<String, String>(1);
        aliases.put("dc", "http://dc.org#");
        assertEquals(absolute, RelationshipTuple.makePredicateFromRel(aliased, aliases).toString());
        assertEquals(prefixed, RelationshipTuple.makePredicateFromRel(prefixed, aliases).toString());
        assertEquals(absolute, RelationshipTuple.makePredicateFromRel(absolute, aliases).toString());
    }
}
