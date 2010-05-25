/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.rest.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * @author Edwin Shin
 * @version $Id$
 */
public class DateTimeParamTest {
    protected final String EPOCH_DT = "1970-01-01T00:00:00.000Z";

    private final String[] testParams =
            {"1970-01-01T00:00:00.000Z", "1970-01-01T00:00:00Z", "1989-11-09"};

    /**
     * Test method for
     * {@link org.fcrepo.server.rest.param.AbstractParam#getValue()}.
     */
    @Test
    public void testGetValue() {
        for (String testParam : testParams) {
            DateTimeParam param = new DateTimeParam(testParam);
            assertNotNull(param.getValue());
        }
    }

    /**
     * Test method for
     * {@link org.fcrepo.server.rest.param.AbstractParam#getOriginalParam()}.
     */
    @Test
    public void testGetOriginalParam() {
        for (String testParam : testParams) {
            DateTimeParam param = new DateTimeParam(testParam);
            assertEquals(testParam, param.getOriginalParam());
        }
    }

    /**
     * Test method for
     * {@link org.fcrepo.server.rest.param.AbstractParam#toString()}.
     */
    @Test
    public void testToString() {
        DateTimeParam param = new DateTimeParam(EPOCH_DT);
        assertEquals("1970-01-01T00:00:00Z", param.toString());
    }

}
