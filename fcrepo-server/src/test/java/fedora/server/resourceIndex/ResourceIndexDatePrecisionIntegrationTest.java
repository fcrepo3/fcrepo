/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.TimeZone;

import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;

import org.junit.Test;

import org.trippi.TripleIterator;

import fedora.common.Constants;

import fedora.server.storage.types.DigitalObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Date precision tests.
 *
 * @author Chris Wilper
 * @author Edwin Shin
 */
public class ResourceIndexDatePrecisionIntegrationTest
        extends ResourceIndexIntegrationTest {

    private static TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final DateFormat _millisFormat;

    public ResourceIndexDatePrecisionIntegrationTest() {
        _millisFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        _millisFormat.setTimeZone(UTC);
    }

    /**
     * Dates with millisecond precision should come back as given.
     */
    @Test
    public void testMillisecondDatePrecision() throws Exception {

        String[] DT =
                {"1970-01-01T00:00:00.001Z", "1970-01-01T00:00:00.010Z",
                        "1970-01-01T00:00:00.100Z"};
        String[] DT_XSD =
                {"1970-01-01T00:00:00.001Z", "1970-01-01T00:00:00.01Z",
                        "1970-01-01T00:00:00.1Z"};
        Date[] date = {new Date(1L), new Date(10L), new Date(100L)};

        initRI(1);
        for (int i = 0; i < DT.length; i++) {
            testDates(DT[i], date[i], "test:" + i, DT_XSD[i]);
        }
    }

    /**
     * Test boundary dates.
     */
    @Test
    public void testBoundaryDates() throws Exception {
        Date EPOCH = new Date(0L);
        Date ONE_CE = new Date(-62135769600000L);
        Date ONE_BCE = new Date(-62167392000000L);
        Date TWO_BCE = new Date(-62198928000000L);

        String EPOCH_DT = "1970-01-01T00:00:00.000Z";
        String ONE_CE_DT = "0001-01-01T00:00:00.000Z";
        String ONE_BCE_DT = "0000-01-01T00:00:00.000Z";
        String TWO_BCE_DT = "-0001-01-01T00:00:00.000Z";

        String EPOCH_XSD = "1970-01-01T00:00:00Z";
        String ONE_CE_XSD = "0001-01-01T00:00:00Z";
        String ONE_BCE_XSD = "0000-01-01T00:00:00Z";
        String TWO_BCE_XSD = "-0001-01-01T00:00:00Z";

        initRI(1);
        testDates(EPOCH_DT, EPOCH, "test:epoch", EPOCH_XSD);
        testDates(ONE_CE_DT, ONE_CE, "test:one_ce", ONE_CE_XSD);
        testDates(ONE_BCE_DT, ONE_BCE, "test:one_bce", ONE_BCE_XSD);
        testDates(TWO_BCE_DT, TWO_BCE, "test:two_bce", TWO_BCE_XSD);
    }

    /**
     * Test that dateTime, date and xsdDateTime all represent the same date.
     *
     * @param dateTime
     *        dateTime for parsing by DateFormat using the pattern
     *        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".
     * @param date
     * @param pid
     *        PID of the test object
     * @param xsdDateTime
     *        canonical lexical representation of the dateTime
     * @throws Exception
     */
    private void testDates(String dateTime,
                           Date date,
                           String pid,
                           String xsdDateTime) throws Exception {
        Date createDate = _millisFormat.parse(dateTime);
        assertEquals(date, createDate);
        DigitalObject obj = getTestObject(pid, pid);
        obj.setCreateDate(createDate);
        addObj(obj, true);

        String query =
                String.format("<info:fedora/%s> <%s> *",
                              pid,
                              Constants.MODEL.CREATED_DATE.uri);
        TripleIterator results = spo(query);

        try {
            assertTrue(results.hasNext());
            ObjectNode dateNode = results.next().getObject();
            assertTrue(dateNode instanceof Literal);
            Literal dateLiteral = (Literal) dateNode;
            assertEquals(dateLiteral.getDatatypeURI().toString(),
                         Constants.RDF_XSD.DATE_TIME.uri);
            assertEquals(xsdDateTime, dateLiteral.getLexicalForm());
        } finally {
            results.close();
        }
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(ResourceIndexDatePrecisionIntegrationTest.class);
    }
}
