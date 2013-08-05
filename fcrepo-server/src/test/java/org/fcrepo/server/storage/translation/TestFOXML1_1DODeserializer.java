/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import static org.fcrepo.common.Models.CONTENT_MODEL_3_0;

import java.util.Iterator;

import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.Datastream;
import org.junit.Test;


/**
 * Unit tests for FOXML1_1DODeserializer.
 *
 * @author Chris Wilper
 */
public class TestFOXML1_1DODeserializer
        extends TestFOXMLDODeserializer {

    public TestFOXML1_1DODeserializer() {
        // superclass sets protected fields
        // m_deserializer and m_serializer as given below
        super(new FOXML1_1DODeserializer(), new FOXML1_1DOSerializer());
    }
    
    //---
    // Tests
    //---

    @Test
    public void testDeserializeSimpleCModelObject() {
        doSimpleTest(CONTENT_MODEL_3_0);
    }
    
    @Test
    public void testDeserializeWithAutoChecksum() throws Exception {
        Datastream.defaultChecksumType = "MD5";
        Datastream.autoChecksum = true;
        BasicDigitalObject obj=new BasicDigitalObject();
        obj.setNew(true);
        m_deserializer.deserialize(this.getClass().getClassLoader().getResourceAsStream("ecm/dataobject1.xml"), obj, "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
        for (Iterator<String> streams=obj.datastreamIdIterator();streams.hasNext();){
            String id=streams.next();
            for (Datastream version:obj.datastreams(id)){
                assertEquals(Datastream.getDefaultChecksumType(), version.DSChecksumType);
                assertEquals(32, version.getChecksum().length());
            }
        }
    }

    @Test
    public void testDeserializeWithoutAutoChecksum() throws Exception {
        Datastream.defaultChecksumType = Datastream.CHECKSUMTYPE_DISABLED;
        Datastream.autoChecksum = false;
        BasicDigitalObject obj=new BasicDigitalObject();
        obj.setNew(true);
        m_deserializer.deserialize(this.getClass().getClassLoader().getResourceAsStream("ecm/dataobject1.xml"), obj, "UTF-8", DOTranslationUtility.DESERIALIZE_INSTANCE);
        for (Iterator<String> streams=obj.datastreamIdIterator();streams.hasNext();){
            String id=streams.next();
            for (Datastream version:obj.datastreams(id)){
                assertEquals(version.DatastreamID, Datastream.CHECKSUMTYPE_DISABLED, version.DSChecksumType);
                assertEquals(version.DatastreamID, Datastream.CHECKSUM_NONE, version.DSChecksum);
            }
        }
    }
    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_1DODeserializer.class);
    }

}
