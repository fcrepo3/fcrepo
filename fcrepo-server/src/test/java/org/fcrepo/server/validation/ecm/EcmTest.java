package org.fcrepo.server.validation.ecm;

import junit.framework.TestCase;
import org.fcrepo.server.storage.MockRepositoryReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.utilities.DateUtility;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.fcrepo.server.utilities.StreamUtility.enc;

public class EcmTest extends TestCase {


    RepositoryReader reader;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MockRepositoryReader mockRepositoryReader = new MockRepositoryReader();
        mockRepositoryReader.putObject(ObjectConstructor.produceContentModel1());
        mockRepositoryReader.putObject(ObjectConstructor.produceContentModel2());
        mockRepositoryReader.putObject(ObjectConstructor.produceContentModel30());
        mockRepositoryReader.putObject(ObjectConstructor.producefedoraObject30());
        mockRepositoryReader.putObject(ObjectConstructor.produceDataObject1());
        mockRepositoryReader.putObject(ObjectConstructor.produceDataObject2());
        mockRepositoryReader.putObject(ObjectConstructor.produceDataObject3());

        mockRepositoryReader.putObject(ObjectConstructor.produceDataObject5());
        mockRepositoryReader.putObject(ObjectConstructor.produceDataObject6());
        mockRepositoryReader.putObject(ObjectConstructor.produceDataObject7());
        reader = mockRepositoryReader;
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testEcm1() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);

        Validation validation1 = ecm.validate(null, "demo:dataObject1", null);
        assertTrue("Dataobject1 failed validation: " + objectValidationToXml(validation1), validation1.isValid());

    }

    @Test
    public void testEcm2() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);


        Validation validation2 = ecm.validate(null, "demo:dataObject2", null);
        assertTrue("Dataobject2 failed validation: " + objectValidationToXml(validation2), validation2.isValid());

    }

    @Test
    public void testEcm3() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);

        Validation validation3 = ecm.validate(null, "demo:contentModel1", null);
        assertTrue("contentmodel1 failed validation: " + objectValidationToXml(validation3), validation3.isValid());

    }

    @Test
    public void testEcm4() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);

        Validation validation3 = ecm.validate(null, "demo:dataObject3", null);
        assertFalse("DataObject3 succeeded validation: " + objectValidationToXml(validation3), validation3.isValid());

    }

    @Test
    public void testEcm5() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);

        Validation validation3 = ecm.validate(null, "demo:dataObject5", null);
        assertFalse("DataObject5 succeeded validation: " + objectValidationToXml(validation3), validation3.isValid());

    }

    @Test
    public void testEcm6() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);

        Validation validation3 = ecm.validate(null, "demo:dataObject6", null);
        assertFalse("DataObject6 succeeded validation: " + objectValidationToXml(validation3), validation3.isValid());

    }

    @Test
    public void testEcm7() throws Exception {
        EcmValidator ecm = new EcmValidator(reader,null);

        Validation validation3 = ecm.validate(null, "demo:dataObject7", null);
        assertFalse("DataObject7 succeeded validation: " + objectValidationToXml(validation3), validation3.isValid());

    }


    public String objectValidationToXml(Validation validation) {
        StringBuilder buffer = new StringBuilder();
        String pid = validation.getPid();
        Date date = validation.getAsOfDateTime();
        String dateString = "";
        boolean valid = validation.isValid();
        if (date != null) {
            dateString = DateUtility.convertDateToString(date);
        }
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buffer.append("<validation "
                      + "pid=\"" + enc(pid) + "\" " +
                      "valid=\"" + valid + "\">\n");
        buffer.append("  <asOfDateTime>" + dateString + "</asOfDateTime>\n");
        buffer.append("  <contentModels>\n");
        for (String model : validation.getContentModels()) {
            buffer.append("    <model>");
            buffer.append(enc(model));
            buffer.append("</model>\n");
        }
        buffer.append("  </contentModels>\n");

        buffer.append("  <problems>\n");
        for (String problem : validation.getObjectProblems()) {
            buffer.append("    <problem>");
            buffer.append(problem);
            buffer.append("</problem>\n");
        }
        buffer.append("  </problems>\n");

        buffer.append("  <datastreamProblems>\n");
        Map<String, List<String>> dsprobs = validation.getDatastreamProblems();
        for (String ds : dsprobs.keySet()) {
            List<String> problems = dsprobs.get(ds);
            buffer.append("    <datastream");
            buffer.append(" datastreamID=\"");
            buffer.append(ds);
            buffer.append("\">\n");
            for (String problem : problems) {
                buffer.append("      <problem>");
                buffer.append(problem);
                buffer.append("</problem>\n");
            }
            buffer.append("    </datastream>");
        }
        buffer.append("  </datastreamProblems>\n");
        buffer.append("</validation>");
        return buffer.toString();
    }


}
