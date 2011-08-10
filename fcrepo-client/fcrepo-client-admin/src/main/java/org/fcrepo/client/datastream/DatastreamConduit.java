/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client.datastream;

import java.net.MalformedURLException;

import java.rmi.RemoteException;

import java.util.List;

import javax.xml.rpc.ServiceException;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Constants;

import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.types.mtom.gen.ArrayOfString;
import org.fcrepo.server.types.mtom.gen.Datastream;
import org.fcrepo.server.utilities.TypeUtility;


/**
 * @author Chris Wilper
 * @version $Id$
 */
public class DatastreamConduit {

    private final FedoraAPIMMTOM m_apim;

    public DatastreamConduit(FedoraAPIMMTOM apim)
            throws MalformedURLException, ServiceException {
        m_apim = apim;
    }

    public static Datastream getDatastream(FedoraAPIMMTOM skeleton,
                                           String pid,
                                           String dsId,
                                           String asOfDateTime){
        return skeleton.getDatastream(pid, dsId, asOfDateTime);
    }

    public Datastream getDatastream(String pid, String dsId, String asOfDateTime){
        return getDatastream(m_apim, pid, dsId, asOfDateTime);
    }

    public static List<Datastream> getDatastreams(FedoraAPIMMTOM skeleton,
                                              String pid,
                                              String asOfDateTime,
                                              String state){
        return skeleton.getDatastreams(pid, asOfDateTime, state);
    }

    public List<Datastream> getDatastreams(String pid,
                                       String asOfDateTime,
                                       String state) throws RemoteException {
        return getDatastreams(m_apim, pid, asOfDateTime, state);
    }

    public static void modifyDatastreamByReference(FedoraAPIMMTOM skeleton,
                                                   String pid,
                                                   String dsId,
                                                   String[] altIDs,
                                                   String dsLabel,
                                                   String mimeType,
                                                   String formatURI,
                                                   String location,
                                                   String checksumType,
                                                   String checksum,
                                                   String logMessage)
    {
        skeleton.modifyDatastreamByReference(pid,
                                             dsId,
                                             TypeUtility.convertStringtoAOS(altIDs),
                                             dsLabel,
                                             mimeType,
                                             formatURI,
                                             location,
                                             checksumType,
                                             checksum,
                                             logMessage,
                                             false);
    }

    public void modifyDatastreamByReference(String pid,
                                            String dsId,
                                            String[] altIDs,
                                            String dsLabel,
                                            String mimeType,
                                            String formatURI,
                                            String location,
                                            String checksumType,
                                            String checksum,
                                            String logMessage){
        modifyDatastreamByReference(m_apim,
                                    pid,
                                    dsId,
                                    altIDs,
                                    dsLabel,
                                    mimeType,
                                    formatURI,
                                    location,
                                    checksumType,
                                    checksum,
                                    logMessage);
    }

    public static void modifyDatastreamByValue(FedoraAPIMMTOM skeleton,
                                               String pid,
                                               String dsId,
                                               String[] altIDs,
                                               String dsLabel,
                                               String mimeType,
                                               String formatURI,
                                               byte[] content,
                                               String checksumType,
                                               String checksum,
                                               String logMessage){
        skeleton.modifyDatastreamByValue(pid,
                                         dsId,
                                         TypeUtility.convertStringtoAOS(altIDs),
                                         dsLabel,
                                         mimeType,
                                         formatURI,
                                         TypeUtility.convertBytesToDataHandler(content),
                                         checksumType,
                                         checksum,
                                         logMessage,
                                         false);
    }

    public void modifyDatastreamByValue(String pid,
                                        String dsId,
                                        String[] altIDs,
                                        String dsLabel,
                                        boolean versionable,
                                        String mimeType,
                                        String formatURI,
                                        byte[] content,
                                        String state,
                                        String checksumType,
                                        String checksum,
                                        String logMessage) throws RemoteException {
        modifyDatastreamByValue(m_apim,
                                pid,
                                dsId,
                                altIDs,
                                dsLabel,
                                mimeType,
                                formatURI,
                                content,
                                checksumType,
                                checksum,
                                logMessage);
    }

    public static List<String> purgeDatastream(FedoraAPIMMTOM skeleton,
                                           String pid,
                                           String dsId,
                                           String startDT,
                                           String endDT,
                                           String logMessage){
        return skeleton.purgeDatastream(pid,
                                        dsId,
                                        startDT,
                                        endDT,
                                        logMessage,
                                        false);
    }

    public List<String> purgeDatastream(String pid,
                                    String dsId,
                                    String startDT,
                                    String endDT,
                                    String logMessage) throws RemoteException {
        return purgeDatastream(m_apim,
                               pid,
                               dsId,
                               startDT,
                               endDT,
                               logMessage);
    }

    public static List<Datastream> getDatastreamHistory(FedoraAPIMMTOM skeleton,
                                                    String pid,
                                                    String dsId){
        return skeleton.getDatastreamHistory(pid, dsId);
    }

    public List<Datastream> getDatastreamHistory(String pid, String dsId){
        return getDatastreamHistory(m_apim, pid, dsId);
    }

    public static void showUsage(String errMessage) {
        System.out.println("Error: " + errMessage);
        System.out.println("");
        System.out
                .println("Usage: fedora-dsinfo host port user password pid protocol [context]");
        System.out.println("Note: protocol must be either http or https.");
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        try {
            if (args.length < 6 || args.length > 7) {
                DatastreamConduit
                        .showUsage("You must provide six or seven arguments.");
            } else {
                String hostName = args[0];
                int portNum = Integer.parseInt(args[1]);
                String username = args[2];
                String password = args[3];
                String pid = args[4];
                String protocol = args[5];

                String context = Constants.FEDORA_DEFAULT_APP_CONTEXT;
                if (args.length == 7 && !args[6].equals("")) {
                    context = args[6];
                }
                // ******************************************
                // NEW: use new client utility class
                String baseURL =
                        protocol + "://" + hostName + ":" + portNum + "/"
                        + context;
                FedoraClient fc = new FedoraClient(baseURL, username, password);
                FedoraAPIMMTOM sourceRepoAPIM = fc.getAPIM();
                //*******************************************
                DatastreamConduit c = new DatastreamConduit(sourceRepoAPIM);

                List<Datastream> datastreams = c.getDatastreams(pid, null, null);
                for (Datastream ds : datastreams) {
                    System.out.println("   Datastream : " + ds.getID());
                    System.out.println("Control Group : "
                                       + ds.getControlGroup().toString());
                    System.out.println("  Versionable : " + ds.isVersionable());
                    System.out.println("    Mime Type : " + ds.getMIMEType());
                    System.out.println("   Format URI : " + ds.getFormatURI());
                    ArrayOfString altIDs = ds.getAltIDs();
                    if (altIDs != null && altIDs.getItem() != null) {
                        for (String element : altIDs.getItem()) {
                            System.out.println(" Alternate ID : " + element);
                        }
                    }
                    System.out.println("        State : " + ds.getState());
                    // print version id, create date, and label for each version
                    List<Datastream> versions =
                            c.getDatastreamHistory(pid, ds.getID());
                    for (Datastream ver : versions) {
                        System.out.println("      VERSION : "
                                           + ver.getVersionID());
                        System.out.println("        Created : "
                                           + ver.getCreateDate());
                        System.out.println("          Label : "
                                           + ver.getLabel());
                        System.out.println("       Location : "
                                           + ver.getLocation());
                    }
                    System.out.println("");
                }
            }
        } catch (Exception e) {
            DatastreamConduit.showUsage(e.getClass().getName()
                                        + " - "
                                        + (e.getMessage() == null ? "(no detail provided)" : e
                    .getMessage()));
        }
    }
}
