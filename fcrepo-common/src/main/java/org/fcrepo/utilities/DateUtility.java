/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A collection of utility methods for performing frequently required tasks.
 * 
 * @author Ross Wayland
 * @author Frank Asseg
 */
public abstract class DateUtility {

//	private static final Date ONE_BCE = new Date(-62198755200000L);

	private static final Date ONE_CE = new Date(-62135596800000L);

	private static final DateTimeFormatter FORMATTER_MILLISECONDS_T_Z = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private static final DateTimeFormatter FORMATTER_MILLISECONDS_T = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private static final DateTimeFormatter FORMATTER_SECONDS_T_Z = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static final DateTimeFormatter FORMATTER_SECONDS_T = DateTimeFormat
			.forPattern("yyyy-MM-dd'T'HH:mm:ss");
	private static final DateTimeFormatter FORMATTER_SECONDS_Z = DateTimeFormat
			.forPattern("HH:mm:ss.SSS'Z'");
	private static final DateTimeFormatter FORMATTER_DATE_Z = DateTimeFormat
			.forPattern("yyyy-MM-dd'Z'");
	private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormat
			.forPattern("yyyy-MM-dd");
	private static final DateTimeFormatter FORMATTER_TIMEZONE = DateTimeFormat
			.forPattern("EEE, dd MMMM yyyyy HH:mm:ss");

	static {
		DateTimeZone.setDefault(DateTimeZone.UTC);
	}

	/**
	 * Converts a datetime string into and instance of java.util.Date using the
	 * date format: yyyy-MM-ddTHH:mm:ss.SSSZ.
	 * 
	 * @param dateTime
	 *            A datetime string
	 * @return Corresponding instance of java.util.Date (returns null if
	 *         dateTime string argument is empty string or null)
	 */
	public static Date convertStringToDate(String dateTime) {
		return parseDateLoose(dateTime);
	}

	/**
	 * Converts an instance of java.util.Date into a String using the date
	 * format: yyyy-MM-ddTHH:mm:ss.SSSZ.
	 * 
	 * @param date
	 *            Instance of java.util.Date.
	 * @return ISO 8601 String representation (yyyy-MM-ddTHH:mm:ss.SSSZ) of the
	 *         Date argument or null if the Date argument is null.
	 */
	public static String convertDateToString(Date date) {
		return convertDateToString(date, true);
	}

	/**
	 * Converts an instance of java.util.Date into an ISO 8601 String
	 * representation. Uses the date format yyyy-MM-ddTHH:mm:ss.SSSZ or
	 * yyyy-MM-ddTHH:mm:ssZ, depending on whether millisecond precision is
	 * desired.
	 * 
	 * @param date
	 *            Instance of java.util.Date.
	 * @param millis
	 *            Whether or not the return value should include milliseconds.
	 * @return ISO 8601 String representation of the Date argument or null if
	 *         the Date argument is null.
	 */
	public static String convertDateToString(Date date, boolean millis) {
		if (date == null) {
			return null;
		} else {
			DateTimeFormatter df;
			if (millis) {
				// df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				df = FORMATTER_MILLISECONDS_T_Z;
			} else {
				// df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				df = FORMATTER_SECONDS_T_Z;
			}

			return df.print(date.getTime());
		}
	}

	/**
	 * Converts an instance of <code>Date</code> into the canonical lexical
	 * representation of an XSD dateTime with the following exceptions: - Dates
	 * before 1 CE (i.e. 1 AD) are handled according to ISO 8601:2000 Second
	 * Edition: "0000" is the lexical representation of 1 BCE "-0001" is the
	 * lexical representation of 2 BCE
	 * 
	 * @param date
	 *            Instance of java.util.Date.
	 * @return the lexical form of the XSD dateTime value, e.g.
	 *         "2006-11-13T09:40:55.001Z".
	 * @see <a
	 *      href="http://www.w3.org/TR/xmlschema-2/#date-canonical-representation">3.2.7.2
	 *      Canonical representation</a>
	 */
	public static String convertDateToXSDString(Date date) {
		if (date == null) {
			return null;
		}
		String dateTime = convertDateToString(date, true);
		if (date.before(ONE_CE)) {
			// fix the format for lexical representation of the year
			// e.g. 1 BCE: 0000-01.01 (1BCE is year 0)
			int pos = dateTime.indexOf('-', 1);
			int year = Integer.parseInt(dateTime.substring(0, pos));
			if (year == -1) {
				dateTime = "0000" + dateTime.substring(pos);
			} else if (year < 0) {
				year += 1;
				String prefix = "";
				if (year > -10) {
					prefix = "000";
				} else if (year > -100) {
					prefix = "00";
				} else if (year > -1000) {
					prefix = "0";
				}
				dateTime = "-" + prefix + Math.abs(year)
						+ dateTime.substring(pos);
			}
		}
		// fix the format for the lexical representation of the milliseconds,
		// no leading 0s are allowed, and if it's all zeros it has to be
		// removed.
		int posDot = dateTime.indexOf('.');
		int posZ = dateTime.indexOf('Z');
		int millis = Integer.parseInt(dateTime.substring(posDot + 1, posZ));
		String milliString;
		if (millis == 0) {
			milliString = "";
		} else if (millis < 10) {
			milliString = ".00" + millis;
		} else if (millis < 100) {
			milliString = ".0" + millis;
		} else {
			milliString = "." + millis;
		}
		while (milliString.length() > 0
				&& milliString.charAt(milliString.length() - 1) == '0') {
			milliString = milliString.substring(0, milliString.length() - 1);
		}
		dateTime = dateTime.substring(0, posDot) + milliString + "Z";
		return dateTime;
	}

	/**
	 * Converts an instance of java.util.Date into a String using the date
	 * format: yyyy-MM-ddZ.
	 * 
	 * @param date
	 *            Instance of java.util.Date.
	 * @return Corresponding date string (returns null if Date argument is
	 *         null).
	 */
	public static String convertDateToDateString(Date date) {
		if (date == null) {
			return null;
		} else {
			// DateFormat df = new SimpleDateFormat("yyyy-MM-dd'Z'");
			DateTimeFormatter df = FORMATTER_DATE_Z;
			return df.print(date.getTime());
		}
	}

	/**
	 * Converts an instance of java.util.Date into a String using the date
	 * format: HH:mm:ss.SSSZ.
	 * 
	 * @param date
	 *            Instance of java.util.Date.
	 * @return Corresponding time string (returns null if Date argument is
	 *         null).
	 */
	public static String convertDateToTimeString(Date date) {
		if (date == null) {
			return null;
		} else {
			// DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS'Z'");
			DateTimeFormatter df = FORMATTER_SECONDS_Z;
			return df.print(date.getTime());
		}
	}

	/**
	 * Convenience method for {@link #parseDateStrict(String)} which does not
	 * throw an exception on error, but merely returns null.
	 * 
	 * @param dateString
	 *            the date string to parse
	 * @return Date the date, if parse was successful; null otherwise
	 */
	public static Date parseDateLoose(String dateString) {
		try {
			return parseDateStrict(dateString);
		} catch (ParseException e) {
			return null;
		}
	}

	/**
	 * Convenience method for {@link #parseDateStrict(String)} with the
	 * following difference: null or empty input returns null. Any other parse
	 * errors are wrapped as an IllegalArgumentException.
	 * 
	 * @param dateString
	 *            the date string to parse
	 * @return a Date representation of the dateString or null
	 * @throws IllegalArgumentException
	 *             if dateString is unable to be parsed.
	 */
	public static Date parseDateOrNull(String dateString)
			throws IllegalArgumentException {
		if (dateString == null) {
			return null;
		} else if (dateString.isEmpty()) {
			return null;
		}
		try {
			return parseDateStrict(dateString);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	/**
	 * Attempt to parse the given string of form: yyyy-MM-dd[THH:mm:ss[.SSS][Z]]
	 * as a Date.
	 * 
	 * @param dateString
	 *            the date string to parse
	 * @return a Date representation of the dateString
	 * @throws ParseException
	 *             if dateString is null, empty or is otherwise unable to be
	 *             parsed.
	 */
	public static Date parseDateStrict(String dateString) throws ParseException {
		try {
			if (dateString == null) {
				throw new ParseException("Argument cannot be null.", 0);
			}
			
            int last = dateString.length() - 1;
			if (dateString.length() == 0) {
				throw new ParseException("Argument cannot be empty.", 0);
			} else if (dateString.charAt(last) == '.') {
				throw new ParseException(
						"dateString ends with invalid character.",
						dateString.length() - 1);
			}
			// SimpleDateFormat formatter = new SimpleDateFormat();
			// formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
			int length = dateString.length();
			if (dateString.charAt(0) == '-') {
				length--;
			}
			DateTimeFormatter formatter = FORMATTER_MILLISECONDS_T_Z;
			if (dateString.charAt(last) == 'Z') {
				if (length == 11) {
					// formatter.applyPattern("yyyy-MM-dd'Z'");
					formatter = FORMATTER_DATE_Z;
				} else if (length == 20) {
					// formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
					formatter = FORMATTER_SECONDS_T_Z;
				} else if (length > 21 && length < 24) {
					// right-pad the milliseconds with 0s up to three places
					StringBuilder sb = new StringBuilder(dateString.subSequence(
							0, last));
					int dotIndex = sb.lastIndexOf(".");
					int endIndex = sb.length() - 1;
					int padding = 3 - (endIndex - dotIndex);
					for (int i = 0; i < padding; i++) {
						sb.append('0');
					}
					sb.append('Z');
					dateString = sb.toString();
					// formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
					formatter = FORMATTER_MILLISECONDS_T_Z;
				} else if (length == 24) {
					// formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
					formatter = FORMATTER_MILLISECONDS_T_Z;
				}
			} else {
				if (length == 10) {
					// formatter.applyPattern("yyyy-MM-dd");
					formatter = FORMATTER_DATE;
				} else if (length == 19) {
					// formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
					formatter = FORMATTER_SECONDS_T;
				} else if (length > 20 && length < 23) {
					// right-pad millis with 0s
					StringBuilder sb = new StringBuilder(dateString);
					int dotIndex = sb.lastIndexOf(".");
					int endIndex = sb.length() - 1;
					int padding = 3 - (endIndex - dotIndex);
					for (int i = 0; i < padding; i++) {
						sb.append('0');
					}
					dateString = sb.toString();
					// formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
					formatter = FORMATTER_MILLISECONDS_T;
				} else if (length == 23) {
					// formatter.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
					formatter = FORMATTER_MILLISECONDS_T;
				} else if (dateString.endsWith("GMT")
						|| dateString.endsWith("UTC")) {
					// formatter.applyPattern("EEE, dd MMMM yyyyy HH:mm:ss z");
					// this has to be done by hand since Joda time can't parse
					// the timezone
					// see
					// http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
					dateString = dateString.substring(0,
							dateString.length() - 4);
					formatter = FORMATTER_TIMEZONE;
				}
			}
			DateTimeZone.setDefault(DateTimeZone.UTC);
			DateTime dt = formatter.parseDateTime(dateString);
			return dt.toDate();
		} catch (IllegalArgumentException e) {
			throw new ParseException(e.getLocalizedMessage(), 0);
		}
	}
}
