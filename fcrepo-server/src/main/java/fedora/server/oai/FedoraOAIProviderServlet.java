/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.oai;

import java.io.File;

import fedora.common.Constants;

import fedora.oai.OAIProvider;
import fedora.oai.OAIProviderServlet;
import fedora.oai.OAIResponder;
import fedora.oai.RepositoryException;

import fedora.server.Server;

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
                                .getModule("fedora.oai.OAIProvider");
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
