/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.util.Date;

import org.xml.sax.InputSource;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.DatastreamNotFoundException;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.RepositoryConfigurationException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.errors.UnsupportedTranslationException;
import org.fcrepo.server.storage.service.ServiceMapper;
import org.fcrepo.server.storage.translation.DOTranslator;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.storage.types.MethodDef;


/**
 * A Service Definition Reader based on a DigitalObject.
 * 
 * @author Chris Wilper
 */
public class SimpleServiceDefinitionReader
        extends SimpleServiceAwareReader
        implements ServiceDefinitionReader {

    private final ServiceMapper serviceMapper;

    public SimpleServiceDefinitionReader(Context context,
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
        serviceMapper = new ServiceMapper(GetObjectPID());
    }

    /**
     * Alternate constructor for when a DigitalObject is already available for
     * some reason.
     */
    public SimpleServiceDefinitionReader(Context context,
                            RepositoryReader repoReader,
                            DOTranslator translator,
                            String exportFormat,
                            String encoding,
                            DigitalObject obj) {
        super(context, repoReader, translator, exportFormat, encoding, obj);
        serviceMapper = new ServiceMapper(GetObjectPID());
    }

    public MethodDef[] getAbstractMethods(Date versDateTime)
            throws DatastreamNotFoundException, ObjectIntegrityException,
            RepositoryConfigurationException, GeneralException {
        return serviceMapper
                .getMethodDefs(new InputSource(new ByteArrayInputStream(getMethodMapDatastream(versDateTime).xmlContent)));
    }

    public InputStream getAbstractMethodsXML(Date versDateTime)
            throws DatastreamNotFoundException, ObjectIntegrityException {
        return new ByteArrayInputStream(getMethodMapDatastream(versDateTime).xmlContent);
    }

}
