/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage.translation;

import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import org.fcrepo.server.storage.translation.FOXML1_1DODeserializer;
import org.fcrepo.server.storage.translation.FOXML1_1DOSerializer;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.Datastream;

import static org.fcrepo.common.Models.CONTENT_MODEL_3_0;


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
        System.out.println("Set properties");
        if (System.getProperty("fedora.hostname") == null) {
            System.setProperty("fedora.hostname","localhost");
        }
        if (System.getProperty("fedora.port") == null) {
            System.setProperty("fedora.port","1024");
        }
        if (System.getProperty("fedora.appServerContext") == null) {
            System.setProperty("fedora.appServerContext","fedora");
        }
    }
    
    //---
    // Tests
    //---

    @Test
    public void testDeserializeSimpleCModelObject() {
        doSimpleTest(CONTENT_MODEL_3_0);
    }
    
    @Test
    public void testDefaultChecksum() throws Exception {
        BasicDigitalObject obj = new BasicDigitalObject();
        obj.setNew(true);
        Datastream.defaultChecksumType="MD5";
        Datastream.autoChecksum=true;
        m_deserializer.deserialize(
            this.getClass().getClassLoader().getResourceAsStream("ecm/dataobject1.xml"),
            obj, "UTF-8", 0);
        for (Iterator<String> streams=obj.datastreamIdIterator();streams.hasNext();){
            String id=streams.next();
            for (Datastream version:obj.datastreams(id)){
                assertTrue(version.DSChecksumType == Datastream.getDefaultChecksumType());
                assertTrue(version.getChecksum().length() == 32);
            }
        }

        Datastream.defaultChecksumType="MD5";
        Datastream.autoChecksum=false;
        obj = new BasicDigitalObject();
        m_deserializer.deserialize(
            this.getClass().getClassLoader().getResourceAsStream("ecm/dataobject1.xml"),
            obj, "UTF-8", 0);
        for (Iterator<String> streams=obj.datastreamIdIterator();streams.hasNext();){
            String id=streams.next();
            for (Datastream version:obj.datastreams(id)){
                assertEquals(Datastream.CHECKSUMTYPE_DISABLED, version.DSChecksumType);
                assertEquals(Datastream.CHECKSUM_NONE, version.DSChecksum);
            }
        }
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestFOXML1_1DODeserializer.class);
    }

}
