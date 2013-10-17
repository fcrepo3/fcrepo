/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage.lowlevel;

import java.io.File;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import org.fcrepo.server.errors.LowlevelStorageException;


/**
 * @author Bill Niebel
 */
class TimestampPathAlgorithm
        extends PathAlgorithm {

    private final String storeBase;

    private static final String[] PADDED =
        {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09"};

    private static final String SEP = File.separator;

    public TimestampPathAlgorithm(Map<String, ?> configuration) {
        super(configuration);
        storeBase = (String) configuration.get("storeBase");
    }

    @Override
    public final String get(String pid) throws LowlevelStorageException {
        return format(encode(pid));
    }

    public String format(String pid) throws LowlevelStorageException {
        GregorianCalendar calendar = new GregorianCalendar();
        String year = Integer.toString(calendar.get(Calendar.YEAR));
        String month = leftPaddedUnder100(1 + calendar.get(Calendar.MONTH));
        String dayOfMonth = leftPaddedUnder100(calendar.get(Calendar.DAY_OF_MONTH));
        String hourOfDay = leftPaddedUnder100(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = leftPaddedUnder100(calendar.get(Calendar.MINUTE));
        return storeBase + SEP + year + SEP + month + dayOfMonth + SEP
                + hourOfDay + SEP + minute + SEP + pid;
    }

    private final String leftPaddedUnder100(int i)
            throws LowlevelStorageException {
        if (i < 0 || i > 99) {
            throw new LowlevelStorageException(true, getClass().getName()
                    + ": faulty date padding");
        }
        if (i < 10) {
            return PADDED[i];
        }
        return Integer.toString(i);
    }
}
