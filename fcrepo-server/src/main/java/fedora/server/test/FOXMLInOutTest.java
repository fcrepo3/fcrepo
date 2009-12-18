/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;

import junit.framework.TestCase;

import fedora.common.Constants;

import fedora.server.errors.ServerException;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOSerializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.translation.DOTranslatorImpl;
import fedora.server.storage.translation.FOXML1_1DODeserializer;
import fedora.server.storage.translation.FOXML1_1DOSerializer;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;

/**
 * Tests the FOXML deserializer and serializer by parsing a FOXML input file and
 * re-serializing it in the storage context.
 * 
 * @author Sandy Payette
 */
public class FOXMLInOutTest
        extends TestCase
        implements Constants {

    protected File inFile = null;

    protected File outFile = null;

    protected DigitalObject obj = null;

    @Override
    protected void setUp() {
        //inFile=new File("TestIngestFiles/foxml-reference-example.xml");
        inFile = new File("TestIngestFiles/foxml-simple-nodissem.xml");
        outFile = new File("TestExportFiles/STORE-foxml.xml");
        System.setProperty("fedoraServerHost", "localhost");
        System.setProperty("fedoraServerPort", "8080");

        FileInputStream in = null;
        try {
            in = new FileInputStream(inFile);
        } catch (IOException ioe) {
            System.out.println("Error on FOXML file inputstream: "
                    + ioe.getMessage());
            ioe.printStackTrace();
        }
        try {
            // setup    
            FOXML1_1DODeserializer deser = new FOXML1_1DODeserializer();
            FOXML1_1DOSerializer ser = new FOXML1_1DOSerializer();
            HashMap<String, DODeserializer> desermap = new HashMap<String, DODeserializer>();
            HashMap<String, DOSerializer> sermap = new HashMap<String, DOSerializer>();
            desermap.put(FOXML1_1.uri, deser);
            DOTranslatorImpl trans = new DOTranslatorImpl(sermap, desermap);
            obj = new BasicDigitalObject();

            // deserialize input XML
            System.out.println("Deserializing...");
            trans.deserialize(in,
                              obj,
                              FOXML1_1.uri,
                              "UTF-8",
                              DOTranslationUtility.DESERIALIZE_INSTANCE);
            System.out.println("Digital Object PID= " + obj.getPid());
            // serialize
            sermap.put(FOXML1_1.uri, ser);
            System.out.println("Re-serializing...");
            System.out.println("Writing file to... " + outFile.getPath());
            FileOutputStream out = new FileOutputStream(outFile);
            // re-serialize (either for the EXPORT or STORAGE context)
            int m_transContext = DOTranslationUtility.SERIALIZE_EXPORT_MIGRATE;
            trans.serialize(obj, out, FOXML1_1.uri, "UTF-8", m_transContext);
            System.out.println("Done. Serialized for context: "
                    + m_transContext);
        } catch (ServerException e) {
            System.out
                    .println("ServerException: suppressing info not available without running server.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception: (" + e.getClass().getName() + "):"
                    + e.getMessage());

        }
    }

    public void testDigitalObject() {
        //assertNotNull("Failure: digital object PID is null.", obj.getPid());
        //assertNotNull("Failure: digital object audit record set is null.", obj.getAuditRecords());
        //assertNotNull("Failure: digital object cmodel is null.", obj.getContentModelId());
        //assertNotNull("Failure: digital object createDate is null.", obj.getCreateDate());    
    }
}
