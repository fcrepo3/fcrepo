/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bill Niebel
 */
public class CacheElement {

    private static final Log LOG = LogFactory.getLog(CacheElement.class);
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
    private Map m_namedValues = null;
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
                               Set predicates,
                               Map namedValues,
                               String errorMessage) {
        String m = m_cacheabbrev + " populate() ";
        LOG.debug(m + ">");
        try {
            if (predicates != null) {
                LOG.warn(m + " predicates are deprecated; will be ignored");
            }
            assertInvalid();
            if (errorMessage != null) {
                LOG.error(m + "errorMessage==" + errorMessage);
                throw new Exception(errorMessage);
            } else {
                validate(authenticated, namedValues);
                // can't set expiration here -- don't have cache reference
                // can't set pwd here, don't have it
            }
        } catch (Throwable t) {
            LOG.error(m + "invalidating to be sure");
            this.invalidate(errorMessage);
        } finally {
            LOG.debug(m + "<");
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
        LOG.debug(m + ">");
        Boolean rc = null;
        try {
            LOG.debug(m + "m_valid==" + m_valid);
            if (m_valid && !CacheElement.isExpired(m_expiration)) {
                LOG.debug(m + "valid and not expired, so use");
                if (!isAuthenticated()) {
                    LOG.debug(m + "auth==" + m_authenticated);
                    rc = m_authenticated;
                } else {
                    LOG.debug(m + "already authd, request password==" + pwd);
                    if (pwd == null) {
                        LOG.debug(m + "null request password");
                        rc = Boolean.FALSE;
                    } else if ("".equals(pwd)) {
                        LOG.debug(m + "zero-length request password");
                        rc = Boolean.FALSE;
                    } else {
                        LOG.debug(m + "stored password==" + m_password);
                        rc = pwd.equals(m_password);
                    }
                }
            } else { // expired or invalid
                LOG.debug(m + "expired or invalid, so try to repopulate");
                this.invalidate();
                CacheElementPopulator cePop = cache.getCacheElementPopulator();
                cePop.populateCacheElement(this, pwd);
                int duration = 0;
                String unit = null;
                m_password = null;
                if (m_authenticated == null || !m_valid) {
                    duration = cache.getAuthExceptionTimeoutDuration();
                    unit = cache.getAuthExceptionTimeoutUnit();
                    LOG.debug(m + "couldn't complete population");
                } else {
                    LOG.debug(m + "populate completed");
                    if (isAuthenticated()) {
                        m_password = pwd;
                        duration = cache.getAuthSuccessTimeoutDuration();
                        unit = cache.getAuthSuccessTimeoutUnit();
                        LOG.debug(m + "populate succeeded");
                    } else {
                        duration = cache.getAuthFailureTimeoutDuration();
                        unit = cache.getAuthFailureTimeoutUnit();
                        LOG.debug(m + "populate failed");
                    }
                }
                m_expiration = CacheElement.calcExpiration(duration, unit);
                rc = m_authenticated;
            }
        } catch (Throwable th) {
            this.invalidate();
            rc = m_authenticated;
            LOG.error(m + "invalidating to be sure");
        } finally {
            audit();
            LOG.debug(m + "< " + rc);
        }
        return rc;
    }

    public final synchronized Map getNamedValues(Cache cache, String pwd) {
        // Original Comment:
        // Synchronized so evaluation of cache item state will be sequential,
        // non-interlaced.  This protects against overlapping calls resulting in
        // redundant (authenticator?) calls.
        // TODO: refactor method name so that it doesn't look like "getter"
        String m = m_cacheabbrev + " namedValues ";
        LOG.debug(m + ">");
        Map rc = null;
        try {
            LOG.debug(m + "valid==" + m_valid);
            if (m_valid && !CacheElement.isExpired(m_expiration)) {
                LOG.debug(m + "valid and not expired, so use");
            } else {
                LOG.debug(m + "expired or invalid, so try to repopulate");
                this.invalidate();
                CacheElementPopulator cePop = cache.getCacheElementPopulator();
                cePop.populateCacheElement(this, pwd);
                int duration = 0;
                String unit = null;
                if (m_namedValues == null || !m_valid) {
                    duration = cache.getAuthExceptionTimeoutDuration();
                    unit = cache.getAuthExceptionTimeoutUnit();
                    LOG.debug(m + "couldn't complete population");
                } else {
                    LOG.debug(m + "populate completed");
                    if (m_namedValues == null) {
                        duration = cache.getAuthFailureTimeoutDuration();
                        unit = cache.getAuthFailureTimeoutUnit();
                        LOG.debug(m + "populate failed");
                    } else {
                        m_password = pwd;
                        duration = cache.getAuthSuccessTimeoutDuration();
                        unit = cache.getAuthSuccessTimeoutUnit();
                        LOG.debug(m + "populate succeeded");
                    }
                }
                m_expiration = CacheElement.calcExpiration(duration, unit);
            }
        } catch (Throwable th) {
            String msg = m + "invalidating to be sure";
            this.invalidate(msg);
            LOG.error(msg);
        } finally {
            audit();
            rc = m_namedValues;
            if (rc == null) {
                rc = new Hashtable();
            }
            LOG.debug(m + "< " + rc);
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
        if (LOG.isDebugEnabled()) {
            try {
                Calendar now = Calendar.getInstance();
                LOG.debug(m + "> " + m_cacheid + " " + getInstanceId()
                        + " @ " + format(now));
                LOG.debug(m + "valid==" + m_valid);
                LOG.debug(m + "userid==" + getUserid());
                LOG.debug(m + "password==" + m_password);
                LOG.debug(m + "authenticated==" + m_authenticated);
                LOG.debug(m + "errorMessage==" + m_errorMessage);
                LOG.debug(m + "expiration==" + format(m_expiration));
                LOG.debug(m + compareForExpiration(now, m_expiration));
                if (m_namedValues == null) {
                    LOG.debug(m + "(no named attributes");
                } else {
                    CacheElement.auditNamedValues(m, m_namedValues);
                }
            } finally {
                LOG.debug(m + "<");
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
            LOG.debug(m + m_errorMessage);
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

    private void validate(Boolean authenticated, Map namedValues) {
        assertInvalid();
        m_authenticated = authenticated;
        m_namedValues = namedValues;
        m_errorMessage = null;
        m_valid = true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private Class Methods
    ///////////////////////////////////////////////////////////////////////////

    private static final void auditNamedValues(String m, Map namedValues) {
        if (LOG.isDebugEnabled()) {
            assert namedValues != null;
            for (Iterator outer = namedValues.keySet().iterator(); outer
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
                    Set values = (Set) temp;
                    sb.append("(" + values.size() + ") {");
                    String punct = "";
                    for (Iterator it = values.iterator(); it.hasNext();) {
                        temp = it.next();
                        if (!(temp instanceof String)) {
                            LOG.error(m + "set member not string, ==" + temp);
                        } else {
                            String value = (String) temp;
                            sb.append(punct + value);
                            punct = ",";
                        }
                    }
                    sb.append("}");
                }
                LOG.debug(sb.toString());
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
                rc = padNeeded + st;
            } else {
                rc = st + padNeeded;
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
            sb.append(" ");
        } else {
            sb.append(Long.toString(day));
            sb.append(" days ");
        }
        sb.append(CacheElement.pad(hour, "00"));
        sb.append(":");
        sb.append(CacheElement.pad(minute, "00"));
        sb.append(":");
        sb.append(CacheElement.pad(second, "00"));
        sb.append(".");
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
        LOG.debug(m + ">");
        Calendar now = Calendar.getInstance();
        Calendar rc = Calendar.getInstance();
        try {
            CacheElement.checkCalcExpiration(duration, unit);
            if (duration > 0) {
                rc.add(unit, duration);
                LOG.debug(m + CacheElement.compareForExpiration(now, rc));
            } else {
                LOG.debug(m + "timeout set to now (effectively, no caching)");
            }
        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug(m + "< " + format(rc));
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
            LOG.error(m + "(" + msg + ")");
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
            LOG.error(m + "using duration==" + duration);
            LOG.error(m + "using calendarUnit==" + calendarUnit);
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
            LOG.debug(m + ">");
        }
        boolean rc = CacheElement.s_expired_default;
        try {
            if (now == null) {
                String msg = "illegal parm now==" + now;
                LOG.error(m + "(" + msg + ")");
                throw new IllegalArgumentException(msg);
            }
            if (expiration == null) {
                String msg = "illegal parm expiration==" + expiration;
                LOG.error(m + "(" + msg + ")");
                throw new IllegalArgumentException(msg);
            }
            if (verbose) {
                LOG.debug(m + "now==" + format(now));
                LOG.debug(m + "exp==" + format(expiration));
            }
            rc = !now.before(expiration);
        } catch (Throwable th) {
            LOG.error(m + "failed comparison");
            rc = CacheElement.s_expired_default;
        } finally {
            if (verbose) {
                LOG.debug(m + compareForExpiration(now, expiration));
                LOG.debug(m + "< " + rc);
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
                LOG.error(m + "(" + msg + ")");
                throw new IllegalArgumentException(msg);
            }
            Calendar now = Calendar.getInstance();
            rc = CacheElement.isExpired(now, expiration, verbose);
        } catch (Throwable th) {
            LOG.error(m + "failed comparison");
            rc = CacheElement.s_expired_default;
        }
        return rc;
    }

    private static final boolean isExpired(Calendar now) {
        return isExpired(now, true);
    }

}
