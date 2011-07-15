/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.fcrepo.utilities.DateUtility;
import org.junit.Test;

/**
 * @author Edwin Shin
 */
public class TestDateUtility {

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

	@Test
	public void testConvertDateToString() {
		assertEquals(EPOCH_DT, DateUtility.convertDateToString(EPOCH));
		assertEquals(EPOCH_DT, DateUtility.convertDateToString(EPOCH, true));
		assertEquals(EPOCH_DT2, DateUtility.convertDateToString(EPOCH, false));
		assertEquals(ONE_CE_DT, DateUtility.convertDateToString(ONE_CE));
		assertEquals(ONE_BCE_DT, DateUtility.convertDateToString(ONE_BCE));
		assertEquals(TWO_BCE_DT, DateUtility.convertDateToString(TWO_BCE));
	}

	@Test
	public void testConvertDateToDateString() {
		assertEquals(EPOCH_D, DateUtility.convertDateToDateString(EPOCH));
	}

	@Test
	public void testConvertDateToTimeString() {
		assertEquals(EPOCH_T, DateUtility.convertDateToTimeString(EPOCH));
	}

	@Test
	public void testConvertDateToXSDString() {
		assertEquals(EPOCH_XSD_DT, DateUtility.convertDateToXSDString(EPOCH));
		assertEquals(ONE_CE_XSD_DT, DateUtility.convertDateToXSDString(ONE_CE));
		assertEquals(ONE_BCE_XSD_DT,
				DateUtility.convertDateToXSDString(ONE_BCE));
		assertEquals(TWO_BCE_XSD_DT,
				DateUtility.convertDateToXSDString(TWO_BCE));
	}

	@Test
	public void testParseDate() {
		String[] dates = { "1970-01-01T00:00:00.000Z",
				"1970-01-01T00:00:00.00Z", "1970-01-01T00:00:00.0Z",
				"1970-01-01T00:00:00Z", "1970-01-01Z",
				"1970-01-01T00:00:00.000", "1970-01-01T00:00:00.00",
				"1970-01-01T00:00:00.0", "1970-01-01T00:00:00", "1970-01-01",
				"Thu, 01 Jan 1970 00:00:00 GMT" };
		for (String element : dates) {
			assertEquals(EPOCH, DateUtility.parseDateLoose(element));
		}

		String[] badDates = { "", "ABCD-EF-GHTIJ:KL:MN.OPQZ", "1234", "1",
				"1970-01", "1970-1-1", "12345-01-01T00:00:00.000Z",
				"12345-01-01T00:00:00." };
		for (String element : badDates) {
			assertNull(element + " not null",
					DateUtility.parseDateLoose(element));
		}

		assertEquals(ONE_CE, DateUtility.parseDateLoose(ONE_CE_XSD_DT));
		assertEquals(ONE_BCE, DateUtility.parseDateLoose(ONE_BCE_XSD_DT));
		assertEquals(TWO_BCE, DateUtility.parseDateLoose(TWO_BCE_XSD_DT));
	}

	@Test
	public void testMillis() throws Exception {
		// canonical form of 200 milliseconds after Epoch
		String a = "1970-01-01T00:00:00.2Z";
		// also 200 ms after Epoch
		String b = "1970-01-01T00:00:00.200Z";
		Date aDate = DateUtility.parseDateStrict(a);
		Date bDate = DateUtility.parseDateStrict(b);
		assertEquals(200, aDate.getTime());
		assertEquals(200, bDate.getTime());
		assertEquals(a, DateUtility.convertDateToXSDString(aDate));
		assertEquals(b, DateUtility.convertDateToString(bDate));

		// canonical form of 20 milliseconds after Epoch
		String c = "1970-01-01T00:00:00.02Z";
		// also 20 ms after Epoch
		String d = "1970-01-01T00:00:00.020Z";
		Date cDate = DateUtility.parseDateStrict(c);
		Date dDate = DateUtility.parseDateStrict(d);
		assertEquals(20, cDate.getTime());
		assertEquals(20, dDate.getTime());
		assertEquals(c, DateUtility.convertDateToXSDString(cDate));
		assertEquals(d, DateUtility.convertDateToString(dDate));

		// canonical form of 2 milliseconds after Epoch
		String e = "1970-01-01T00:00:00.002Z";
		Date eDate = DateUtility.parseDateStrict(e);
		assertEquals(2, eDate.getTime());
		assertEquals(e, DateUtility.convertDateToXSDString(eDate));
		assertEquals(e, DateUtility.convertDateToString(eDate));

		// variations of Epoch
		String f = "1970-01-01T00:00:00.0Z";
		String g = "1970-01-01T00:00:00.00Z";
		Date fDate = DateUtility.parseDateStrict(f);
		Date gDate = DateUtility.parseDateStrict(g);
		assertEquals(0, fDate.getTime());
		assertEquals(0, gDate.getTime());
		assertEquals(EPOCH_XSD_DT, DateUtility.convertDateToXSDString(fDate));
		assertEquals(EPOCH_XSD_DT, DateUtility.convertDateToXSDString(gDate));
		assertEquals(EPOCH_DT, DateUtility.convertDateToString(fDate));
		assertEquals(EPOCH_DT, DateUtility.convertDateToString(gDate));

		// negative dates
		String n = "-1234-01-01T00:00:00.2Z";
		String o = "-1234-01-01T00:00:00.200Z";
		Date nDate = DateUtility.parseDateStrict(n);
		Date oDate = DateUtility.parseDateStrict(o);
		assertEquals(nDate.getTime(), oDate.getTime());
		assertEquals(n, DateUtility.convertDateToXSDString(nDate));
	}
}
