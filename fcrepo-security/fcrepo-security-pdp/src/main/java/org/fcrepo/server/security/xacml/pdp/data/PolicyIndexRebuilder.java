package org.fcrepo.server.security.xacml.pdp.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.utilities.rebuild.Rebuilder;


public class PolicyIndexRebuilder
        implements Rebuilder {


    protected PolicyIndex m_policyIndex = null;

    @Override
    public void addObject(DigitalObject object) throws Exception {
        // does it have a policy datastream?

        Iterable<Datastream> policyDatastreams = object.datastreams(FedoraPolicyStore.POLICY_DATASTREAM);

        // try to get the latest policy datastream version
        Date latest = null;
        Datastream policyDatastream = null;
        for (Datastream ds : policyDatastreams) {
            if (latest == null || ds.DSCreateDT.after(latest)) {
                latest = ds.DSCreateDT;
                policyDatastream = ds;
            }
        }
        // null means no policy datastream found
        if (policyDatastream != null ) {
            // add to cache
            System.out.println("   Adding " + object.getPid() + " to index.");
            String policy = new String(IOUtils.toByteArray(policyDatastream.getContentStream()), "UTF-8");
            // TODO: PolicyIndex would benefit from methods that can accept streams
            m_policyIndex.addPolicy(object.getPid(), policy);
        }



    }

    @Override
    public void finish() throws Exception {

    }

    @Override
    public String getAction() {
        return "Rebuild the FeSL policy cache";
    }

    @Override
    public boolean shouldStopServer() {
        return true;
    }

    @Override
    public void start(Map<String, String> options) throws Exception {

        // attempt to clear index; request manual deletion if this fails
        if (!m_policyIndex.clear()) {
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));

            System.out.println();
            System.out
                    .println("NOTE: You must now manually delete (clear) ");
            System.out
                    .println("      the existing database.  This rebuilder was");
            System.out
                    .println("      unable to perform this step. ");
            System.out
                    .println("      Press enter when finished.");
            try {
                reader.readLine();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public Map<String, String> getOptions() throws Exception {
        return null;
    }

    @Override
    public void init() {
        PolicyIndexFactory factory = new PolicyIndexFactory();
        try {
            m_policyIndex = factory.newPolicyIndex();
        } catch (PolicyIndexException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setServerConfiguration(ServerConfiguration serverConfig) {
        // not needed
    }

    @Override
    public void setServerDir(File serverBaseDir) {
        // not needed
    }

}
