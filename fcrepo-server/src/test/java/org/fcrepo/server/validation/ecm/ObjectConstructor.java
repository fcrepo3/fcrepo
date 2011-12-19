package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.errors.ObjectIntegrityException;
import org.fcrepo.server.errors.StreamIOException;
import org.fcrepo.server.storage.translation.DOTranslationUtility;
import org.fcrepo.server.storage.translation.FOXML1_1DODeserializer;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.DigitalObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jun 26, 2010
 * Time: 1:04:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectConstructor {
    static String DCBEGIN =
            "                <oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n";
    static String DCEND = "                </oai_dc:dc>";


    public static DigitalObject producefedoraObject30()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream(
                "src/main/resources/utilities/server/org/fcrepo/server/resources/fedora-system_FedoraObject-3.0.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;

    }

    public static DigitalObject produceContentModel30()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream(
                "src/main/resources/utilities/server/org/fcrepo/server/resources/fedora-system_ContentModel-3.0.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;

    }

    public static DigitalObject produceServiceDef30()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream(
                "src/main/resources/utilities/server/org/fcrepo/server/resources/fedora-system_ServiceDefinition-3.0.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;

    }

    public static DigitalObject produceServiceDep30()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream(
                "src/main/resources/utilities/server/org/fcrepo/server/resources/fedora-system_ServiceDeployment-3.0.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;

    }


    public static DigitalObject produceContentModel1()
            throws ObjectIntegrityException, StreamIOException, UnsupportedEncodingException, FileNotFoundException {

        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/contentmodel1.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;

    }


    public static DigitalObject produceContentModel2()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/contentmodel2.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }


    public static DigitalObject produceContentModel3()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/contentmodel3.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }


    public static DigitalObject produceDataObject1()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject1.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }


    public static DigitalObject produceDataObject2()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject2.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }

    public static DigitalObject produceDataObject3()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject3.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }

    public static DigitalObject produceDataObject5()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject5.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }

    public static DigitalObject produceDataObject6()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject6.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }

    public static DigitalObject produceDataObject7()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject7.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }

    public static DigitalObject produceDataObject8()
            throws FileNotFoundException, ObjectIntegrityException, StreamIOException, UnsupportedEncodingException {
        FOXML1_1DODeserializer deserialiser = new FOXML1_1DODeserializer();
        BasicDigitalObject object = new BasicDigitalObject();
        FileInputStream in = new FileInputStream("src/test/resources/ecm/dataobject8.xml");
        deserialiser.deserialize(in, object, "UTF-8", DOTranslationUtility.AS_IS);
        return object;
    }


}
