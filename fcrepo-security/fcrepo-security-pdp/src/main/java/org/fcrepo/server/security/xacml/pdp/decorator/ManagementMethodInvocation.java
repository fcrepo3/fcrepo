/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.security.xacml.pdp.decorator;

import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.management.Management;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.server.storage.types.Validation;

/**
 * Used to provide an explicitly paramterised view of API-M methods that affect the policy cache
 *
 * Note: this is not strictly an implementation of Management, but using this interface ensures
 * a build error will be generated if Management changes and any new or modified methods are not
 * catered for by the decorator.
 *
 * Constructor locates the equivalent API-M method in this class, and invokes it.
 *
 * Invoking the method pulls out the relevant method parameters, and determines what
 * kind of operation it is.
 *
 * Methods that do not affect the policy cache should set action to NA
 *
 * @author Stephen Bayliss
 * @version $Id$
 */
public class ManagementMethodInvocation
        implements Management {

    /** Logger for this class. */
    private static Logger LOG =
            LoggerFactory.getLogger(PolicyIndexInvocationHandler.class.getName());

    // represents the type of operation
    public static enum Action {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        NA
    }
    public Action action = null;

    // represents the overall target of the operation
    public static enum Target {
        DIGITALOBJECT,
        DATASTREAM,
        OTHER
    }
    public Target target = null;

    // represents the component of the Target
    public static enum Component {
        CONTENT,
        STATE,
        OTHER
    }
    public Component component = null;

    // parameters of the method (only the ones relevant to operations on policy objects are included)
    public class Parameters {
        public Context context = null;
        public String pid = null;
        public String dsID = null;
        public String dsState = null;
        public String objectState = null;
    }
    public Parameters parameters = new Parameters();

    @SuppressWarnings("unused")
    private ManagementMethodInvocation() {
    }

    /**
     * Given an API-M method and arguments, locate the same method in this class and invoke it,
     * populating the method parameters and classifying the method in terms of the kind of action
     * and the kind of resource it is operating on.
     *
     * @param method
     * @param args
     * @throws InvocationTargetException
     */
    public ManagementMethodInvocation(Method method, Object[] args) throws InvocationTargetException {

        // get methods of this class
        Method methods[] = this.getClass().getMethods();

        // find the equivalent method in this class to method given in constructor
        Method targetMethod = null;
        for (int i = 0; i < methods.length; i++ ) {
            if (methods[i].getName().equals(method.getName())) {
                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) {
            LOG.warn("Method " + method.getName() + " not found");
            throw new InvocationTargetException(new Exception("Method not found"));
        }

        // invoke the method
        try {
            targetMethod.invoke(this, args);

            // or can we just do
            // method.invoke(this, args)?
        } catch (Exception e) {
            LOG.warn("Execution failed for method " + method.getName());
            throw new InvocationTargetException(e);
        }

    }


    @Override
    public String addDatastream(Context context,
                                String pid,
                                String dsID,
                                String[] altIDs,
                                String dsLabel,
                                boolean versionable,
                                String mimeType,
                                String formatURI,
                                String dsLocation,
                                String controlGroup,
                                String dsState,
                                String checksumType,
                                String checksum,
                                String logMessage) throws ServerException {

        action = Action.CREATE;
        target = Target.DATASTREAM;
        component = Component.CONTENT;
        parameters.context = context;
        parameters.pid = pid;
        parameters.dsID = dsID;
        parameters.dsState = dsState;

        return null;
    }

    @Override
    public boolean addRelationship(Context context,
                                   String subject,
                                   String relationship,
                                   String object,
                                   boolean isLiteral,
                                   String datatype) throws ServerException {
        action = Action.NA;
        return false;
    }

    @Override
    public String compareDatastreamChecksum(Context context,
                                            String pid,
                                            String dsID,
                                            Date asOfDateTime)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public InputStream export(Context context,
                              String pid,
                              String format,
                              String exportContext,
                              String encoding) throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public Datastream getDatastream(Context context,
                                    String pid,
                                    String dsID,
                                    Date asOfDateTime) throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public Datastream[] getDatastreamHistory(Context context,
                                             String pid,
                                             String dsID)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public Datastream[] getDatastreams(Context context,
                                       String pid,
                                       Date asOfDateTime,
                                       String dsState) throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public String[] getNextPID(Context context, int numPIDs, String namespace)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public InputStream getObjectXML(Context context, String pid, String encoding)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public RelationshipTuple[] getRelationships(Context context,
                                                String subject,
                                                String relationship)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public InputStream getTempStream(String id) throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public String ingest(Context context,
                         InputStream serialization,
                         String logMessage,
                         String format,
                         String encoding,
                         String newPid) throws ServerException {
        action = Action.CREATE;
        target = Target.DIGITALOBJECT;
        component = Component.CONTENT;
        parameters.context = context;
        return null;
    }

    @Override
    public Date modifyDatastreamByReference(Context context,
                                            String pid,
                                            String dsID,
                                            String[] altIDs,
                                            String dsLabel,
                                            String mimeType,
                                            String formatURI,
                                            String dsLocation,
                                            String checksumType,
                                            String checksum,
                                            String logMessage,
                                            Date lastModifiedDate)
            throws ServerException {


        action = Action.UPDATE;
        target = Target.DATASTREAM;
        component = Component.CONTENT;
        parameters.context = context;
        parameters.pid = pid;
        parameters.dsID = dsID;
        return null;
    }

    @Override
    public Date modifyDatastreamByValue(Context context,
                                        String pid,
                                        String dsID,
                                        String[] altIDs,
                                        String dsLabel,
                                        String mimeType,
                                        String formatURI,
                                        InputStream dsContent,
                                        String checksumType,
                                        String checksum,
                                        String logMessage,
                                        Date lastModifiedDate) throws ServerException {
        action = Action.UPDATE;
        target = Target.DATASTREAM;
        component = Component.CONTENT;
        parameters.context = context;
        parameters.pid = pid;
        parameters.dsID = dsID;
        return null;
    }

    @Override
    public Date modifyObject(Context context,
                             String pid,
                             String state,
                             String label,
                             String ownerID,
                             String logMessage,
                             Date lastModifiedDate) throws ServerException {
        action = Action.UPDATE;
        target = Target.DIGITALOBJECT;
        component = Component.STATE;
        parameters.context = context;
        parameters.pid = pid;
        parameters.objectState = state;
        return null;
    }

    @Override
    public Date[] purgeDatastream(Context context,
                                  String pid,
                                  String dsID,
                                  Date startDT,
                                  Date endDT,
                                  String logMessage) throws ServerException {
        action = Action.DELETE;
        target = Target.DATASTREAM;
        component = Component.CONTENT;
        parameters.context = context;
        parameters.pid = pid;
        parameters.dsID = dsID;
        return null;
    }

    @Override
    public Date purgeObject(Context context,
                            String pid,
                            String logMessage) throws ServerException {
        action = Action.DELETE;
        target = Target.DIGITALOBJECT;
        component = Component.CONTENT;
        parameters.context = context;
        parameters.pid = pid;
        return null;
    }

    @Override
    public boolean purgeRelationship(Context context,
                                     String subject,
                                     String relationship,
                                     String object,
                                     boolean isLiteral,
                                     String datatype) throws ServerException {
        action = Action.NA;
        return false;
    }

    @Override
    public String putTempStream(Context context, InputStream in)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public Date setDatastreamState(Context context,
                                   String pid,
                                   String dsID,
                                   String dsState,
                                   String logMessage) throws ServerException {
        action = Action.UPDATE;
        target = Target.DATASTREAM;
        component = Component.STATE;
        parameters.context = context;
        parameters.pid = pid;
        parameters.dsID = dsID;
        parameters.dsState = dsState;
        return null;
    }

    @Override
    public Date setDatastreamVersionable(Context context,
                                         String pid,
                                         String dsID,
                                         boolean versionable,
                                         String logMessage)
            throws ServerException {
        action = Action.NA;
        return null;
    }

    @Override
    public Validation validate(Context context, String pid, Date asOfDateTime)
            throws ServerException {
        action = Action.NA;
        return null;
    }

}
