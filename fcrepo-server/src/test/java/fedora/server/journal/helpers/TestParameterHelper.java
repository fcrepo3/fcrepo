/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.journal.helpers;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * <p>
 * <b>Title:</b> TestParameterHelper.java
 * </p>
 * <p>
 * <b>Description:</b> Test cases for the ParameterHelper class.
 * </p>
 *
 * @author jblake@cs.cornell.edu
 * @version $Id: TestParameterHelper.java 6734 2008-03-03 22:12:28 +0000 (Mon,
 *          03 Mar 2008) j2blake $
 */

public class TestParameterHelper
        implements JournalConstants {

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestParameterHelper.class);
    }

    private static final String PARAMETER_NAME = "name";

    private Map<String, String> parameters;

    @Before
    public void setUp() {
        parameters = new HashMap<String, String>();
    }

    @Test
    public void testGetOptionalBooleanParameter_NullParameters()
            throws JournalException {
        try {
            ParameterHelper.getOptionalBooleanParameter(null,
                                                        PARAMETER_NAME,
                                                        false);
            fail("Expected a NullPointerException");
        } catch (NullPointerException e) {
            // expected the exception
        }
    }

    @Test
    public void testGetOptionalBooleanParameter_NullParameterName()
            throws JournalException {
        try {
            ParameterHelper
                    .getOptionalBooleanParameter(parameters, null, false);
            fail("Expected a NullPointerException");
        } catch (NullPointerException e) {
            // expected the exception
        }
    }

    @Test
    public void testGetOptionalBooleanParameter_ValueTrue()
            throws JournalException {
        parameters.put(PARAMETER_NAME, VALUE_TRUE);
        boolean result =
                ParameterHelper.getOptionalBooleanParameter(parameters,
                                                            PARAMETER_NAME,
                                                            false);
        assertEquals(true, result);
    }

    @Test
    public void testGetOptionalBooleanParameter_ValueFalse()
            throws JournalException {
        parameters.put(PARAMETER_NAME, VALUE_FALSE);
        boolean result =
                ParameterHelper.getOptionalBooleanParameter(parameters,
                                                            PARAMETER_NAME,
                                                            true);
        assertEquals(false, result);
    }

    @Test
    public void testGetOptionalBooleanParameter_NoValue()
            throws JournalException {
        boolean result1 =
                ParameterHelper.getOptionalBooleanParameter(parameters,
                                                            PARAMETER_NAME,
                                                            true);
        assertEquals(true, result1);

        boolean result2 =
                ParameterHelper.getOptionalBooleanParameter(parameters,
                                                            PARAMETER_NAME,
                                                            false);
        assertEquals(false, result2);
    }

    @Test
    public void testGetOptionalBooleanParameter_InvalidValue() {
        parameters.put(PARAMETER_NAME, "BOGUS");
        try {
            ParameterHelper.getOptionalBooleanParameter(parameters,
                                                        PARAMETER_NAME,
                                                        false);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testGetOptionalStringParameter_Value() {
        parameters.put("fred", "theValue");
        String result =
                ParameterHelper.getOptionalStringParameter(parameters,
                                                           "fred",
                                                           "WhoCares?");
        assertEquals("theValue", result);
    }

    @Test
    public void testGetOptionalStringParameter_Default() {
        String result =
                ParameterHelper.getOptionalStringParameter(parameters,
                                                           "fred",
                                                           "WhoCares?");
        assertEquals("WhoCares?", result);
    }

    @Test
    public void testParseParametersForFilenamePrefix_Value() {
        parameters.put(PARAMETER_JOURNAL_FILENAME_PREFIX, "somePrefix");
        String result =
                ParameterHelper.parseParametersForFilenamePrefix(parameters);
        assertEquals("somePrefix", result);
    }

    @Test
    public void testParseParametersForFilenamePrefix_Default() {
        String result =
                ParameterHelper.parseParametersForFilenamePrefix(parameters);
        assertEquals(DEFAULT_FILENAME_PREFIX, result);
    }

    @Test
    public void testParseParametersForSizeLimit_Default()
            throws JournalException {
        long actual = ParameterHelper.parseParametersForSizeLimit(parameters);
        parameters.put(PARAMETER_JOURNAL_FILE_SIZE_LIMIT, DEFAULT_SIZE_LIMIT);
        long expected = ParameterHelper.parseParametersForSizeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForSizeLimit_NoSuffix()
            throws JournalException {
        long expected = 4196L;
        parameters.put(PARAMETER_JOURNAL_FILE_SIZE_LIMIT, "4196");
        long actual = ParameterHelper.parseParametersForSizeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForSizeLimit_K() throws JournalException {
        long expected = 168L * 1024L;
        parameters.put(PARAMETER_JOURNAL_FILE_SIZE_LIMIT, "168K");
        long actual = ParameterHelper.parseParametersForSizeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForSizeLimit_M() throws JournalException {
        long expected = 5L * 1024L * 1024L;
        parameters.put(PARAMETER_JOURNAL_FILE_SIZE_LIMIT, "5M");
        long actual = ParameterHelper.parseParametersForSizeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForSizeLimit_G() throws JournalException {
        long expected = 999L * 1024L * 1024L * 1024L;
        parameters.put(PARAMETER_JOURNAL_FILE_SIZE_LIMIT, "999G");
        long actual = ParameterHelper.parseParametersForSizeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForSizeLimit_InvalidSuffix() {
        parameters.put(PARAMETER_JOURNAL_FILE_SIZE_LIMIT, "5X");
        try {
            ParameterHelper.parseParametersForSizeLimit(parameters);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

    @Test
    public void testParseParametersForAgeLimit_Default()
            throws JournalException {
        long actual = ParameterHelper.parseParametersForAgeLimit(parameters);
        parameters.put(PARAMETER_JOURNAL_FILE_AGE_LIMIT, DEFAULT_AGE_LIMIT);
        long expected = ParameterHelper.parseParametersForAgeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForAgeLimit_NoSuffix()
            throws JournalException {
        long expected = 4196L * 1000L;
        parameters.put(PARAMETER_JOURNAL_FILE_AGE_LIMIT, "4196");
        long actual = ParameterHelper.parseParametersForAgeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForAgeLimit_M() throws JournalException {
        long expected = 168L * 1000L * 60L;
        parameters.put(PARAMETER_JOURNAL_FILE_AGE_LIMIT, "168M");
        long actual = ParameterHelper.parseParametersForAgeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForAgeLimit_H() throws JournalException {
        long expected = 5L * 1000L * 60L * 60L;
        parameters.put(PARAMETER_JOURNAL_FILE_AGE_LIMIT, "5H");
        long actual = ParameterHelper.parseParametersForAgeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForAgeLimit_D() throws JournalException {
        long expected = 999L * 1000L * 60L * 60L * 24L;
        parameters.put(PARAMETER_JOURNAL_FILE_AGE_LIMIT, "999D");
        long actual = ParameterHelper.parseParametersForAgeLimit(parameters);
        assertEquals(expected, actual);
    }

    @Test
    public void testParseParametersForAgeLimit_InvalidSuffix() {
        parameters.put(PARAMETER_JOURNAL_FILE_AGE_LIMIT, "5X");
        try {
            ParameterHelper.parseParametersForAgeLimit(parameters);
            fail("Expected a JournalException");
        } catch (JournalException e) {
            // expected the exception
        }
    }

}
