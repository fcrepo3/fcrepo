/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client;

import java.net.MalformedURLException;

import javax.xml.rpc.ServiceException;

import org.fcrepo.server.access.FedoraAPIAMTOM;


/**
 * @author Chris Wilper
 */
public abstract class APIAStubFactory {


    public static int SOCKET_TIMEOUT_SECONDS = 120;

    /**
     * Method to rewrite the default API-A base URL (specified in the service
     * locator class FedoraAPIAServiceLocator). In this case we allow the
     * protocol, host, and port parts of the service URL to be replaced. A SOAP
     * stub will be returned with the desired service endpoint URL.
     *
     * @param protocol
     * @param host
     * @param port
     * @param username
     * @param password
     * @return FedoraAPIA SOAP stub
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public static FedoraAPIAMTOM getStub(String protocol,
                                     String host,
                                     int port,
                                     String username,
                                     String password)
            throws MalformedURLException, ServiceException {
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new javax.xml.rpc.ServiceException("The protocol" + " "
                                                     + protocol + " is not supported by this service.");
        }
        org.apache.cxf.jaxws.JaxWsProxyFactoryBean clientFactory = new org.apache.cxf.jaxws.JaxWsProxyFactoryBean();
        clientFactory.setAddress(protocol+"://"+host+":"+port+"/fedora/services/accessMTOM");
      clientFactory.setServiceClass(FedoraAPIAMTOM.class);
      clientFactory.setUsername(username);
      clientFactory.setPassword(password);
      FedoraAPIAMTOM service = (FedoraAPIAMTOM)clientFactory.create();

      if (Administrator.INSTANCE == null) {
          // if running without Administrator, don't wrap it with the statusbar stuff
          return service;
      } else {
          return new APIAStubWrapper(service);
      }
    }

//    /**
//     * Method to rewrite the default API-A base URL (specified in the service
//     * locator class FedoraAPIAServiceLocator). In this case we allow the path
//     * of the service URL to be replaced. A SOAP stub will be returned with the
//     * desired service endpoint URL.
//     *
//     * @param protocol
//     * @param host
//     * @param port
//     * @param path
//     * @param username
//     * @param password
//     * @return FedoraAPIA SOAP stub
//     * @throws MalformedURLException
//     * @throws ServiceException
//     */
//    public static FedoraAPIA getStubAltPath(String protocol,
//                                            String host,
//                                            int port,
//                                            String path,
//                                            String username,
//                                            String password)
//            throws MalformedURLException, ServiceException {
//
//        FedoraAPIAServiceLocator locator =
//                new FedoraAPIAServiceLocator(username,
//                                             password,
//                                             SOCKET_TIMEOUT_SECONDS);
//
//        //SDP - HTTPS support added
//        URL ourl = null;
//        URL nurl = null;
//        if (protocol.equalsIgnoreCase("http")) {
//            ourl = new URL(locator.getFedoraAPIAPortSOAPHTTPAddress());
//            nurl = rewriteServiceURL(ourl, protocol, host, port, path);
//            if (Administrator.INSTANCE == null) {
//                // if running without Administrator, don't wrap it with the statusbar stuff
//                return locator.getFedoraAPIAPortSOAPHTTP(nurl);
//            } else {
//                return new APIAStubWrapper(locator
//                        .getFedoraAPIAPortSOAPHTTP(nurl));
//            }
//        } else if (protocol.equalsIgnoreCase("https")) {
//            ourl = new URL(locator.getFedoraAPIAPortSOAPHTTPSAddress());
//            nurl = rewriteServiceURL(ourl, protocol, host, port, path);
//            if (Administrator.INSTANCE == null) {
//                // if running without Administrator, don't wrap it with the statusbar stuff
//                return locator.getFedoraAPIAPortSOAPHTTPS(nurl);
//            } else {
//                return new APIAStubWrapper(locator
//                        .getFedoraAPIAPortSOAPHTTPS(nurl));
//            }
//        } else {
//            throw new javax.xml.rpc.ServiceException("The protocol" + " "
//                    + protocol + " is not supported by this service.");
//        }
//    }
//
//    private static String rewriteServiceURL(URL ourl,
//                                         String protocol,
//                                         String host,
//                                         int port,
//                                         String path)
//            throws MalformedURLException, ServiceException {
//
//        StringBuffer nurl = new StringBuffer();
//        if (protocol.equalsIgnoreCase("http")) {
//            nurl.append("http://");
//        } else if (protocol.equalsIgnoreCase("https")) {
//            nurl.append("https://");
//        } else {
//            throw new javax.xml.rpc.ServiceException("The protocol" + " "
//                    + protocol + " is not supported by this service.");
//        }
//
//        nurl.append(host);
//        nurl.append(':');
//        nurl.append(port);
//
//        // Use the path, query, and fragment from the original URL
//        // Otherwise, if an alternate path is provided, use it to complete the service URL
//        if (path == null || path.equals("")) {
//            nurl.append(ourl.getPath());
//            if (ourl.getQuery() != null && !ourl.getQuery().equals("")) {
//                nurl.append('?');
//                nurl.append(ourl.getQuery());
//            }
//            if (ourl.getRef() != null && !ourl.getRef().equals("")) {
//                nurl.append('#');
//                nurl.append(ourl.getRef());
//            }
//        } else {
//            path = path.trim();
//            if (!path.startsWith("/")) {
//                path = "/" + path;
//            }
//            nurl.append(path);
//        }
//
//        return nurl.toString();
//    }
}
