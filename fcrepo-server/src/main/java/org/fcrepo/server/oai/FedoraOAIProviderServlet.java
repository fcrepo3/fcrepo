/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.oai;

import java.io.File;

import org.fcrepo.common.Constants;
import org.fcrepo.oai.OAIProvider;
import org.fcrepo.oai.OAIProviderServlet;
import org.fcrepo.oai.OAIResponder;
import org.fcrepo.oai.RepositoryException;
import org.fcrepo.server.Server;




/**
 * FedoraOAIProviderServlet.
 * 
 * @author Chris Wilper
 */
public class FedoraOAIProviderServlet
        extends OAIProviderServlet {

    private static final long serialVersionUID = 1L;

    OAIResponder m_responder;

    @Override
    public OAIResponder getResponder() throws RepositoryException {
        if (m_responder == null) {
            try {
                Server server =
                        Server.getInstance(new File(Constants.FEDORA_HOME),
                                           false);
                OAIProvider provider =
                        (OAIProvider) server
                                .getModule("org.fcrepo.oai.OAIProvider");
                m_responder = new OAIResponder(provider);
            } catch (Exception e) {
                throw new RepositoryException(e.getClass().getName() + ": "
                        + e.getMessage());
            }
        }
        return m_responder;
    }

    public static void main(String[] args) throws Exception {
        new FedoraOAIProviderServlet().test(args);
    }

}
