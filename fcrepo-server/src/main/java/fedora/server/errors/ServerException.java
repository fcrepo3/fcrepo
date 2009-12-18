/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.errors;

import java.text.MessageFormat;

import java.util.Locale;
import java.util.ResourceBundle;

import fedora.server.Server;

/**
 * The superclass for all Fedora server exceptions.
 * 
 * <p>This class encourages the use of resource bundles as message (and detail
 * text) sources so that localization may easily be implemented.
 *
 * <p>Methods that return text will attempt to find the message by code in the
 * ResourceBundle, but will fall back to simply returning the code if no such
 * message can be found in the ResourceBundle, or no such ResourceBundle exists
 * or the bundleName given in the constructor is null.
 *
 * <p>This enables developers to temporarily construct exceptions with something
 * like:
 * <pre>
 *     throw new MyException(null, "myMessageId", null, null, null);
 * </pre>
 * 
 * <p>Exceptions of this type have the benefit that they can be easily 
 * converted to informative, localized SOAP Fault envelopes.
 * 
 * @author Chris Wilper
 */
public abstract class ServerException
        extends Exception {

    /** The bundle in which the message, identified by m_code, resides. */
    private final String m_bundleName;

    /** The identifier for the message in the bundle. */
    private final String m_code;

    /** The message in the default locale, if it's already been determined. */
    private String m_defaultMessage;

    /** Replacements for placeholders in the message, starting at {0}. */
    private final String[] m_values;

    /** Identifiers for messages that provide detail on the error */
    private final String[] m_details;

    /** Whether the error was internal to the server, returned by wasServer() */
    private boolean m_wasServer;

    /** An empty string array */
    private static String[] s_emptyStringArray = new String[] {};

    /**
     * Constructs a new ServerException.
     * 
     * @param bundleName
     *        The bundle in which the message resides.
     * @param code
     *        The identifier for the message in the bundle, aka the key.
     * @param values
     *        Replacements for placeholders in the message, where placeholders
     *        are of the form {num} where num starts at 0, indicating the 0th
     *        (1st) item in this array.
     * @param details
     *        Identifiers for messages which provide detail on the error. This
     *        may empty or null.
     * @param cause
     *        The underlying exception if known, null meaning unknown or none.
     */
    public ServerException(String bundleName,
                           String code,
                           String[] values,
                           String[] details,
                           Throwable cause) {
        super(code, cause);
        m_bundleName = bundleName;
        m_code = code;
        m_values = values;
        m_details = details;
        m_wasServer = false;
    }

    /**
     * Gets the identifier for the message.
     * 
     * @return The code, which is also the key in the <code>MessageBundle</code>
     *         for this exception.
     */
    public String getCode() {
        return m_code;
    }

    /**
     * Tells whether the error occurred because of an unexpected error in the
     * server, likely requiring action on the part of the server administrator.
     * <p>
     * </p>
     * If it's not an error in the server, it means that the client made a
     * mistake, which is the more likely case.
     */
    public boolean wasServer() {
        return m_wasServer;
    }

    /**
     * Sets the value for the "wasServer" flag for this exception.
     */
    public void setWasServer() {
        m_wasServer = true;
    }

    /**
     * Gets the message, preferring the <code>Server</code> locale.
     * 
     * @return The message, with {num}-indexed placeholders populated, if
     *         needed.
     */
    @Override
    public String getMessage() {
        if (m_defaultMessage == null) {
            m_defaultMessage =
                    getLocalizedOrCode(m_bundleName,
                                       Server.getLocale(),
                                       m_code,
                                       m_values);
        }
        return m_defaultMessage;
    }

    /**
     * Gets the message, preferring the provided locale.
     * 
     * <p>When a message in the desired locale is not found, the locale 
     * selection logic described by <a
     * href="http://java.sun.com/j2se/1.4/docs/api/java/util/ResourceBundle.html">the
     * java.util.ResourceBundle</a> class javadoc is used.
     * 
     * @param locale
     *        The preferred locale.
     * @return The message, with {num}-indexed placeholders populated, if
     *         needed.
     */
    public String getMessage(Locale locale) {
        return getLocalizedOrCode(m_bundleName, locale, m_code, m_values);
    }

    /**
     * Gets any detail messages, preferring the <code>Server</code> locale.
     * 
     * @return The detail messages, with {num}-indexed placeholders populated,
     *         if needed.
     */
    public String[] getDetails() {
        return getDetails(Server.getLocale());
    }

    /**
     * Gets any detail messages, preferring the provided locale.
     * 
     * @return The detail messages, with {num}-indexed placeholders populated,
     *         if needed.
     */
    public String[] getDetails(Locale locale) {
        if (m_details == null || m_details.length == 0) {
            return s_emptyStringArray;
        }
        String[] ret = new String[m_details.length];
        for (int i = 0; i < m_details.length; i++) {
            ret[i] = getLocalizedOrCode(m_bundleName, locale, m_code, null);
        }
        return ret;
    }

    /**
     * Gets the message from the resource bundle, formatting it if needed, and
     * on failure, just returns the code.
     * 
     * @param bundleName
     *        The ResourceBundle where the message is defined.
     * @param locale
     *        The preferred locale.
     * @param code
     *        The message key.
     * @param values
     *        The replacement values, assumed empty if null.
     * @return The detail messages, with {num}-indexed placeholders populated,
     *         if needed.
     */
    private static String getLocalizedOrCode(String bundleName,
                                             Locale locale,
                                             String code,
                                             String[] values) {
        ResourceBundle bundle = null;
        if (bundleName != null) {
            bundle = ResourceBundle.getBundle(bundleName, locale);
        }
        if (bundle == null) {
            return code;
        }
        String locMessage = bundle.getString(code);
        if (locMessage == null) {
            return code;
        }
        if (values == null) {
            return locMessage;
        }
        return MessageFormat.format(locMessage, (Object[]) values);
    }

}
