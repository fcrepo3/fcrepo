/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.fcrepo.common.Constants;
import org.fcrepo.common.FedoraTestConstants;
import org.fcrepo.server.validation.DOValidatorXMLSchema;

import junit.framework.JUnit4TestAdapter;


/**
 * @author Edwin Shin
 * @version $Id$
 */
public class DOValidatorXMLSchemaTest
        implements FedoraTestConstants {

    private static String RESOURCES = "src/main/resources/";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    private static File getDemoFile(String path) {
        return new File(DEMO_DIR_PREFIX + path);
    }

    @Test
    public void testFoxmlValidation() throws Exception {
        InputStream in =
                new FileInputStream(RESOURCES + "demo/demo-objects/foxml/local-server-demos/simple-image-demo/obj_demo_5.xml");
        DOValidatorXMLSchema dov =
                new DOValidatorXMLSchema(RESOURCES + "xsd/foxml1-1.xsd");
        dov.validate(in);
    }

    @Test
    public void testMetsValidation() throws Exception {
        InputStream in =
                new FileInputStream(getDemoFile("mets/local-server-demos/simple-image-demo/obj_demo_5.xml"));
        DOValidatorXMLSchema dov =
                new DOValidatorXMLSchema(RESOURCES + "xsd/mets-fedora-ext1-1.xsd");
        dov.validate(in);
    }

    @Test
    public void testAtomValidation() throws Exception {
        InputStream in =
                new FileInputStream(getDemoFile("atom/local-server-demos/simple-image-demo/obj_demo_5.xml"));
        DOValidatorXMLSchema dov = new DOValidatorXMLSchema(RESOURCES + "xsd/atom.xsd");
        dov.validate(in);

    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(DOValidatorXMLSchemaTest.class);
    }
}
