/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;

import fedora.common.Models;

import fedora.server.Context;
import fedora.server.errors.DisseminationException;
import fedora.server.errors.MethodNotFoundException;
import fedora.server.errors.ObjectIntegrityException;
import fedora.server.errors.ServerException;
import fedora.server.errors.StorageException;
import fedora.server.errors.StreamIOException;
import fedora.server.errors.UnsupportedTranslationException;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.translation.DOTranslator;
import fedora.server.storage.types.AuditRecord;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.MethodDef;
import fedora.server.storage.types.MethodParmDef;
import fedora.server.storage.types.ObjectMethodsDef;
import fedora.server.storage.types.RelationshipTuple;
import fedora.server.utilities.DateUtility;

import static fedora.common.Constants.MODEL;


/**
 * A DOReader backed by a DigitalObject.
 *
 * @author Chris Wilper
 */
public class SimpleDOReader
        implements DOReader {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(SimpleDOReader.class.getName());

    protected final DigitalObject m_obj;

    private final Context m_context;

    private final RepositoryReader m_repoReader;

    private final DOTranslator m_translator;

    private final String m_exportFormat;

    private String m_storageFormat;

    private final SimpleDateFormat m_formatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public SimpleDOReader(Context context,
                          RepositoryReader repoReader,
                          DOTranslator translator,
                          String exportFormat,
                          String storageFormat,
                          String encoding,
                          InputStream serializedObject)
            throws ObjectIntegrityException, StreamIOException,
                   UnsupportedTranslationException, ServerException {
        m_context = context;
        m_repoReader = repoReader;
        m_translator = translator;
        m_exportFormat = exportFormat;
        m_storageFormat = storageFormat;
        m_obj = new BasicDigitalObject();
        m_translator.deserialize(serializedObject,
                                 m_obj,
                                 m_storageFormat,
                                 encoding,
                                 DOTranslationUtility.DESERIALIZE_INSTANCE);
    }

    /**
     * Alternate constructor for when a DigitalObject is already available for
     * some reason.
     */
    public SimpleDOReader(Context context,
                          RepositoryReader repoReader,
                          DOTranslator translator,
                          String exportFormat,
                          String encoding,
                          DigitalObject obj) {
        m_context = context;
        m_repoReader = repoReader;
        m_translator = translator;
        m_exportFormat = exportFormat;
        m_obj = obj;
    }

    /**
     * {@inheritDoc}
     */
    public DigitalObject getObject() {
        return m_obj;
    }

    /**
     * {@inheritDoc}
     */
    public Date getCreateDate() {
        return m_obj.getCreateDate();
    }

    /**
     * {@inheritDoc}
     */
    public Date getLastModDate() {
        return m_obj.getLastModDate();
    }

    /**
     * {@inheritDoc}
     */
    public String getOwnerId() {
        return m_obj.getOwnerId();
    }

    /**
     * {@inheritDoc}
     */
    public List<AuditRecord> getAuditRecords() {
        return m_obj.getAuditRecords();
    }

    /**
     * Return the object as an XML input stream in the internal serialization
     * format.
     */
    public InputStream GetObjectXML() throws ObjectIntegrityException,
                                             StreamIOException, UnsupportedTranslationException, ServerException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        m_translator.serialize(m_obj,
                               bytes,
                               m_storageFormat,
                               "UTF-8",
                               DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    /**
     * {@inheritDoc}
     */
    public InputStream Export(String format, String exportContext)
            throws ObjectIntegrityException, StreamIOException,
                   UnsupportedTranslationException, ServerException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int transContext;
        // first, set the translation context...
        LOG.debug("Export context: " + exportContext);

        if (exportContext == null || exportContext.equals("")
            || exportContext.equalsIgnoreCase("default")) {
            // null and default is set to PUBLIC translation
            transContext = DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC;
        } else if (exportContext.equalsIgnoreCase("public")) {
            transContext = DOTranslationUtility.SERIALIZE_EXPORT_PUBLIC;
        } else if (exportContext.equalsIgnoreCase("migrate")) {
            transContext = DOTranslationUtility.SERIALIZE_EXPORT_MIGRATE;
        } else if (exportContext.equalsIgnoreCase("archive")) {
            transContext = DOTranslationUtility.SERIALIZE_EXPORT_ARCHIVE;
        } else {
            throw new UnsupportedTranslationException("Export context "
                                                      + exportContext + " is not valid.");
        }
        // now serialize for export in the proper XML format...
        if (format == null || format.equals("")
            || format.equalsIgnoreCase("default")) {
            LOG.debug("Export in default format: " + m_exportFormat);
            m_translator.serialize(m_obj,
                                   bytes,
                                   m_exportFormat,
                                   "UTF-8",
                                   transContext);
        } else {
            LOG.debug("Export in format: " + format);
            m_translator.serialize(m_obj, bytes, format, "UTF-8", transContext);
        }

        return new ByteArrayInputStream(bytes.toByteArray());
    }

    /**
     * @deprecated in Fedora 3.0, use Export instead
     */
    @Deprecated
    public InputStream ExportObject(String format, String exportContext)
            throws ObjectIntegrityException, StreamIOException,
                   UnsupportedTranslationException, ServerException {
        return Export(format, exportContext);
    }

    /**
     * {@inheritDoc}
     */
    public String GetObjectPID() {
        return m_obj.getPid();
    }

    /**
     * {@inheritDoc}
     */
    public String GetObjectLabel() {
        return m_obj.getLabel();
    }

    /**
     * {@inheritDoc}
     */
    public String GetObjectState() {
        if (m_obj.getState() == null) {
            return "A"; // shouldn't happen, but if it does don't die
        }
        return m_obj.getState();
    }

    public List<String> getContentModels() throws ServerException {

        List<String> list = new ArrayList<String>();
        for (RelationshipTuple rel : getRelationships(MODEL.HAS_MODEL,null)) {
            list.add(rel.object);
        }
        return list;
    }

    public boolean hasContentModel(ObjectNode contentModel)
            throws ServerException {
        return hasRelationship(MODEL.HAS_MODEL,contentModel);
    }

    /**
     * {@inheritDoc}
     */
    public String[] ListDatastreamIDs(String state) {
        Iterator<String> iter = m_obj.datastreamIdIterator();
        ArrayList<String> al = new ArrayList<String>();
        while (iter.hasNext()) {
            String dsId = iter.next();
            if (state == null) {
                al.add(dsId);
            } else {
                // below should never return null -- already know id exists,
                // and am asking for any the latest existing one.
                Datastream ds = GetDatastream(dsId, null);
                if (ds.DSState.equals(state)) {
                    al.add(dsId);
                }
            }
        }
        iter = al.iterator();
        String[] out = new String[al.size()];
        int i = 0;
        while (iter.hasNext()) {
            out[i] = iter.next();
            i++;
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public Datastream getDatastream(String dsID, String versionID) {
        for (Datastream ds : m_obj.datastreams(dsID)) {
            if (ds.DSVersionID.equals(versionID)) {
                return ds;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Datastream GetDatastream(String datastreamID, Date versDateTime) {
        // get the one with the closest creation date
        // without going over
        Datastream closestWithoutGoingOver = null;
        Datastream latestCreated = null;
        long bestTimeDifference = -1;
        long latestCreateTime = -1;
        long vTime = -1;
        if (versDateTime != null) {
            vTime = versDateTime.getTime();
        }
        for (Datastream ds : m_obj.datastreams(datastreamID)) {
            if (versDateTime == null) {
                if (ds.DSCreateDT.getTime() > latestCreateTime) {
                    latestCreateTime = ds.DSCreateDT.getTime();
                    latestCreated = ds;
                }
            } else {
                long diff = vTime - ds.DSCreateDT.getTime();
                if (diff >= 0) {
                    if (diff < bestTimeDifference || bestTimeDifference == -1) {
                        bestTimeDifference = diff;
                        closestWithoutGoingOver = ds;
                    }
                }
            }
        }
        if (versDateTime == null) {
            return latestCreated;
        } else {
            return closestWithoutGoingOver;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date[] getDatastreamVersions(String datastreamID) {
        ArrayList<Date> versionDates = new ArrayList<Date>();
        for (Datastream d : m_obj.datastreams(datastreamID)) {
            versionDates.add(d.DSCreateDT);
        }
        return versionDates.toArray(new Date[0]);
    }

    /**
     * {@inheritDoc}
     */
    public Datastream[] GetDatastreams(Date versDateTime, String state) {
        String[] ids = ListDatastreamIDs(null);
        ArrayList<Datastream> al = new ArrayList<Datastream>();
        for (String element : ids) {
            Datastream ds = GetDatastream(element, versDateTime);
            if (ds != null && (state == null || ds.DSState.equals(state))) {
                al.add(ds);
            }
        }
        Datastream[] out = new Datastream[al.size()];
        Iterator<Datastream> iter = al.iterator();
        int i = 0;
        while (iter.hasNext()) {
            out[i] = iter.next();
            i++;
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getObjectHistory(String PID) {
        String[] dsIDs = ListDatastreamIDs("A");
        TreeSet<String> modDates = new TreeSet<String>();
        for (String element : dsIDs) {
            Date[] dsDates = getDatastreamVersions(element);
            for (Date element2 : dsDates) {
                modDates.add(DateUtility.convertDateToString(element2));
            }
        }
        return modDates.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    private MethodDef[] listMethods(String sDefPID,
                                    ServiceDefinitionReader sDefReader,
                                    Date versDateTime)
            throws MethodNotFoundException, ServerException {
        if (sDefPID.equalsIgnoreCase("fedora-system:1")
            || sDefPID.equalsIgnoreCase("fedora-system:3")) {
            throw new MethodNotFoundException("[getObjectMethods] The object, "
                                              + m_obj.getPid()
                                              + ", will not report on dynamic method definitions "
                                              + "at this time (fedora-system:1 and fedora-system:3.");
        }

        if (sDefReader == null) {
            return null;
        }
        MethodDef[] methods = sDefReader.getAbstractMethods(versDateTime);
        // Filter out parms that are internal to the mechanism and not part
        // of the abstract method definition. We just want user parms.
        for (int i = 0; i < methods.length; i++) {
            methods[i].methodParms = filterParms(methods[i]);
        }
        return methods;
    }

    /**
     * Filters out mechanism-specific parms (system default parms and datastream
     * input parms) so that what is returned is only method parms that reflect
     * abstract method definitions. Abstract method definitions only expose
     * user-supplied parms.
     *
     * @param method
     * @return
     */
    private MethodParmDef[] filterParms(MethodDef method) {
        ArrayList<MethodParmDef> filteredParms = new ArrayList<MethodParmDef>();
        for (MethodParmDef element : method.methodParms) {
            if (element.parmType.equalsIgnoreCase(MethodParmDef.USER_INPUT)) {
                filteredParms.add(element);
            }
        }
        return filteredParms.toArray(new MethodParmDef[0]);
    }

    protected String getWhenString(Date versDateTime) {
        if (versDateTime != null) {
            return m_formatter.format(versDateTime);
        } else {
            return "the current time";
        }
    }

    /**
     * {@inheritDoc}
     */
    public ObjectMethodsDef[] listMethods(Date versDateTime)
            throws ServerException {
        ArrayList<MethodDef> methodList = new ArrayList<MethodDef>();
        ArrayList<String> sDefIDList = new ArrayList<String>();

        ServiceDefinitionReader sDefReader = null;
/*
        Set<RelationshipTuple> cmRels =
                getRelationships(MODEL.HAS_MODEL, null);

        for (RelationshipTuple element : cmRels) {
*/

        for (String cm:getContentModels()){
            /*
             * FIXME: If the we encounter a relation to one of the "system"
             * models, then skip it, since its functionality is hardwired in.
             * Ideally, we would actually instantiate the system objects, and
             * wire in the default system behaviour based upon their content
             * (just like everything else)
             */
            if (Models.contains(cm)) {
                continue;
            }

            DOReader cmReader;
            String cModelPid = cm.substring(12);
            if ("self".equals(cModelPid)) {
                cmReader = this;
                cModelPid = GetObjectPID();
            } else {
                try {
                    cmReader =
                            m_repoReader.getReader(false, m_context, cModelPid);
                } catch (StorageException e) {
                    throw new DisseminationException(null,
                                                     "Content Model Object "
                                                     + cModelPid
                                                     + " does not exist.",
                                                     null,
                                                     null,
                                                     e);
                }
            }
            Set<RelationshipTuple> hasServiceRels =
                    cmReader
                            .getRelationships(MODEL.HAS_SERVICE, null);
            for (RelationshipTuple element2 : hasServiceRels) {
                String sDefPid = element2.getObjectPID();

                try {
                    sDefReader =
                            m_repoReader.getServiceDefinitionReader(false,
                                                                    m_context,
                                                                    sDefPid);
                } catch (StorageException se) {
                    throw new DisseminationException("Service definition "
                                                     + sDefPid + " required by Content Model "
                                                     + cModelPid + " not found.");
                }
                MethodDef[] methods =
                        listMethods(sDefPid, sDefReader, versDateTime);
                if (methods != null) {
                    for (MethodDef element3 : methods) {
                        methodList.add(element3);
                        sDefIDList.add(element2.getObjectPID());
                    }
                }
            }
        }

        ObjectMethodsDef[] ret = new ObjectMethodsDef[methodList.size()];
        for (int i = 0; i < methodList.size(); i++) {
            MethodDef def = methodList.get(i);
            ret[i] = new ObjectMethodsDef();
            ret[i].PID = GetObjectPID();
            ret[i].sDefPID = sDefIDList.get(i);
            ret[i].methodName = def.methodName;
            ret[i].methodParmDefs = def.methodParms;
            ret[i].asOfDate = versDateTime;
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasRelationship(SubjectNode subject, PredicateNode predicate, ObjectNode object) {
        return m_obj.hasRelationship(subject, predicate, object);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasRelationship(PredicateNode predicate, ObjectNode object) {
        return m_obj.hasRelationship(predicate, object);
    }

    /**
     * {@inheritDoc}
     */
    public Set<RelationshipTuple> getRelationships(SubjectNode subject,
                                                   PredicateNode predicate,
                                                   ObjectNode object) {
        return m_obj.getRelationships(subject, predicate, object);
    }

    /**
     * {@inheritDoc}
     */
    public Set<RelationshipTuple> getRelationships(PredicateNode predicate,
                                                   ObjectNode object) {
        return m_obj.getRelationships(predicate, object);
    }
    /**
     * {@inheritDoc}
     */
    public Set<RelationshipTuple> getRelationships() {
        return m_obj.getRelationships();
    }

}
