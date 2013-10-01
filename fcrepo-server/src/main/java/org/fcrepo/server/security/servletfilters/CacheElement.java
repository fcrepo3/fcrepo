/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
public class CacheElement {

    private static final Logger logger =
            LoggerFactory.getLogger(CacheElement.class);

    private static final Calendar EARLIER;
    private static final boolean s_expired_default = true; // safest
    private static final long MILLIS_IN_SECOND = 1000;
    private static final long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    private static final long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;

    private final String m_userid;
    private final String m_cacheid;
    private final String m_cacheabbrev;

    private String m_password = null;
    private boolean m_valid = false;
    private Calendar m_expiration = null;
    private Boolean m_authenticated = null;
    // a map of String to Set<String>
    private Map<String, Set<?>> m_namedValues = null;
    private String m_errorMessage = null;

    static {
        Calendar temp = Calendar.getInstance();
        temp.set(Calendar.YEAR, 1999);
        EARLIER = temp;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Primary Contract
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Create a new cache element with the given userid.
     *
     * Note: The element will start in the invalid state.
     */
    public CacheElement(String userid, String cacheid, String cacheabbrev) {
        m_userid = userid;
        m_cacheid = cacheid;
        m_cacheabbrev = cacheabbrev;
        this.invalidate();
    }

    /**
     * Gets the user id associated with this cache element.
     */
    public String getUserid() {
        return m_userid;
    }

    /**
     * Populates this cache element with the given authenticated state
     * and named values, then puts it in the valid state.
     *
     * Note: Prior to the call, the element must be in the invalid state.
     * TODO: The predicates parameter is deprecated and should be removed.
     *        For now, callers can avoid a warning by giving it as null.
     */
    public final void populate(Boolean authenticated,
                               Set<?> predicates,
                               Map<String, Set<?>> map,
                               String errorMessage) {
        String m = m_cacheabbrev + " populate() ";
        logger.debug(m + ">");
        try {
            if (predicates != null) {
                logger.warn(m + " predicates are deprecated; will be ignored");
            }
            assertInvalid();
            if (errorMessage != null) {
                logger.error(m + "errorMessage==" + errorMessage);
                throw new Exception(errorMessage);
            } else {
                validate(authenticated, map);
                // can't set expiration here -- don't have cache reference
                // can't set pwd here, don't have it
            }
        } catch (Throwable t) {
            logger.error(m + "invalidating to be sure");
            this.invalidate(errorMessage);
        } finally {
            logger.debug(m + "<");
        }
    }

    /**
     * If in the valid state and not expired:
     *   If authenticated and given password is instance password, return true.
     *   Else return false.
     * If invalid or expired:
     *   Re-initialize this element by setting it to invalid state and running
     *     the underlying authN code.
     *   If never authenticated or currently not valid, return m_authenticated
     *   If authenticated, set thekkkk instance password to the given one and return true.
     *   If not authenticated, return false.
     */
    public final synchronized Boolean authenticate(Cache cache, String pwd) {
        // Original Comment:
        // Synchronized so evaluation of cache item state will be sequential,
        // non-interlaced.  This protects against overlapping calls resulting in
        // redundant authenticator calls.
        String m = m_cacheabbrev + " authenticate() ";
        logger.debug(m + ">");
        Boolean rc = null;
        try {
            logger.debug(m + "m_valid==" + m_valid);
            if (m_valid && !CacheElement.isExpired(m_expiration)) {
                logger.debug(m + "valid and not expired, so use");
                if (!isAuthenticated()) {
                    logger.debug(m + "auth==" + m_authenticated);
                    rc = m_authenticated;
                } else {
                    logger.debug(m + "already authd, request password==" + pwd);
                    if (pwd == null) {
                        logger.debug(m + "null request password");
                        rc = Boolean.FALSE;
                    } else if ("".equals(pwd)) {
                        logger.debug(m + "zero-length request password");
                        rc = Boolean.FALSE;
                    } else {
                        logger.debug(m + "stored password==" + m_password);
                        rc = pwd.equals(m_password);
                    }
                }
            } else { // expired or invalid
                logger.debug(m + "expired or invalid, so try to repopulate");
                this.invalidate();
                CacheElementPopulator cePop = cache.getCacheElementPopulator();
                cePop.populateCacheElement(this, pwd);
                int duration = 0;
                String unit = null;
                m_password = null;
                if (m_authenticated == null || !m_valid) {
                    duration = cache.getAuthExceptionTimeoutDuration();
                    unit = cache.getAuthExceptionTimeoutUnit();
                    logger.debug(m + "couldn't complete population");
                } else {
                    logger.debug(m + "populate completed");
                    if (isAuthenticated()) {
                        m_password = pwd;
                        duration = cache.getAuthSuccessTimeoutDuration();
                        unit = cache.getAuthSuccessTimeoutUnit();
                        logger.debug(m + "populate succeeded");
                    } else {
                        duration = cache.getAuthFailureTimeoutDuration();
                        unit = cache.getAuthFailureTimeoutUnit();
                        logger.debug(m + "populate failed");
                    }
                }
                m_expiration = CacheElement.calcExpiration(duration, unit);
                rc = m_authenticated;
            }
        } catch (Throwable th) {
            this.invalidate();
            rc = m_authenticated;
            logger.error(m + "invalidating to be sure");
        } finally {
            audit();
            logger.debug(m + "< " + rc);
        }
        return rc;
    }

    public final synchronized Map<String, Set<?>> getNamedValues(Cache cache, String pwd) {
        // Original Comment:
        // Synchronized so evaluation of cache item state will be sequential,
        // non-interlaced.  This protects against overlapping calls resulting in
        // redundant (authenticator?) calls.
        // TODO: refactor method name so that it doesn't look like "getter"
        String m = m_cacheabbrev + " namedValues ";
        logger.debug(m + ">");
        Map<String, Set<?>> rc = null;
        try {
            logger.debug(m + "valid==" + m_valid);
            if (m_valid && !CacheElement.isExpired(m_expiration)) {
                logger.debug(m + "valid and not expired, so use");
            } else {
                logger.debug(m + "expired or invalid, so try to repopulate");
                this.invalidate();
                CacheElementPopulator cePop = cache.getCacheElementPopulator();
                cePop.populateCacheElement(this, pwd);
                int duration = 0;
                String unit = null;
                if (m_namedValues == null || !m_valid) {
                    duration = cache.getAuthExceptionTimeoutDuration();
                    unit = cache.getAuthExceptionTimeoutUnit();
                    logger.debug(m + "couldn't complete population");
                } else {
                    logger.debug(m + "populate completed");
                    m_password = pwd;
                    duration = cache.getAuthSuccessTimeoutDuration();
                    unit = cache.getAuthSuccessTimeoutUnit();
                    logger.debug(m + "populate succeeded");
                }
                m_expiration = CacheElement.calcExpiration(duration, unit);
            }
        } catch (Throwable th) {
            String msg = m + "invalidating to be sure";
            this.invalidate(msg);
            logger.error(msg);
        } finally {
            audit();
            rc = m_namedValues;
            if (rc == null) {
                rc = new HashMap<String, Set<?>>();
            }
            logger.debug(m + "< " + rc);
        }
        return rc;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Logging/Debugging Contract
    ///////////////////////////////////////////////////////////////////////////

    public String getInstanceId() {
        String rc = toString();
        int i = rc.indexOf("@");
        if (i > 0) {
            rc = rc.substring(i + 1);
        }
        return rc;
    }

    public final void audit() {
        String m = m_cacheabbrev + " audit() ";
        if (logger.isDebugEnabled()) {
            try {
                Calendar now = Calendar.getInstance();
                logger.debug(m + "> " + m_cacheid + " " + getInstanceId()
                        + " @ " + format(now));
                logger.debug(m + "valid==" + m_valid);
                logger.debug(m + "userid==" + getUserid());
                logger.debug(m + "password==" + m_password);
                logger.debug(m + "authenticated==" + m_authenticated);
                logger.debug(m + "errorMessage==" + m_errorMessage);
                logger.debug(m + "expiration==" + format(m_expiration));
                logger.debug(m + compareForExpiration(now, m_expiration));
                if (m_namedValues == null) {
                    logger.debug(m + "(no named attributes");
                } else {
                    CacheElement.auditNamedValues(m, m_namedValues);
                }
            } finally {
                logger.debug(m + "<");
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private Instance Methods
    ///////////////////////////////////////////////////////////////////////////

    private boolean isAuthenticated() {
        if (m_authenticated == null) return false;
        return m_authenticated.booleanValue();
    }

    private void invalidate() {
        invalidate(null);
    }

    private void invalidate(String errorMessage) {
        String m = m_cacheabbrev + " invalidate() ";
        m_valid = false;
        m_errorMessage = errorMessage;
        m_authenticated = null;
        m_namedValues = null;
        m_expiration = EARLIER;
        m_password = null;
        if (m_errorMessage != null) {
            logger.debug(m + m_errorMessage);
        }
    }

    private final void assertInvalid() {
        assert m_authenticated == null;
        assert m_namedValues == null;
        assert !m_valid;
        assert isExpired(m_expiration, false);
        assert m_password == null;
    }

    private static final void checkCalcExpiration(int duration, int unit)
            throws IllegalArgumentException {
        if (duration < 0) {
            throw new IllegalArgumentException("bad duration==" + duration);
        }
        switch (unit) {
            case Calendar.MILLISECOND:
            case Calendar.SECOND:
            case Calendar.MINUTE:
            case Calendar.HOUR:
                break;
            default:
                throw new IllegalArgumentException("bad unit==" + unit);
        }
    }

    private void validate(Boolean authenticated, Map<String, Set<?>> map) {
        assertInvalid();
        m_authenticated = authenticated;
        m_namedValues = map;
        m_errorMessage = null;
        m_valid = true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private Class Methods
    ///////////////////////////////////////////////////////////////////////////

    private static final void auditNamedValues(String m, Map<String, ?> namedValues) {
        if (logger.isDebugEnabled()) {
            assert namedValues != null;
            for (Iterator<String> outer = namedValues.keySet().iterator(); outer
                    .hasNext();) {
                Object name = outer.next();
                assert name instanceof String : "not a string, name==" + name;
                StringBuffer sb = new StringBuffer(m + name + "==");
                Object temp = namedValues.get(name);
                assert temp instanceof String || temp instanceof Set : "neither string nor set, temp=="
                        + temp;
                if (temp instanceof String) {
                    sb.append(temp.toString());
                } else if (temp instanceof Set) {
                    @SuppressWarnings("unchecked")
                    Set<String> values = (Set<String>) temp;
                    sb.append("(" + values.size() + ") {");
                    String punct = "";
                    for (Iterator<String> it = values.iterator(); it.hasNext();) {
                        String value = it.next();
                        sb.append(punct + value);
                        punct = ",";
                    }
                    sb.append("}");
                }
                logger.debug(sb.toString());
            }
        }
    }

    private static final String pad(long i, String pad, boolean padLeft) {
        String rc = "";
        String st = Long.toString(i);
        if (st.length() == pad.length()) {
            rc = st;
        } else if (st.length() > pad.length()) {
            rc = st.substring(0, pad.length());
        } else {
            String padNeeded = pad.substring(0, pad.length() - st.length());
            if (padLeft) {
                rc = padNeeded.concat(st);
            } else {
                rc = st.concat(padNeeded);
            }
        }
        return rc;
    }

    private static final String pad(long i, String pad) {
        return CacheElement.pad(i, pad, true);
    }

    private static final String format(long day,
                                      long hour,
                                      long minute,
                                      long second,
                                      long millisecond,
                                      String dayPad) {
        StringBuffer sb = new StringBuffer();
        if (dayPad != null) {
            sb.append(CacheElement.pad(day, "00"));
            sb.append(' ');
        } else {
            sb.append(Long.toString(day));
            sb.append(" days ");
        }
        sb.append(CacheElement.pad(hour, "00"));
        sb.append(':');
        sb.append(CacheElement.pad(minute, "00"));
        sb.append(':');
        sb.append(CacheElement.pad(second, "00"));
        sb.append('.');
        sb.append(CacheElement.pad(millisecond, "000"));
        return sb.toString();
    }

    private static final String format(long year,
                                      long month,
                                      long day,
                                      long hour,
                                      long minute,
                                      long second,
                                      long millisecond) {
        StringBuffer sb = new StringBuffer();
        sb.append(CacheElement.pad(year, "0000"));
        sb.append("-");
        sb.append(CacheElement.pad(month, "00"));
        sb.append("-");
        sb.append(format(day, hour, minute, second, millisecond, "00"));
        return sb.toString();
    }

    private static final String format(Calendar time) {
        return format(time.get(Calendar.YEAR),
                      time.get(Calendar.MONTH) + 1,
                      time.get(Calendar.DATE),
                      time.get(Calendar.HOUR_OF_DAY),
                      time.get(Calendar.MINUTE),
                      time.get(Calendar.SECOND),
                      time.get(Calendar.MILLISECOND));
    }

    private static final String difference(Calendar earlier, Calendar later) {
        long milliseconds = later.getTimeInMillis() - earlier.getTimeInMillis();

        long days = milliseconds / MILLIS_IN_DAY;
        milliseconds = milliseconds % MILLIS_IN_DAY;
        long hours = milliseconds / MILLIS_IN_HOUR;
        milliseconds = milliseconds % MILLIS_IN_HOUR;
        long minutes = milliseconds / MILLIS_IN_MINUTE;
        milliseconds = milliseconds % MILLIS_IN_MINUTE;
        long seconds = milliseconds / MILLIS_IN_SECOND;
        milliseconds = milliseconds % MILLIS_IN_SECOND;
        String rc = format(days, hours, minutes, seconds, milliseconds, null);
        return rc;
    }

    private static final String compareForExpiration(Calendar first,
                                                    Calendar second) {
        String rc = null;
        if (first.before(second)) {
            rc = "expires in " + difference(first, second);
        } else {
            rc = "expired " + difference(second, first) + " ago";
        }
        return rc;
    }

    private static final Calendar calcExpiration(int duration, int unit) {
        String m = "- calcExpiration(int,int) ";
        logger.debug(m + ">");
        Calendar now = Calendar.getInstance();
        Calendar rc = Calendar.getInstance();
        try {
            CacheElement.checkCalcExpiration(duration, unit);
            if (duration > 0) {
                rc.add(unit, duration);
                logger.debug(m + CacheElement.compareForExpiration(now, rc));
            } else {
                logger.debug(m + "timeout set to now (effectively, no caching)");
            }
        } finally {
            if (logger.isDebugEnabled()) {
                logger.debug(m + "< " + format(rc));
            }
        }
        return rc;
    }

    private static final int calcCalendarUnit(String unit) {
        String m = "- calcCalendarUnit() ";
        int rc = Calendar.SECOND;
        if (!unit.endsWith("s")) {
            unit += "s";
        }
        if ("milliseconds".equalsIgnoreCase(unit)) {
            rc = Calendar.MILLISECOND;
        } else if ("seconds".equalsIgnoreCase(unit)) {
            rc = Calendar.SECOND;
        } else if ("minutes".equalsIgnoreCase(unit)) {
            rc = Calendar.MINUTE;
        } else if ("hours".equalsIgnoreCase(unit)) {
            rc = Calendar.HOUR;
        } else {
            String msg = "illegal Calendar unit: " + unit;
            logger.error(m + "(" + msg + ")");
            throw new IllegalArgumentException(msg);
        }
        return rc;
    }

    private static final Calendar calcExpiration(int duration, String unit) {
        String m = "- calcExpiration(int,String) ";
        Calendar rc = Calendar.getInstance();
        int calendarUnit = Calendar.SECOND;
        try {
            calendarUnit = calcCalendarUnit(unit);
        } catch (Throwable t) {
            duration = 0;
            logger.error(m + "using duration==" + duration);
            logger.error(m + "using calendarUnit==" + calendarUnit);
        } finally {
            rc = CacheElement.calcExpiration(duration, calendarUnit);
        }
        return rc;
    }

    private static final boolean isExpired(Calendar now,
                                          Calendar expiration,
                                          boolean verbose) {
        String m = "- isExpired() ";
        if (verbose) {
            logger.debug(m + ">");
        }
        boolean rc = CacheElement.s_expired_default;
        try {
            if (now == null) {
                String msg = "illegal parm now==" + now;
                logger.error(m + "(" + msg + ")");
                throw new IllegalArgumentException(msg);
            }
            if (expiration == null) {
                String msg = "illegal parm expiration==" + expiration;
                logger.error(m + "(" + msg + ")");
                throw new IllegalArgumentException(msg);
            }
            if (verbose) {
                logger.debug(m + "now==" + format(now));
                logger.debug(m + "exp==" + format(expiration));
            }
            rc = !now.before(expiration);
        } catch (Throwable th) {
            logger.error(m + "failed comparison");
            rc = CacheElement.s_expired_default;
        } finally {
            if (verbose) {
                logger.debug(m + compareForExpiration(now, expiration));
                logger.debug(m + "< " + rc);
            }
        }
        return rc;
    }

    private static final boolean isExpired(Calendar expiration, boolean verbose) {
        String m = "- isExpired() ";
        boolean rc = CacheElement.s_expired_default;
        try {
            if (expiration == null) {
                String msg = "illegal parm expiration==" + expiration;
                logger.error(m + "(" + msg + ")");
                throw new IllegalArgumentException(msg);
            }
            Calendar now = Calendar.getInstance();
            rc = CacheElement.isExpired(now, expiration, verbose);
        } catch (Throwable th) {
            logger.error(m + "failed comparison");
            rc = CacheElement.s_expired_default;
        }
        return rc;
    }

    private static final boolean isExpired(Calendar now) {
        return isExpired(now, true);
    }

}
