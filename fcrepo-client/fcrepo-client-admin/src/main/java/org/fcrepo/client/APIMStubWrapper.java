
package org.fcrepo.client;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import org.fcrepo.server.types.gen.ArrayOfString;
import org.fcrepo.server.types.gen.Datastream;
import org.fcrepo.server.types.gen.RelationshipTuple;
import org.fcrepo.server.types.gen.Validation;

public class APIMStubWrapper
        implements org.fcrepo.server.management.FedoraAPIM {

    /** The wrapped instance */
    private final org.fcrepo.server.management.FedoraAPIM m_instance;

    public APIMStubWrapper(org.fcrepo.server.management.FedoraAPIM instance) {
        m_instance = instance;
    }

    @Override
    public String ingest(final byte[] objectXML,
                                   final String format,
                                   final String logMessage){
        String buf = "Ingest";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance.ingest(objectXML,
                                             format,
                                             logMessage);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String modifyObject(final String pid,
            final String state,
            final String label,
            final String ownerId,
            final String logMessage){
        String buf = "Modify object";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance.modifyObject(pid,
                                                   state,
                                                   label,
                                                   ownerId,
                                                   logMessage);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public byte[] getObjectXML(final String pid){
        String buf = "Get object XML";
        // Run the method in a SwingWorker thread
        SwingWorker<byte[]> worker = new SwingWorker<byte[]>() {

            @Override
            public byte[] construct() {
                    // call wrapped method
                    return m_instance.getObjectXML(pid);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public byte[] export(final String pid,
                         final String format,
                         final String context){
        String buf = "Export";
        // Run the method in a SwingWorker thread
        SwingWorker<byte[]> worker = new SwingWorker<byte[]>() {

            @Override
            public byte[] construct() {
                    // call wrapped method
                    return m_instance.export(pid,
                                             format,
                                             context);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String purgeObject(final String pid,
                                        final String logMessage,
                                        final boolean force){
        String buf = "Purge object";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance.purgeObject(pid, logMessage, force);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String addDatastream(final String pid,
                                final String dsID,
                                final ArrayOfString altIDs,
                                final String dsLabel,
                                final boolean versionable,
                                final String MIMEType,
                                final String formatURI,
                                final String dsLocation,
                                final String controlGroup,
                                final String dsState,
                                final String checksumType,
                                final String checksum,
                                final String logMessage){
        String buf = "Add datastream";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance
                            .addDatastream(pid,
                                           dsID,
                                           altIDs,
                                           dsLabel,
                                           versionable,
                                           MIMEType,
                                           formatURI,
                                           dsLocation,
                                           controlGroup,
                                           dsState,
                                           checksumType,
                                           checksum,
                                           logMessage);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String modifyDatastreamByReference(final String pid,
                                              final String dsID,
                                              final ArrayOfString altIDs,
                                              final String dsLabel,
                                              final String MIMEType,
                                              final String formatURI,
                                              final String dsLocation,
                                              final String checksumType,
                                              final String checksum,
                                              final String logMessage,
                                              final boolean force){
        String buf = "Modify datastream by reference";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance
                            .modifyDatastreamByReference(pid,
                                                         dsID,
                                                         altIDs,
                                                         dsLabel,
                                                         MIMEType,
                                                         formatURI,
                                                         dsLocation,
                                                         checksumType,
                                                         checksum,
                                                         logMessage,
                                                         force);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String modifyDatastreamByValue(
            final String pid,
            final String dsID,
            final ArrayOfString altIDs,
            final String dsLabel,
            final String MIMEType,
            final String formatURI,
            final byte[] dsContent,
            final String checksumType,
            final String checksum,
            final String logMessage,
            final boolean force){
        String buf = "Modify datastream by value";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance
                            .modifyDatastreamByValue(pid,
                                                     dsID,
                                                     altIDs,
                                                     dsLabel,
                                                     MIMEType,
                                                     formatURI,
                                                     dsContent,
                                                     checksumType,
                                                     checksum,
                                                     logMessage,
                                                     force);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String setDatastreamState(final String pid,
                                     final String dsID,
                                     final String dsState,
                                     final String logMessage){
        String buf = "Set datastream state";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    return m_instance
                            .setDatastreamState(pid, dsID, dsState, logMessage);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String setDatastreamVersionable(final String pid,
                                           final String dsID,
                                           final boolean versionable,
                                           final String logMessage){
        String buf = "Set datastream versionable";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance
                            .setDatastreamVersionable(pid,
                                                      dsID,
                                                      versionable,
                                                      logMessage);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public String compareDatastreamChecksum(final String pid,
                                            final String dsID,
                                            final String versionDate){
        String buf = "Compare datastream checksum";
        // Run the method in a SwingWorker thread
        SwingWorker<String> worker = new SwingWorker<String>() {

            @Override
            public String construct() {
                    // call wrapped method
                    return m_instance
                            .compareDatastreamChecksum(pid,
                                    dsID, versionDate);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public Datastream getDatastream(
            final String pid,
            final String dsID,
            final String asOfDateTime){
        String buf = "Get datastream";
        // Run the method in a SwingWorker thread
        SwingWorker<Datastream> worker = new SwingWorker<Datastream>() {

            @Override
            public Datastream construct() {
                    // call wrapped method
                    return m_instance
                            .getDatastream(pid, dsID, asOfDateTime);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<Datastream> getDatastreams(java.lang.String pid,
            java.lang.String asOfDateTime,
            java.lang.String dsState){
        String buf = "Get datastreams";
        HashMap<String,String> PARMS = new HashMap<String,String>(3);
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        PARMS.put("dsState", dsState);
        // Run the method in a SwingWorker thread
        SwingWorker<List<Datastream>> worker =
                new SwingWorker<List<Datastream>>(PARMS) {

            @Override
            public List<Datastream> construct() {
                    // call wrapped method
                    return m_instance.getDatastreams((java.lang.String) parms
                            .get("pid"), (java.lang.String) parms
                            .get("asOfDateTime"), (java.lang.String) parms
                            .get("dsState"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<Datastream> getDatastreamHistory(final String pid,
                                                 final String dsID){
        String buf = "Get datastream history";
        // Run the method in a SwingWorker thread
        SwingWorker<List<Datastream>> worker =
                new SwingWorker<List<Datastream>>() {

            @Override
            public List<Datastream> construct() {
                    // call wrapped method
                    return m_instance
                            .getDatastreamHistory(pid, dsID);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<String> purgeDatastream(final String pid,
                                        final String dsID,
                                        final String startDT,
                                        final String endDT,
                                        final String logMessage,
                                        final boolean force){
        String buf = "Purge datastream";
        // Run the method in a SwingWorker thread
        SwingWorker<List<String>> worker =
                new SwingWorker<List<String>>() {

            @Override
            public List<String> construct() {
                    // call wrapped method
                    return m_instance
                            .purgeDatastream(pid, dsID, startDT, endDT,
                                    logMessage, force);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<String> getNextPID(final BigInteger numPIDs,
                                   final java.lang.String pidNamespace){
        String buf = "Get next PID";
        // Run the method in a SwingWorker thread
        SwingWorker<List<String>> worker =
                new SwingWorker<List<String>>() {

            @Override
            public List<String> construct() {
                    // call wrapped method
                    return m_instance
                            .getNextPID(numPIDs, pidNamespace);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public List<RelationshipTuple> getRelationships(final String pid,
                                                    final String relationship){
        String buf = "Get relationships";
        // Run the method in a SwingWorker thread
        SwingWorker<List<RelationshipTuple>> worker =
                new SwingWorker<List<RelationshipTuple>>() {

            @Override
            public List<RelationshipTuple> construct() {
                    // call wrapped method
                    return m_instance.getRelationships(pid, relationship);
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public boolean addRelationship(java.lang.String pid,
                                   java.lang.String relationship,
                                   java.lang.String object,
                                   boolean isLiteral,
                                   java.lang.String datatype){
        String buf = "Add relationship";
        HashMap<String,Object> PARMS = new HashMap<String,Object>();
        PARMS.put("pid", pid);
        PARMS.put("relationship", relationship);
        PARMS.put("object", object);
        PARMS.put("isLiteral", new Boolean(isLiteral));
        PARMS.put("datatype", datatype);
        // Run the method in a SwingWorker thread
        SwingWorker<Boolean> worker = new SwingWorker<Boolean>(PARMS) {

            @Override
            public Boolean construct() {
                    // call wrapped method
                    return m_instance.addRelationship((java.lang.String) parms
                            .get("pid"), (java.lang.String) parms
                            .get("relationship"), (java.lang.String) parms
                            .get("object"), ((Boolean) parms.get("isLiteral"))
                            .booleanValue(), (java.lang.String) parms
                            .get("datatype"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public boolean purgeRelationship(java.lang.String pid,
                                     java.lang.String relationship,
                                     java.lang.String object,
                                     boolean isLiteral,
                                     java.lang.String datatype){
        String buf = "Purge relationship";
        HashMap<String,Object> PARMS = new HashMap<String, Object>();
        PARMS.put("pid", pid);
        PARMS.put("relationship", relationship);
        PARMS.put("object", object);
        PARMS.put("isLiteral", new Boolean(isLiteral));
        PARMS.put("datatype", datatype);
        // Run the method in a SwingWorker thread
        SwingWorker<Boolean> worker = new SwingWorker<Boolean>(PARMS) {

            @Override
            public Boolean construct() {
                    // call wrapped method
                    return m_instance
                            .purgeRelationship((java.lang.String) parms
                                                       .get("pid"),
                                               (java.lang.String) parms
                                                       .get("relationship"),
                                               (java.lang.String) parms
                                                       .get("object"),
                                               ((Boolean) parms
                                                       .get("isLiteral"))
                                                       .booleanValue(),
                                               (java.lang.String) parms
                                                       .get("datatype"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }

    @Override
    public Validation validate(java.lang.String pid, java.lang.String asOfDateTime){
        String buf = "Validate";
        HashMap<String,String> PARMS = new HashMap<String,String>(2);
        PARMS.put("pid", pid);
        PARMS.put("asOfDateTime", asOfDateTime);
        // Run the method in a SwingWorker thread
        SwingWorker<Validation> worker = new SwingWorker<Validation>(PARMS) {

            @Override
            public Validation construct() {
                    // call wrapped method
                    return m_instance.validate((java.lang.String) parms
                            .get("pid"), (java.lang.String) parms
                            .get("asOfDateTime"));
            }
        };
        return SwingWorker.waitForResult(worker, buf);
    }
}
