/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.rest.param;

import java.util.Date;

import javax.ws.rs.WebApplicationException;

import org.fcrepo.server.utilities.DateUtility;

/**
 * A JAX-RS parameter handler for ISO datetime Strings.
 *
 * @author Edwin Shin
 * @version $Id$
 */
public class DateTimeParam
        extends AbstractParam<Date> {

    public DateTimeParam(String param)
            throws WebApplicationException {
        super(param);
    }

    @Override
    protected Date parse(String param) throws Throwable {
        return DateUtility.parseDateOrNull(param);
    }

    @Override
    public String toString() {
        return DateUtility.convertDateToXSDString(getValue());
    }
}
