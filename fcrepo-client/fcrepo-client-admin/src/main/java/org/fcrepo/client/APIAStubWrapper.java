
package org.fcrepo.client;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fcrepo.server.types.gen.DatastreamDef;
import org.fcrepo.server.types.gen.FieldSearchResult;
import org.fcrepo.server.types.gen.MIMETypedStream;
import org.fcrepo.server.types.gen.ObjectMethodsDef;
import org.fcrepo.server.types.gen.ObjectProfile;
import org.fcrepo.server.types.gen.RepositoryInfo;

public class APIAStubWrapper
        implements org.fcrepo.server.access.FedoraAPIA {

    /** The wrapped instance */
    private final org.fcrepo.server.access.FedoraAPIA m_instance;

    public APIAStubWrapper(org.fcrepo.server.access.FedoraAPIA instance) {
        m_instance = instance;
    }

    @Override
    public RepositoryInfo describeRepository() {
        String buf = "Describe repository";
        Map<String, String> PARMS = Collections.emptyMap();
        // Run the method in a SwingWorker thread
        SwingWorker<RepositoryInfo> worker =
                new SwingWorker<RepositoryInfo>(PARMS) {

            @Override
            public RepositoryInfo construct() {

                // call wrapped method
                return m_instance.describeRepository();

            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public ObjectProfile getObjectProfile(java.lang.String pid,
                                          java.lang.String asOfDateTime) {
        String buf = "Get object profile";
        HashMap<String, String> PARMS = new HashMap<String, String>(2);
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker<ObjectProfile> worker =
                new SwingWorker<ObjectProfile>(PARMS) {

            @Override
            public ObjectProfile construct() {

                // call wrapped method
                return m_instance.getObjectProfile((java.lang.String) parms
                        .get("pid"), (java.lang.String) parms
                        .get("asOfDateTime"));

            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<ObjectMethodsDef> listMethods(java.lang.String pid,
                                              java.lang.String asOfDateTime) {
        String buf = "List methods";
        HashMap<String, String> PARMS = new HashMap<String, String>(2);
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker<List<ObjectMethodsDef>> worker =
                new SwingWorker<List<ObjectMethodsDef>>(PARMS) {

            @Override
            public List<ObjectMethodsDef> construct() {
                // call wrapped method
                return m_instance.listMethods((java.lang.String) parms
                        .get("pid"), (java.lang.String) parms
                        .get("asOfDateTime"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<DatastreamDef> listDatastreams(java.lang.String pid,
                                               java.lang.String asOfDateTime) {
        String buf = "List datastreams";
        HashMap<String, String> PARMS = new HashMap<String, String>(2);
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker<List<DatastreamDef>> worker =
                new SwingWorker<List<DatastreamDef>>(PARMS) {

            @Override
            public List<DatastreamDef> construct() {
                // call wrapped method
                return m_instance.listDatastreams((java.lang.String) parms
                        .get("pid"), (java.lang.String) parms
                        .get("asOfDateTime"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public MIMETypedStream getDatastreamDissemination(java.lang.String pid,
                                                      java.lang.String dsID,
                                                      java.lang.String asOfDateTime) {
        String buf = "Get datastream dissemination";
        HashMap<String, String> PARMS = new HashMap<String,String>(3);
        PARMS.put("pid", pid);
        PARMS.put("dsID", dsID);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker<MIMETypedStream> worker =
                new SwingWorker<MIMETypedStream>(PARMS) {

            @Override
            public MIMETypedStream construct() {
                // call wrapped method
                return m_instance
                        .getDatastreamDissemination((java.lang.String) parms
                                .get("pid"), (java.lang.String) parms
                                .get("dsID"), (java.lang.String) parms
                                .get("asOfDateTime"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public MIMETypedStream getDissemination(java.lang.String pid,
                                            java.lang.String serviceDefinitionPid,
                                            java.lang.String methodName,
                                            org.fcrepo.server.types.gen.GetDissemination.Parameters parameters,
                                            java.lang.String asOfDateTime) {
        String buf = "Get dissemination";
        HashMap<String, Object> PARMS = new HashMap<String, Object>();
        PARMS.put("pid", pid);
        PARMS.put("serviceDefinitionPid", serviceDefinitionPid);
        PARMS.put("methodName", methodName);
        PARMS.put("parameters", parameters);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker<MIMETypedStream> worker =
                new SwingWorker<MIMETypedStream>(PARMS) {

            @Override
            public MIMETypedStream construct() {
                // call wrapped method
                return m_instance
                        .getDissemination((java.lang.String) parms.get("pid"),
                                          (java.lang.String) parms
                                                  .get("serviceDefinitionPid"),
                                          (java.lang.String) parms
                                                  .get("methodName"),
                                          (org.fcrepo.server.types.gen.GetDissemination.Parameters) parms
                                                  .get("parameters"),
                                          (java.lang.String) parms
                                                  .get("asOfDateTime"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public FieldSearchResult findObjects(org.fcrepo.server.types.gen.ArrayOfString resultFields,
                                         BigInteger maxResults,
                                         org.fcrepo.server.types.gen.FieldSearchQuery query) {
        String buf = "Find objects";
        HashMap<String,Object> PARMS = new HashMap<String, Object>(3);
        PARMS.put("resultFields", resultFields);
        PARMS.put("maxResults", maxResults);
        PARMS.put("query", query);
        // Run the method in a SwingWorker thread
        SwingWorker<FieldSearchResult> worker =
                new SwingWorker<FieldSearchResult>(PARMS) {

            @Override
            public FieldSearchResult construct() {
                // call wrapped method
                return m_instance
                        .findObjects((org.fcrepo.server.types.gen.ArrayOfString) parms
                                             .get("resultFields"),
                                     (BigInteger) parms.get("maxResults"),
                                     (org.fcrepo.server.types.gen.FieldSearchQuery) parms
                                             .get("query"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public FieldSearchResult resumeFindObjects(java.lang.String sessionToken) {
        String buf = "Resume find objects";
        Map<String, String> PARMS =
                Collections.singletonMap("sessionToken", sessionToken);
        // Run the method in a SwingWorker thread
        SwingWorker<FieldSearchResult> worker =
                new SwingWorker<FieldSearchResult>(PARMS) {

            @Override
            public FieldSearchResult construct() {
                // call wrapped method
                return m_instance.resumeFindObjects((java.lang.String) parms
                        .get("sessionToken"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<java.lang.String> getObjectHistory(java.lang.String pid) {
        String buf = "Get object history";
        Map<String, String> PARMS = Collections.singletonMap("pid", pid);
        // Run the method in a SwingWorker thread
        SwingWorker<List<java.lang.String>> worker =
                new SwingWorker<List<java.lang.String>>(PARMS) {

            @Override
            public List<java.lang.String> construct() {
                // call wrapped method
                return m_instance.getObjectHistory((java.lang.String) parms
                        .get("pid"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }
    
}