/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import fedora.server.validation.DOValidatorSchematron;
import fedora.server.validation.DOValidatorSchematronResult;

/**
 * @author Sandy Payette
 */
public class ValidateSchematronTest
        extends TestCase {

    protected String inFile = null;

    protected String inSchematronPPFile = null;

    protected String inSchematronRulesFile = null;

    protected String tempdir = null;

    protected DOValidatorSchematronResult result = null;

    @Override
    protected void setUp() {
        tempdir = "TestValidation";
        inSchematronPPFile = "server/src/main/resources/schematron/preprocessor.xslt";

        // FOXML
        inFile = "TestIngestFiles/foxml-reference-ingest.xml";
        inSchematronRulesFile = "server/src/main/resources/schematron/foxmlRules1-0.xml";

        FileInputStream in = null;
        try {
            in = new FileInputStream(new File(inFile));
        } catch (IOException ioe) {
            System.out.println("Error on XML file inputstream: "
                    + ioe.getMessage());
            ioe.printStackTrace();
        }

        try {
            DOValidatorSchematron dovs =
                    new DOValidatorSchematron(inSchematronRulesFile,
                                              inSchematronPPFile,
                                              "ingest");
            dovs.validate(in);
        } catch (Exception e) {
            System.out.println("Error: (" + e.getClass().getName() + "):"
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    public void testFoo() {
        //assertNotNull("Failure: foo is null.", foo.getA());
    }
}
