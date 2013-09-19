/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import static org.fcrepo.common.Constants.MODEL;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.fcrepo.common.Models;
import org.fcrepo.server.Context;
import org.fcrepo.server.errors.DisseminationException;
import org.fcrepo.server.errors.MethodNotFoundException;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StorageException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.UnsupportedTranslationException;
import org.fcrepo.server.storage.translation.DOTranslationUtility;
import org.fcrepo.server.storage.translation.DOTranslator;
import org.fcrepo.server.storage.types.AuditRecord;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.MethodDef;
import org.fcrepo.server.storage.types.MethodParmDef;
import org.fcrepo.server.storage.types.ObjectMethodsDef;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.utilities.DateUtility;
import org.fcrepo.utilities.ReadableByteArrayOutputStream;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * A DOReader backed by a DigitalObject.
 *
 * @author Chris Wilper
 */
public class SimpleDOReader
        implements DOReader {

    private static final Logger logger =
            LoggerFactory.getLogger(SimpleDOReader.class);
    
    private static final Datastream[] DATASTREAM_TYPE =
            new Datastream[0];

    private static final Date[] DATE_TYPE =
            new Date[0];
    
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

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
    @Override
    public DigitalObject getObject() {
        return m_obj;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCreateDate() {
        return m_obj.getCreateDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getLastModDate() {
        return m_obj.getLastModDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOwnerId() {
        return m_obj.getOwnerId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AuditRecord> getAuditRecords() {
        return m_obj.getAuditRecords();
    }

    /**
     * Return the object as an XML input stream in the internal serialization
     * format.
     */
    @Override
    public InputStream GetObjectXML() throws ObjectIntegrityException,
                                             StreamIOException, UnsupportedTranslationException, ServerException {
        ReadableByteArrayOutputStream bytes = new ReadableByteArrayOutputStream(4096);
        m_translator.serialize(m_obj,
                               bytes,
                               m_storageFormat,
                               "UTF-8",
                               DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);
        return bytes.toInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream Export(String format, String exportContext)
            throws ObjectIntegrityException, StreamIOException,
                   UnsupportedTranslationException, ServerException {
        int transContext;
        // first, set the translation context...
        logger.debug("Export context: {}", exportContext);

        if (exportContext == null || exportContext.isEmpty()
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

        // allocate the ByteArrayOutputStream with a 4k initial capacity to constrain copying up
        ReadableByteArrayOutputStream bytes = new ReadableByteArrayOutputStream(4096);
        // now serialize for export in the proper XML format...
        if (format == null || format.isEmpty()
            || format.equalsIgnoreCase("default")) {
            logger.debug("Export in default format: {}", m_exportFormat);
            m_translator.serialize(m_obj,
                                   bytes,
                                   m_exportFormat,
                                   "UTF-8",
                                   transContext);
        } else {
            logger.debug("Export in format: {}", format);
            m_translator.serialize(m_obj, bytes, format, "UTF-8", transContext);
        }

        return bytes.toInputStream();
    }

    /**
     * @deprecated in Fedora 3.0, use Export instead
     */
    @Override
    @Deprecated
    public InputStream ExportObject(String format, String exportContext)
            throws ObjectIntegrityException, StreamIOException,
                   UnsupportedTranslationException, ServerException {
        return Export(format, exportContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String GetObjectPID() {
        return m_obj.getPid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String GetObjectLabel() {
        return m_obj.getLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String GetObjectState() {
        if (m_obj.getState() == null) {
            return "A"; // shouldn't happen, but if it does don't die
        }
        return m_obj.getState();
    }

    @Override
    public List<String> getContentModels() throws ServerException {

        Set<RelationshipTuple> rels = getRelationships(MODEL.HAS_MODEL,null); 
        List<String> list = new ArrayList<String>(rels.size());
        for (RelationshipTuple rel : rels) {
            list.add(rel.object);
        }
        return list;
    }

    @Override
    public boolean hasContentModel(ObjectNode contentModel)
            throws ServerException {
        return hasRelationship(MODEL.HAS_MODEL,contentModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        return al.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public Datastream GetDatastream(String datastreamID, Date versDateTime) {
        // get the one with the closest creation date
        // without going over
        Datastream result = null;
        long bestTimeDifference = Long.MAX_VALUE;
        long latestCreateTime = -1;
        long vTime = -1;
        if (versDateTime != null) {
            vTime = versDateTime.getTime();
        }
        for (Datastream ds : m_obj.datastreams(datastreamID)) {
            if (versDateTime == null) {
                if (ds.DSCreateDT == null || ds.DSCreateDT.getTime() > latestCreateTime || result == null) {
                    result = ds;
                    if (ds.DSCreateDT != null) latestCreateTime = ds.DSCreateDT.getTime();
                }
            } else {
                //TODO If none of the versions have a create date, what should behavior be?
                long diff = (ds.DSCreateDT == null)? vTime : vTime - ds.DSCreateDT.getTime();
                if (diff >= 0) {
                    if (diff < bestTimeDifference) {
                        bestTimeDifference = diff;
                        result = ds;
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date[] getDatastreamVersions(String datastreamID) {
        ArrayList<Date> versionDates = new ArrayList<Date>();
        
        for (Datastream d : m_obj.datastreams(datastreamID)) {
            versionDates.add(d.DSCreateDT);
        }
        return versionDates.toArray(DATE_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Datastream[] GetDatastreams(Date versDateTime, String state) {
        String[] ids = ListDatastreamIDs(null);
        ArrayList<Datastream> al = new ArrayList<Datastream>(ids.length);
        for (String element : ids) {
            Datastream ds = GetDatastream(element, versDateTime);
            if (ds != null && (state == null || ds.DSState.equals(state))) {
                al.add(ds);
            }
        }
        return al.toArray(DATASTREAM_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getObjectHistory(String PID) {
        String[] dsIDs = ListDatastreamIDs(null);
        TreeSet<String> modDates = new TreeSet<String>();
        for (String element : dsIDs) {
            Date[] dsDates = getDatastreamVersions(element);
            for (Date element2 : dsDates) {
                modDates.add(DateUtility.convertDateToString(element2));
            }
        }
        return modDates.toArray(EMPTY_STRING_ARRAY);
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
    @Override
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
    @Override
    public boolean hasRelationship(SubjectNode subject, PredicateNode predicate, ObjectNode object) {
        return m_obj.hasRelationship(subject, predicate, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRelationship(PredicateNode predicate, ObjectNode object) {
        return m_obj.hasRelationship(predicate, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RelationshipTuple> getRelationships(SubjectNode subject,
                                                   PredicateNode predicate,
                                                   ObjectNode object) {
        return m_obj.getRelationships(subject, predicate, object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RelationshipTuple> getRelationships(PredicateNode predicate,
                                                   ObjectNode object) {
        return m_obj.getRelationships(predicate, object);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RelationshipTuple> getRelationships() {
        return m_obj.getRelationships();
    }

}
