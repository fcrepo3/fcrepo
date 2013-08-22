/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bill Niebel
 */
public class Base {

    private static final Logger logger =
            LoggerFactory.getLogger(Base.class);

    protected static final String[] StringArrayPrototype = new String[0];

    private static final String INTO = " . . .";

    private static final String OUTOF = ". . . ";

    protected String getClassName() {
        String classname = this.getClass().getName();
        String[] parts = classname.split("\\.");
        if (parts.length > 0) {
            classname = parts[parts.length - 1];
        }
        return classname;
    }

    public final String enter(String method) {
        return getClassName() + "." + method + " " + INTO;
    }

    public final String exit(String method) {
        return getClassName() + "." + OUTOF + " " + method;
    }

    public final String enterExit(String method) {
        return getClassName() + "." + OUTOF + method + INTO;
    }

    public final String passFail(String method, String test, String result) {
        return getClassName() + "." + method + ": " + result + " " + test
                + " test";
    }

    public final String pass(String method, String test) {
        return passFail(method, test, "passed");
    }

    public final String fail(String method, String test) {
        return passFail(method, test, "failed");
    }

    public final String format(String method, String msg) {
        return getClassName() + "." + method + ": " + msg;
    }

    public final String format(String method, String msg, String name) {
        return format(method, msg, name, null);
    }

    public final String format(String method,
                               String msg,
                               String name,
                               String value) {
        return getClassName() + "." + method + ": "
                + (msg == null ? "" : msg + " ") + name + "=="
                + (value == null ? "" : value);
    }

    public static final boolean booleanValue(String string) throws Exception {
        if (Boolean.TRUE.toString().equals(string)
                || Boolean.FALSE.toString().equals(string)) {
            return (Boolean.parseBoolean(string));
        } else {
            throw new Exception("does not represent a boolean");
        }
    }

    protected boolean initErrors = false;

    protected void initThisSubclass(String key, String value) {
        logger.debug("AFB.iTS");
        String method = "initThisSubclass() ";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        initErrors = true;
        if (logger.isErrorEnabled()) {
            logger.error(format(method, "unknown parameter", key, value));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

}
