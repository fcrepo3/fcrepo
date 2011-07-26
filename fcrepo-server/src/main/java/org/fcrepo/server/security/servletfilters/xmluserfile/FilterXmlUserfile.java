/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.servletfilters.xmluserfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

import org.fcrepo.common.Constants;
import org.fcrepo.server.security.servletfilters.BaseCaching;
import org.fcrepo.server.security.servletfilters.CacheElement;
import org.fcrepo.server.security.servletfilters.FinishedParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Bill Niebel
 * @deprecated
 */
@Deprecated
public class FilterXmlUserfile
        extends BaseCaching
        implements Constants {

    private static final Logger logger =
            LoggerFactory.getLogger(FilterXmlUserfile.class);

    private static final String FILEPATH_KEY = "filepath";

    private String FILEPATH = "";

    private final String getFilepath() {
        if (FILEPATH == null || FILEPATH.equals("")) {
            FILEPATH = FedoraUsers.fedoraUsersXML.getAbsolutePath();
        }
        return FILEPATH;
    }

    @Override
    public void destroy() {
        String method = "destroy()";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        super.destroy();
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    @Override
    protected void initThisSubclass(String key, String value) {
        String method = "initThisSubclass()";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        boolean setLocally = false;

        if (FILEPATH_KEY.equals(key)) {
            FILEPATH = value;
            setLocally = true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug(format(method, "deferring to super"));
            }
            super.initThisSubclass(key, value);
        }
        if (setLocally) {
            if (logger.isInfoEnabled()) {
                logger.info(method + "known parameter " + key + "==" + value);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }

    @Override
    public void populateCacheElement(CacheElement cacheElement, String password) {
        String method = "populateCacheElement()";
        if (logger.isDebugEnabled()) {
            logger.debug(enter(method));
        }
        Boolean authenticated = null;
        Map namedAttributes = null;
        String errorMessage = null;
        authenticated = Boolean.FALSE;

        try {
            InputStream is;
            try {
                is = new FileInputStream(getFilepath());
            } catch (Throwable th) {
                logger.error("error reading tomcat users file " + getFilepath(), th);
                throw th;
            }

            ParserXmlUserfile parser = new ParserXmlUserfile(is);
            try {
                parser.parse(cacheElement.getUserid(), password);
                logger.debug("finished parsing");
            } catch (FinishedParsingException f) {
                if (logger.isDebugEnabled()) {
                    logger.debug(format(method, "got finished parsing exception"));
                }
            } catch (Throwable th) {
                String msg = "error parsing tomcat users file";
                logger.error(msg, th);
                throw new IOException(msg, th);
            }
            authenticated = parser.getAuthenticated();
            namedAttributes = parser.getNamedAttributes();
        } catch (Throwable t) {
            authenticated = null;
            namedAttributes = null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(format(method, null, "authenticated"));
            logger.debug(authenticated.toString());
            logger.debug(format(method, null, "namedAttributes"));
            logger.debug(namedAttributes.toString());
            logger.debug(format(method, null, "errorMessage", errorMessage));
        }
        cacheElement.populate(authenticated,
                              null,
                              namedAttributes,
                              errorMessage);
        if (logger.isDebugEnabled()) {
            logger.debug(exit(method));
        }
    }
}
