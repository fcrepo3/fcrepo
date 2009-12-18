/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import java.util.Date;

import junit.framework.TestCase;

/**
 * @author Edwin Shin
 */
public class TestDateUtility
        extends TestCase {

    protected final Date EPOCH = new Date(0L);

    protected final String EPOCH_DT = "1970-01-01T00:00:00.000Z";

    protected final String EPOCH_DT2 = "1970-01-01T00:00:00Z"; // no millis

    protected final String EPOCH_XSD_DT = "1970-01-01T00:00:00Z";

    protected final String EPOCH_D = "1970-01-01Z";

    protected final String EPOCH_T = "00:00:00.000Z";

    protected final String HTTP_DATE = "Thu, 04 Aug 2005 01:35:07 GMT";

    protected final Date ONE_CE = new Date(-62135769600000L);

    protected final String ONE_CE_DT = "0001-01-01T00:00:00.000Z";

    protected final String ONE_CE_XSD_DT = "0001-01-01T00:00:00Z";

    protected final Date ONE_BCE = new Date(-62167392000000L);

    protected final String ONE_BCE_DT = "-0001-01-01T00:00:00.000Z";

    protected final String ONE_BCE_XSD_DT = "0000-01-01T00:00:00Z";

    protected final Date TWO_BCE = new Date(-62198928000000L);

    protected final String TWO_BCE_DT = "-0002-01-01T00:00:00.000Z";

    protected final String TWO_BCE_XSD_DT = "-0001-01-01T00:00:00Z";

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestDateUtility.class);
    }

    public void testConvertDateToString() {
        assertEquals(EPOCH_DT, DateUtility.convertDateToString(EPOCH));
        assertEquals(EPOCH_DT, DateUtility.convertDateToString(EPOCH, true));
        assertEquals(EPOCH_DT2, DateUtility.convertDateToString(EPOCH, false));
        assertEquals(ONE_CE_DT, DateUtility.convertDateToString(ONE_CE));
        assertEquals(ONE_BCE_DT, DateUtility.convertDateToString(ONE_BCE));
        assertEquals(TWO_BCE_DT, DateUtility.convertDateToString(TWO_BCE));
    }

    public void testConvertDateToDateString() {
        assertEquals(EPOCH_D, DateUtility.convertDateToDateString(EPOCH));
    }

    public void testConvertDateToTimeString() {
        assertEquals(EPOCH_T, DateUtility.convertDateToTimeString(EPOCH));
    }

    public void testConvertDateToXSDString() {
        assertEquals(EPOCH_XSD_DT, DateUtility.convertDateToXSDString(EPOCH));
        assertEquals(ONE_CE_XSD_DT, DateUtility.convertDateToXSDString(ONE_CE));
        assertEquals(ONE_BCE_XSD_DT, DateUtility
                .convertDateToXSDString(ONE_BCE));
        assertEquals(TWO_BCE_XSD_DT, DateUtility
                .convertDateToXSDString(TWO_BCE));
    }

    public void testParseDate() {
        String[] dates =
                {"1970-01-01T00:00:00.000Z", "1970-01-01T00:00:00.00Z",
                        "1970-01-01T00:00:00.0Z", "1970-01-01T00:00:00Z",
                        "1970-01-01Z", "1970-01-01T00:00:00.000",
                        "1970-01-01T00:00:00.00", "1970-01-01T00:00:00.0",
                        "1970-01-01T00:00:00", "1970-01-01",
                        "Thu, 01 Jan 1970 00:00:00 GMT"};
        for (String element : dates) {
            assertEquals(EPOCH, DateUtility.parseDateAsUTC(element));
        }

        String[] badDates =
                {"", "ABCD-EF-GHTIJ:KL:MN.OPQZ", "1234", "1", "1970-01",
                        "1970-1-1", "12345-01-01T00:00:00.000Z",
                        "12345-01-01T00:00:00."};
        for (String element : badDates) {
            assertNull(DateUtility.parseDateAsUTC(element));
        }

        assertEquals(ONE_CE, DateUtility.parseDateAsUTC(ONE_CE_XSD_DT));
        assertEquals(ONE_BCE, DateUtility.parseDateAsUTC(ONE_BCE_XSD_DT));
        assertEquals(TWO_BCE, DateUtility.parseDateAsUTC(TWO_BCE_XSD_DT));
    }

}
