/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security.servletfilters.xmluserfile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fedora.common.Constants;

import fedora.server.security.servletfilters.BaseCaching;
import fedora.server.security.servletfilters.CacheElement;
import fedora.server.security.servletfilters.FinishedParsingException;

/**
 * @author Bill Niebel
 */
public class FilterXmlUserfile
        extends BaseCaching
        implements Constants {

    protected static Log log = LogFactory.getLog(FilterXmlUserfile.class);

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
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        super.destroy();
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    protected void initThisSubclass(String key, String value) {
        String method = "initThisSubclass()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
        }
        boolean setLocally = false;

        if (FILEPATH_KEY.equals(key)) {
            FILEPATH = value;
            setLocally = true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(format(method, "deferring to super"));
            }
            super.initThisSubclass(key, value);
        }
        if (setLocally) {
            if (log.isInfoEnabled()) {
                log.info(method + "known parameter " + key + "==" + value);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }

    @Override
    public void populateCacheElement(CacheElement cacheElement, String password) {
        String method = "populateCacheElement()";
        if (log.isDebugEnabled()) {
            log.debug(enter(method));
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
                showThrowable(th, log, "error reading tomcat users file "
                        + getFilepath());
                throw th;
            }
            if (log.isDebugEnabled()) {
                log.debug("read tomcat-users.xml");
            }

            ParserXmlUserfile parser = new ParserXmlUserfile(is);
            if (log.isDebugEnabled()) {
                log.debug("got parser");
            }
            try {
                parser.parse(cacheElement.getUserid(), password);
                if (log.isDebugEnabled()) {
                    log.debug("back from parsing");
                }
            } catch (FinishedParsingException f) {
                if (log.isDebugEnabled()) {
                    log.debug(format(method, "got finished parsing exception"));
                }
            } catch (Throwable th) {
                String msg = "error parsing tomcat users file";
                showThrowable(th, log, msg);
                throw new IOException(msg);
            }
            authenticated = parser.getAuthenticated();
            namedAttributes = parser.getNamedAttributes();
        } catch (Throwable t) {
            authenticated = null;
            namedAttributes = null;
        }
        if (log.isDebugEnabled()) {
            log.debug(format(method, null, "authenticated"));
            log.debug(authenticated);
            log.debug(format(method, null, "namedAttributes"));
            log.debug(namedAttributes);
            log.debug(format(method, null, "errorMessage", errorMessage));
        }
        cacheElement.populate(authenticated,
                              null,
                              namedAttributes,
                              errorMessage);
        if (log.isDebugEnabled()) {
            log.debug(exit(method));
        }
    }
}
