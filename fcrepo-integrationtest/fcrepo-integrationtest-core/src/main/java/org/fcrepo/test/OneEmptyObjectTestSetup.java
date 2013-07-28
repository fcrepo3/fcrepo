/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.test;

import org.fcrepo.client.FedoraClient;
import org.fcrepo.common.Constants;

import org.fcrepo.server.utilities.TypeUtility;


public abstract class OneEmptyObjectTestSetup
        implements Constants {

    private static byte[] getTestObjectBytes(String pid) throws Exception {
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\""
                   + XSI.uri + "\"\n");

        xml.append("           xmlns:foxml=\"" + FOXML.uri + "\"\n");
        xml.append("           xsi:schemaLocation=\"" + FOXML.uri + " "
                   + FOXML1_1.xsdLocation + "\"\n");
        xml.append("\n           PID=\"" + pid + "\">\n");
        xml.append("  <foxml:objectProperties>\n");
        xml.append("    <foxml:property NAME=\"" + MODEL.LABEL.uri
                   + "\" VALUE=\"label\"/>\n");
        xml.append("  </foxml:objectProperties>\n");
        xml.append("</foxml:digitalObject>");
        return xml.toString().getBytes("UTF-8");
    }

    public static void ingestOneEmptyObject(FedoraClient client, String pid)
        throws Exception {
        System.out.println("Ingesting test object: " + pid);
        client.getAPIMMTOM()
            .ingest(TypeUtility.convertBytesToDataHandler(
                    getTestObjectBytes(pid)), FOXML1_1.uri, "");
    }

    public static void purgeOneEmptyObject(FedoraClient client, String pid)
        throws Exception {
        System.out.println("Purging test object: " + pid);
        client.getAPIMMTOM().purgeObject(pid, "", false);
    }

}
