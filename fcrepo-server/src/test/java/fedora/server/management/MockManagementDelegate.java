/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.management;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import fedora.server.Context;
import fedora.server.errors.ServerException;
import fedora.server.journal.JournalConstants;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.RelationshipTuple;

/**
 * A generic management delegate for use in Journal unit testing. Each method
 * call generates a {@link Call} object which is added to a list of calls for
 * later inspection. The method then returns some plausible result value if
 * required.
 *
 * @author Firstname Lastname
 */
public class MockManagementDelegate
        implements ManagementDelegate {

    // ----------------------------------------------------------------------
    // Mocking infrastructure
    // ----------------------------------------------------------------------

    /**
     * A data class that holds the information from a method call: method name
     * and method arguments.
     */
    public static class Call {

        private final String methodName;

        private final Object[] methodArgs;

        public Call(String methodName, Object... methodArgs) {
            this.methodName = methodName;
            this.methodArgs = methodArgs;
        }

        public String getMethodName() {
            return methodName;
        }

        public Object[] getMethodArgs() {
            return methodArgs;
        }

        @Override
        public String toString() {
            return "Call[" + methodName + ", "
                    + Arrays.deepToString(methodArgs) + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!Call.class.equals(obj.getClass())) {
                return false;
            }
            Call that = (Call) obj;

            return methodName.equals(that.methodName)
                    && equalArgs(methodArgs, that.methodArgs);
        }

        private boolean equalArgs(Object[] args1, Object[] args2) {
            if (args1.length != args2.length) {
                return false;
            }
            for (int i = 0; i < args1.length; i++) {
                if (args1[i] instanceof InputStream
                        && args2[i] instanceof InputStream) {
                    // Input streams are considered equal, even if different types,
                    // and without testing the contents.
                    continue;
                } else if (args1[i] instanceof Object[]
                        && args2[i] instanceof Object[]) {
                    // Arrays are compared for equal members, not identical members.
                    if (Arrays.deepEquals((Object[]) args1[i],
                                          (Object[]) args2[i])) {
                        continue;
                    }
                } else if (args1[i].equals(args2[i])) {
                    continue;
                }
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return methodName.hashCode() ^ Arrays.asList(methodArgs).hashCode();
        }

    }

    private final List<Call> calls = new ArrayList<Call>();

    public void reset() {
        calls.clear();
    }

    public int getCallCount() {
        return calls.size();
    }

    public List<Call> getCalls() {
        return new ArrayList<Call>(calls);
    }

    // ----------------------------------------------------------------------
    // Mocked methods
    // ----------------------------------------------------------------------

    public String addDatastream(Context context,
                                String pid,
                                String dsID,
                                String[] altIDs,
                                String dsLabel,
                                boolean versionable,
                                String MIMEType,
                                String formatURI,
                                String location,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_ADD_DATASTREAM,
                           context,
                           pid,
                           dsID,
                           altIDs,
                           dsLabel,
                           versionable,
                           MIMEType,
                           formatURI,
                           location,
                           controlGroup,
                           dsState,
                           checksumType,
                           checksum,
                           logMessage));
        return dsID;
    }

    public boolean adminPing(Context context) throws ServerException {
        calls.add(new Call("adminPing", context));
        return true;
    }

    public boolean addRelationship(Context context,
                                   String subject,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_ADD_RELATIONSHIP,
                           context,
                           subject,
                           relationship,
                           object,
                           isLiteral,
                           datatype));
        return true;
    }

    public String compareDatastreamChecksum(Context context,
                                            String pid,
                                            String dsID,
                                            Date asOfDateTime)
            throws ServerException {
        calls.add(new Call("compareDatastreamChecksum",
                           context,
                           pid,
                           dsID,
                           asOfDateTime));
        return "bogusChecksum";
    }

    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        calls.add(new Call("export",
                           context,
                           pid,
                           format,
                           exportContext,
                           encoding));
        return new ByteArrayInputStream(new byte[0]);
    }

    public Datastream getDatastream(Context context,
                                    String pid,
                                    String datastreamID,
                                    Date asOfDateTime) throws ServerException {
        calls.add(new Call("getDatastream",
                           context,
                           pid,
                           datastreamID,
                           asOfDateTime));
        return null;
    }

    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String datastreamID)
            throws ServerException {
        calls.add(new Call("getDatastreamHistory", context, pid, datastreamID));
        return new Datastream[0];
    }

    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String dsState) throws ServerException {
        calls.add(new Call("getDatastreams",
                           context,
                           pid,
                           asOfDateTime,
                           dsState));
        return new Datastream[0];
    }

    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        calls.add(new Call("getNextPID", context, numPIDs, namespace));
        String[] result = new String[numPIDs];
        for (int i = 0; i < result.length; i++) {
            result[i] = "sillyPID_" + i;
        }
        return result;
    }

    public InputStream getObjectXML(Context context, String pid, String encoding) {
        calls.add(new Call("getObjectXML", context, pid, encoding));
        return new ByteArrayInputStream(new byte[0]);
    }

    public RelationshipTuple[] getRelationships(Context context,
                                                String subject,
                                                String relationship)
            throws ServerException {
        calls.add(new Call("getRelationships", context, subject, relationship));
        return new RelationshipTuple[0];
    }

    public InputStream getTempStream(String id) throws ServerException {
        calls.add(new Call("getTempStream", id));
        return new ByteArrayInputStream(new byte[0]);
    }

    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         boolean newPid) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_INGEST,
                           context,
                           serialization,
                           logMessage,
                           format,
                           encoding,
                           newPid));
        return "Ingest:" + getCallCount();
    }

    public Date modifyDatastreamByReference(Context context,
                                            String pid,
                                            String datastreamID,
                                            String[] altIDs,
                                            String dsLabel,
                                            String mimeType,
                                            String formatURI,
                                            String dsLocation,
                                            String checksumType,
                                            String checksum,
                                            String logMessage,
                                            boolean force)
            throws ServerException {
        calls
                .add(new Call(JournalConstants.METHOD_MODIFY_DATASTREAM_BY_REFERENCE,
                              context,
                              pid,
                              datastreamID,
                              altIDs,
                              dsLabel,
                              mimeType,
                              formatURI,
                              dsLocation,
                              checksumType,
                              checksum,
                              logMessage,
                              force));
        return new Date(111111L);
    }

    public Date modifyDatastreamByValue(Context context,
                                        String pid,
                                        String datastreamID,
                                        String[] altIDs,
                                        String dsLabel,
                                        String mimeType,
                                        String formatURI,
                                        InputStream dsContent,
                                        String checksumType,
                                        String checksum,
                                        String logMessage,
                                        boolean force) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_MODIFY_DATASTREAM_BY_VALUE,
                           context,
                           pid,
                           datastreamID,
                           altIDs,
                           dsLabel,
                           mimeType,
                           formatURI,
                           dsContent,
                           checksumType,
                           checksum,
                           logMessage,
                           force));
        return new Date(222222L);
    }

    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerId,
                             String logMessage) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_MODIFY_OBJECT,
                           context,
                           pid,
                           state,
                           label,
                           ownerId,
                           logMessage));
        return new Date(10000L);
    }

    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String datastreamID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage,
                                  boolean force) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_PURGE_DATASTREAM,
                           context,
                           pid,
                           datastreamID,
                           startDT,
                           endDT,
                           logMessage,
                           force));
        return new Date[0];
    }

    public Date purgeObject(Context context,
                            String pid,
                            String logMessage,
                            boolean force) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_PURGE_OBJECT,
                           context,
                           pid,
                           logMessage,
                           force));
        return new Date(654L);
    }

    public boolean purgeRelationship(Context context,
                                     String subject,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_PURGE_RELATIONSHIP,
                           context,
                           subject,
                           relationship,
                           object,
                           isLiteral,
                           datatype));
        return false;
    }

    public String putTempStream(Context context, InputStream in)
            throws ServerException {
        calls
                .add(new Call(JournalConstants.METHOD_PUT_TEMP_STREAM,
                              context,
                              in));
        return "tempStreamId";
    }

    public Date setDatastreamState(Context context,
                                   String pid,
                                   String dsID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_SET_DATASTREAM_STATE,
                           context,
                           pid,
                           dsID,
                           dsState,
                           logMessage));
        return new Date(3000000L);
    }

    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String dsID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        calls.add(new Call(JournalConstants.METHOD_SET_DATASTREAM_VERSIONABLE,
                           context,
                           pid,
                           dsID,
                           versionable,
                           logMessage));
        return new Date(234234L);
    }

    // ----------------------------------------------------------------------
    // Un-implemented methods
    // ----------------------------------------------------------------------

}
