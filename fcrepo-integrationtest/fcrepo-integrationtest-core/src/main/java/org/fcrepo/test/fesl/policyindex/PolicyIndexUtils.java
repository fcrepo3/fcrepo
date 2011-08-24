package org.fcrepo.test.fesl.policyindex;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.rmi.RemoteException;

import org.fcrepo.common.Constants;

import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.utilities.StreamUtility;
import org.fcrepo.server.utilities.TypeUtility;


public class PolicyIndexUtils implements Constants {

    private FedoraAPIMMTOM m_apim = null;

    public PolicyIndexUtils(FedoraAPIMMTOM apim) {
        m_apim = apim;
    }

    public String addPolicyObject(String policy, String objectState, String datastreamState) throws IOException {
        // nb, must be in demo: namespace for tearDown to purge
        String pid = getNextPids(1)[0];

        byte[] object = getPolicyObject(policy, objectState, datastreamState, pid);

        m_apim.ingest(TypeUtility.convertBytesToDataHandler(object), FOXML1_1.uri,
                    "ingesting new foxml object");

        return pid;
    }

    public String[] getNextPids(int pidCount) throws RemoteException {

        return m_apim.getNextPID(new java.math.BigInteger(Integer.toString(pidCount)), "demo").toArray(new String[0]);

    }


    // synchronized to prevent concurrent reads on same file
    public static synchronized String getPolicy(String policy) throws IOException {

        StringBuilder sb = new StringBuilder();

        String base =
            System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                    .getProperty("fcrepo-integrationtest-core.classes")
                    : "src/test/resources/";

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(base + "/test-objects/xacml/test-policy-" + policy + ".xml"),"UTF-8"));
        String ln;
        while ((ln = br.readLine()) != null)
            sb.append(ln + "\n");
        return sb.toString();


    }

    public static byte[] getPolicyObject(String policy, String objectState, String datastreamState, String pid) throws IOException {
        StringBuilder foxml = new StringBuilder();
        if (datastreamState != null)
            if (!"AID".contains(datastreamState))
                throw new RuntimeException("Invalid datastreamState parameter " + datastreamState);
        if (!"AID".contains(objectState))
            throw new RuntimeException("Invalid datastreamState parameter " + datastreamState);

        foxml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        foxml.append("<foxml:digitalObject VERSION=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        foxml.append("    xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"\n");
        foxml.append("           xsi:schemaLocation=\"" + Constants.FOXML.uri
                     + " " + Constants.FOXML1_1.xsdLocation + "\"");
        foxml.append("\n           PID=\"" + StreamUtility.enc(pid)
                         + "\">\n");
        foxml.append("  <foxml:objectProperties>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"" + objectState + "\"/>\n");
        foxml.append("    <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""
                + StreamUtility.enc("test policy object") + "\"/>\n");
        foxml.append("  </foxml:objectProperties>\n");

        // policy datastream, unless null/empty string specified
        if (datastreamState != null) {
            foxml.append("<foxml:datastream ID=\"" + FedoraPolicyStore.FESL_POLICY_DATASTREAM + "\" STATE=\"" + datastreamState
                         + "\" CONTROL_GROUP=\"X\">");
            foxml.append("<foxml:datastreamVersion ID=\"POLICY.0\" MIMETYPE=\"text/xml\" LABEL=\"XACML policy datastream\">");

            foxml.append("  <foxml:xmlContent>");

            // the policy
            foxml.append(getPolicy(policy));

            foxml.append("    </foxml:xmlContent>");

            foxml.append("  </foxml:datastreamVersion>");
            foxml.append("</foxml:datastream>");
        }


        foxml.append("</foxml:digitalObject>");

        return foxml.toString().getBytes("UTF-8");

    }
    }

