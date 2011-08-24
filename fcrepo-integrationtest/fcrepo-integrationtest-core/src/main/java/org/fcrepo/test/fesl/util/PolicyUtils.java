
package org.fcrepo.test.fesl.util;

import java.io.File;
import java.io.IOException;

import javax.xml.rpc.ServiceException;

import org.w3c.dom.Document;

import org.fcrepo.client.FedoraClient;

import org.fcrepo.common.Constants;

import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.utilities.TypeUtility;

/**
 * Utilities for managing FeSL policies Used to add and delete policies
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class PolicyUtils
        implements Constants {

    FedoraAPIMMTOM apim = null;

    private static final String RESOURCEBASE =
            System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                    .getProperty("fcrepo-integrationtest-core.classes")
                    + "test-objects"
                    : "src/test/resources/test-objects";

    @SuppressWarnings("unused")
    private PolicyUtils() {
    }

    public PolicyUtils(FedoraClient fedoraClient)
            throws ServiceException, IOException {
        apim = fedoraClient.getAPIM();

    }

    public String addPolicy(File policyFile) throws Exception {
        byte[] policy = DataUtils.loadFile(policyFile.getAbsolutePath());

        String policyId = getPolicyId(policy);

        String policyFileName = "file:///" + policyFile.getAbsolutePath();

        // escape any pid namespace character
        if (policyId.contains(":")) {
            policyId = policyId.replace(":", "%3A");
        }

        String pid = "demo:" + policyId;

        StringBuilder foxml = new StringBuilder();

        // basic empty object

        foxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        foxml
                .append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        foxml
                .append("    xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        foxml.append("           xsi:schemaLocation=\"" + Constants.FOXML.uri
                + " " + Constants.FOXML1_1.xsdLocation + "\"");
        foxml.append("\n           PID=\"" + StreamUtility.enc(pid) + "\">\n");
        foxml.append("  <foxml:objectProperties>\n");
        foxml
                .append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>\n");
        foxml
                .append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""
                        + StreamUtility.enc("test policy object") + "\"/>\n");
        foxml.append("  </foxml:objectProperties>\n");

        foxml.append("<foxml:datastream ID=\""
                + FedoraPolicyStore.FESL_POLICY_DATASTREAM
                + "\" CONTROL_GROUP=\"M\">");
        foxml
                .append("<foxml:datastreamVersion ID=\"POLICY.0\" MIMETYPE=\"text/xml\" LABEL=\"XACML policy datastream\">");

        foxml.append("  <foxml:contentLocation REF=\"" + policyFileName
                + "\" TYPE=\"URL\"/>");

        //foxml.append("  <foxml:xmlContent>");
        //foxml.append(policy);
        //foxml.append("    </foxml:xmlContent>");

        foxml.append("  </foxml:datastreamVersion>");
        foxml.append("</foxml:datastream>");

        foxml.append("</foxml:digitalObject>");

        apim.ingest(TypeUtility.convertBytesToDataHandler(foxml.toString().getBytes("UTF-8")),
                    FOXML1_1.uri,
                    "ingesting new foxml object");

        return policyId;

    }

    public String addPolicy(String policyName) throws Exception {
        return addPolicy(new File(RESOURCEBASE + "/xacml/" + policyName));

    }

    public void delPolicy(String policyId) throws Exception {
        String pid = "demo:" + policyId;
        apim.purgeObject(pid, "removing test policy object", false);

    }

    private static String getPolicyId(byte[] data) throws Exception {
        Document doc = DataUtils.getDocumentFromBytes(data);
        String pid = doc.getDocumentElement().getAttribute("PolicyId");

        return pid;
    }

}
