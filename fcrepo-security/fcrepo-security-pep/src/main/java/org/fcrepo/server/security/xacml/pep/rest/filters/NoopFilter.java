package org.fcrepo.server.security.xacml.pep.rest.filters;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.xacml.ctx.RequestCtx;

import org.fcrepo.server.security.xacml.pep.PEPException;

/**
 * A filter for operations that require no authorisation
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class NoopFilter
        extends AbstractFilter {

    public NoopFilter() throws PEPException {
        super();
    }

    @Override
    public RequestCtx handleRequest(HttpServletRequest request,
                                    HttpServletResponse response)
            throws IOException, ServletException {
        return null;
    }

}
