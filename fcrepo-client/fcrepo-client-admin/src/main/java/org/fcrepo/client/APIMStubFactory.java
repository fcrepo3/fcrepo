/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.fcrepo.server.management.FedoraAPIMMTOM;

/**
 * @author Chris Wilper
 */
public abstract class APIMStubFactory {

    public static int SOCKET_TIMEOUT_SECONDS = 120;

    /**
     * Method to rewrite the default API-M base URL (specified in the service
     * locator class FedoraAPIMServiceLocator). In this case we allow the
     * protocol, host, and port parts of the service URL to be replaced. A SOAP
     * stub will be returned with the desired service endpoint URL.
     *
     * @param protocol
     * @param host
     * @param port
     * @param username
     * @param password
     * @return FedoraAPIM SOAP stub
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public static FedoraAPIMMTOM getStub(String protocol,
                                         String host,
                                         int port,
                                         String username,
                                         String password)
            throws MalformedURLException, ServiceException {
        if (!"http".equalsIgnoreCase(protocol)
                && !"https".equalsIgnoreCase(protocol)) {
            throw new javax.xml.rpc.ServiceException("The protocol" + " "
                    + protocol + " is not supported by this service.");
        }

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("mtom-enabled", Boolean.TRUE);

        org.apache.cxf.jaxws.JaxWsProxyFactoryBean clientFactory =
                new org.apache.cxf.jaxws.JaxWsProxyFactoryBean();
        clientFactory.setAddress(protocol + "://" + host + ":" + port
                + "/fedora/services/managementMTOM");
        clientFactory.setServiceClass(FedoraAPIMMTOM.class);
        clientFactory.setUsername(username);
        clientFactory.setPassword(password);
        clientFactory.setProperties(props);
        //      LoggingInInterceptor log1 = new LoggingInInterceptor(new PrintWriter(System.out));
        //      LoggingOutInterceptor log2 = new LoggingOutInterceptor(new PrintWriter(System.out));
        //      clientFactory.getInInterceptors().add(log1);
        //      clientFactory.getInInterceptors().add(log2);

        // temporarily turn off err stream, because initialization of service spams the stream
        PrintStream aux = System.err;
        System.setErr(new PrintStream(new OutputStream() {

            @Override
            public void write(int arg0) throws IOException {
            }
        }));
        FedoraAPIMMTOM service = (FedoraAPIMMTOM) clientFactory.create();
        System.setErr(aux);

        if (Administrator.INSTANCE == null) {
            // if running without Administrator, don't wrap it with the statusbar stuff
            return service;
        } else {
            return new APIMStubWrapper(service);
        }
    }

    //    /**
    //     * Method to rewrite the default API-M base URL (specified in the service
    //     * locator class FedoraAPIMServiceLocator). In this case we allow the
    //     * protocol, host, port, and PATH parts of the service URL to be replaced. A
    //     * SOAP stub will be returned with the desired service endpoint URL.
    //     *
    //     * @param protocol
    //     * @param host
    //     * @param port
    //     * @param path
    //     * @param username
    //     * @param password
    //     * @return FedoraAPIM SOAP stub
    //     * @throws MalformedURLException
    //     * @throws ServiceException
    //     */
    //    public static FedoraAPIM getStubAltPath(String protocol,
    //                                            String host,
    //                                            int port,
    //                                            String path,
    //                                            String username,
    //                                            String password)
    //            throws MalformedURLException, ServiceException {
    //
    //        FedoraAPIMServiceLocator locator =
    //                new FedoraAPIMServiceLocator(username,
    //                                             password,
    //                                             SOCKET_TIMEOUT_SECONDS);
    //
    //        //SDP - HTTPS support added
    //        URL ourl = null;
    //        URL nurl = null;
    //        if (protocol.equalsIgnoreCase("http")) {
    //            ourl = new URL(locator.getFedoraAPIMPortSOAPHTTPAddress());
    //            nurl = rewriteServiceURL(ourl, protocol, host, port, path);
    //            if (Administrator.INSTANCE == null) {
    //                // if running without Administrator, don't wrap it with the statusbar stuff
    //                return locator.getFedoraAPIMPortSOAPHTTP(nurl);
    //            } else {
    //                return new APIMStubWrapper(locator
    //                        .getFedoraAPIMPortSOAPHTTP(nurl));
    //            }
    //        } else if (protocol.equalsIgnoreCase("https")) {
    //            ourl = new URL(locator.getFedoraAPIMPortSOAPHTTPSAddress());
    //            nurl = rewriteServiceURL(ourl, protocol, host, port, path);
    //            if (Administrator.INSTANCE == null) {
    //                // if running without Administrator, don't wrap it with the statusbar stuff
    //                return locator.getFedoraAPIMPortSOAPHTTPS(nurl);
    //            } else {
    //                return new APIMStubWrapper(locator
    //                        .getFedoraAPIMPortSOAPHTTPS(nurl));
    //            }
    //        } else {
    //            throw new javax.xml.rpc.ServiceException("The protocol" + " "
    //                    + protocol + " is not supported by this service.");
    //        }
    //    }
    //
    //    private static URL rewriteServiceURL(URL ourl,
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
    //        return new URL(nurl.toString());
    //    }

}
