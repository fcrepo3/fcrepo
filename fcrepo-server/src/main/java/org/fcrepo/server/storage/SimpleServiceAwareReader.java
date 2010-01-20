/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.InputStream;

import java.util.Date;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.UnsupportedTranslationException;
import org.fcrepo.server.storage.translation.DOTranslator;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.DatastreamXMLMetadata;
import org.fcrepo.server.storage.types.DigitalObject;


/**
 * DOReader that knows about WSDL, method maps, and DS input specs.
 * 
 * @author Chris Wilper
 */
public class SimpleServiceAwareReader
        extends SimpleDOReader {

    public SimpleServiceAwareReader(Context context,
                                    RepositoryReader repoReader,
                                    DOTranslator translator,
                                    String exportFormat,
                                    String storageFormat,
                                    String encoding,
                                    InputStream serializedObject)
            throws ObjectIntegrityException, StreamIOException,
            UnsupportedTranslationException, ServerException {
        super(context,
              repoReader,
              translator,
              exportFormat,
              storageFormat,
              encoding,
              serializedObject);
    }

    /**
     * Alternate constructor for when a DigitalObject is already available for
     * some reason.
     */
    public SimpleServiceAwareReader(Context context,
                                    RepositoryReader repoReader,
                                    DOTranslator translator,
                                    String exportFormat,
                                    String encoding,
                                    DigitalObject obj) {
        super(context, repoReader, translator, exportFormat, encoding, obj);
    }

    protected DatastreamXMLMetadata getWSDLDatastream(Date versDateTime)
            throws DatastreamNotFoundException, ObjectIntegrityException {
        Datastream ds = GetDatastream("WSDL", versDateTime);
        if (ds == null) {
            throw new DatastreamNotFoundException("The object, "
                    + GetObjectPID() + " does not have a WSDL datastream"
                    + " existing at " + getWhenString(versDateTime));
        }
        DatastreamXMLMetadata wsdlDS = null;
        try {
            wsdlDS = (DatastreamXMLMetadata) ds;
        } catch (Throwable th) {
            throw new ObjectIntegrityException("The object, " + GetObjectPID()
                    + " has a WSDL datastream existing at "
                    + getWhenString(versDateTime) + ", but it's not an "
                    + "XML metadata datastream");
        }
        return wsdlDS;
    }

    protected DatastreamXMLMetadata getMethodMapDatastream(Date versDateTime)
            throws DatastreamNotFoundException, ObjectIntegrityException {
        Datastream ds = GetDatastream("METHODMAP", versDateTime);
        if (ds == null) {
            throw new DatastreamNotFoundException("The object, "
                    + GetObjectPID() + " does not have a METHODMAP datastream"
                    + " existing at " + getWhenString(versDateTime));
        }
        DatastreamXMLMetadata mmapDS = null;
        try {
            mmapDS = (DatastreamXMLMetadata) ds;
        } catch (Throwable th) {
            throw new ObjectIntegrityException("The object, " + GetObjectPID()
                    + " has a METHODMAP datastream existing at "
                    + getWhenString(versDateTime) + ", but it's not an "
                    + "XML metadata datastream");
        }
        return mmapDS;
    }

    protected DatastreamXMLMetadata getDSInputSpecDatastream(Date versDateTime)
            throws DatastreamNotFoundException, ObjectIntegrityException {
        Datastream ds = GetDatastream("DSINPUTSPEC", versDateTime);
        if (ds == null) {
            throw new DatastreamNotFoundException("The object, "
                    + GetObjectPID()
                    + " does not have a DSINPUTSPEC datastream"
                    + " existing at " + getWhenString(versDateTime));
        }
        DatastreamXMLMetadata dsInSpecDS = null;
        try {
            dsInSpecDS = (DatastreamXMLMetadata) ds;
        } catch (Throwable th) {
            throw new ObjectIntegrityException("The object, " + GetObjectPID()
                    + " has a DSINPUTSPEC datastream existing at "
                    + getWhenString(versDateTime) + ", but it's not an "
                    + "XML metadata datastream");
        }
        return dsInSpecDS;
    }
}
