
package org.fcrepo.client.mtom;

import java.math.BigInteger;
import java.util.List;

import javax.activation.DataHandler;

import org.fcrepo.client.SwingWorker;
import org.fcrepo.server.types.gen.ArrayOfString;
import org.fcrepo.server.types.gen.Datastream;
import org.fcrepo.server.types.gen.RelationshipTuple;
import org.fcrepo.server.types.gen.Validation;

public class APIMStubWrapper
        implements org.fcrepo.server.management.FedoraAPIMMTOM {

    /** The wrapped instance */
    private final org.fcrepo.server.management.FedoraAPIMMTOM m_instance;

    public APIMStubWrapper(org.fcrepo.server.management.FedoraAPIMMTOM instance) {
        m_instance = instance;
    }

    @Override
    public String ingest(final DataHandler objectXML,
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
    public DataHandler getObjectXML(final String pid){
        String buf = "Get object XML";
        // Run the method in a SwingWorker thread
        SwingWorker<DataHandler> worker = new SwingWorker<DataHandler>() {

            @Override
            public DataHandler construct() {
                    // call wrapped method
                    return m_instance.getObjectXML(pid);
            }
        };
        return SwingWorker.waitForResult(worker, buf);

    }

    @Override
    public DataHandler export(final String pid,
                         final String format,
                         final String context){
        String buf = "Export";
        // Run the method in a SwingWorker thread
        SwingWorker<DataHandler> worker = new SwingWorker<DataHandler>() {

            @Override
            public DataHandler construct() {
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
                    return m_instance
			    .purgeObject(pid,
                                         logMessage,
			                 force);
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
    public String modifyDatastreamByValue(final String pid,
            final String dsID,
            final ArrayOfString altIDs,
            final String dsLabel,
            final String MIMEType,
            final String formatURI,
            final DataHandler dsContent,
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
                            .compareDatastreamChecksum(pid, dsID, versionDate);
            }
        };
        return SwingWorker.waitForResult(worker, buf);

    }

    @Override
    public Datastream getDatastream(final String pid,
            final String dsID,
            final String asOfDateTime){
        String buf = "Get datastream";
        // Run the method in a SwingWorker thread
        SwingWorker<Datastream> worker = new SwingWorker<Datastream>() {

            @Override
            public Datastream construct() {
                    // call wrapped method
                    return m_instance
                            .getDatastream(pid,
                                           dsID,
                                           asOfDateTime);
            }
        };
        return SwingWorker.waitForResult(worker, buf);

    }

    @Override
    public List<Datastream> getDatastreams(final String pid,
            final String asOfDateTime,
            final String dsState){
        String buf = "Get datastreams";
        // Run the method in a SwingWorker thread
        SwingWorker<List<Datastream>> worker =
                new SwingWorker<List<Datastream>>() {

            @Override
            public List<Datastream> construct() {
                    // call wrapped method
                    return m_instance.getDatastreams(pid, asOfDateTime, dsState);
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
                                         final String pidNamespace){
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
    public boolean addRelationship(final String pid,
            final String relationship,
            final String object,
            final boolean isLiteral,
            final String datatype){
        String buf = "Add relationship";
        // Run the method in a SwingWorker thread
        SwingWorker<Boolean> worker = new SwingWorker<Boolean>() {

            @Override
            public Boolean construct() {
                    // call wrapped method
                    return m_instance.addRelationship(pid,
                            relationship, object, isLiteral, datatype);
            }
        };
        return SwingWorker.waitForResult(worker, buf);

    }

    @Override
    public boolean purgeRelationship(final String pid,
            final String relationship,
            final String object,
            final boolean isLiteral,
            final String datatype){
        String buf = "Purge relationship";
        // Run the method in a SwingWorker thread
        SwingWorker<Boolean> worker = new SwingWorker<Boolean>() {

            @Override
            public Boolean construct() {
                    // call wrapped method
                    return m_instance
                            .purgeRelationship(pid,
                                    relationship,
                                    object,
                                    isLiteral,
                                    datatype);
            }
        };
        return SwingWorker.waitForResult(worker, buf);

    }

    @Override
    public Validation validate(final String pid,
            final String asOfDateTime){
        String buf = "Validate";
        // Run the method in a SwingWorker thread
        SwingWorker<Validation> worker = new SwingWorker<Validation>() {

            @Override
            public Validation construct() {
                    // call wrapped method
                    return m_instance.validate(pid, asOfDateTime);
            }
        };
        return SwingWorker.waitForResult(worker, buf);

    }
}
