package org.fcrepo.server.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class PIDStreamIterableWrapperTest {

    // list-style input tests
    // (pids on separate lines)

    @Test
    public void testEmpty() throws Exception {
        // no data
        testIterable(new String[0], getIterable(""));
        testIterable(new String[0], getIterable("   "));

        // blank lines
        testIterable(new String[0], getIterable("\n"));
        testIterable(new String[0], getIterable("   \n"));
        testIterable(new String[0], getIterable("   \n"   ));
        testIterable(new String[0], getIterable("   \n   \n"));
        testIterable(new String[0], getIterable("\n\n\n\n"));

    }

    @Test
    public void testSingleItem() throws Exception {
        // single item
        testIterable(new String[] {"item"}, getIterable("item"));
        testIterable(new String[] {"item"}, getIterable("   item"));
        testIterable(new String[] {"item"}, getIterable("   item   "));

        // with blank lines
        testIterable(new String[] {"item"}, getIterable("\n   item   "));
        testIterable(new String[] {"item"}, getIterable("\n   item   \n"));
        testIterable(new String[] {"item"}, getIterable("item\n"));
        testIterable(new String[] {"item"}, getIterable("\nitem"));
        testIterable(new String[] {"item"}, getIterable("\nitem\n"));
    }

    // xml input tests
    // (element: pid, element and contents on single line, no sub-elements, attributes, etc)

    @Test
    public void testXML() throws IOException {
        // no element
        testIterable(new String[0], getIterable("<dummy>\n</dummy>"));
        testIterable(new String[0], getIterable("\n<dummy>\n</dummy>"));
        testIterable(new String[0], getIterable("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<dummy>\n</dummy>"));
        testIterable(new String[0], getIterable("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<dummy>\n</dummy>"));

        // single element
        testIterable(new String[] {"test"}, getIterable("<dummy>\n<pid>test</pid>\n</dummy>"));
        testIterable(new String[] {"test"}, getIterable("\n<dummy>\n\n<pid>test</pid>\n</dummy>"));
        testIterable(new String[] {"test"}, getIterable("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<dummy>\n<pid>test</pid>\n</dummy>"));

        // several elements, newlines, spaces before and after
        String test3 = "\n<dummy>\n\n<pid>test1</pid>\n\n     <pid>test2</pid>\n<pid>test3</pid>    \n</dummy>";
        String[] expected = {"test1", "test2", "test3"};
        testIterable(expected, getIterable(test3));
        test3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + test3;
        testIterable(expected, getIterable(test3));
    }

    private static Iterable<String> getIterable(String string) throws IOException {
        return getIterable(string.getBytes("UTF-8"));
    }
    private static Iterable<String> getIterable(byte[] bytes) throws IOException {
        return new PIDStreamIterableWrapper(new ByteArrayInputStream(bytes));
    }

    private static void testIterable(String[] expected, Iterable<String> it) {

        // iterate, collect the results
        ArrayList<String> res = new ArrayList<String>();
        for (String element : it) {
            res.add(element);
        }
        // check number of elements found
        assertEquals("Iterator count matches expected", expected.length, res.size());

        // check each element
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Element matches", expected[i], res.get(i));
        }
    }

}
