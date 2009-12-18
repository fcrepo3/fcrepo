/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.io.IOException;

import org.junit.Test;

import org.xml.sax.SAXException;

import junit.framework.JUnit4TestAdapter;

import fedora.common.FaultException;
import fedora.common.PID;

import fedora.server.ReadOnlyContext;
import fedora.server.errors.ValidationException;
import fedora.server.security.MockPolicyParser;
import fedora.server.security.PolicyParser;
import fedora.server.storage.DOReader;
import fedora.server.storage.MockRepositoryReader;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.ObjectBuilder;
import fedora.server.utilities.StreamUtility;

import static fedora.server.security.TestPolicyParser.POLICY_GOODENOUGH;
import static fedora.server.security.TestPolicyParser.POLICY_QUESTIONABLE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for ValidationUtility.
 */
public class ValidationUtilityTest {

    private static final String TEST_PID = "test:1";

    private static final String RELSEXT_GOOD
            = "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#\'\n"
            + "       xmlns:rel='info:fedora/fedora-system:def/relations-external#'>\n"
            + "  <rdf:Description rdf:about='info:fedora/" + TEST_PID + "'>\n"
            + "     <rel:isMemberOf rdf:resource='info:fedora/test:X'/>\n"
            + "  </rdf:Description>\n"
            + "</rdf:RDF>";

    private static final String RELSINT_GOOD
    = "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#\'\n"
    + "       xmlns:foo='http://www.example.org/bar#'>\n"
    + "  <rdf:Description rdf:about='info:fedora/" + TEST_PID + "/DS1" + "'>\n"
    + "     <foo:baz rdf:resource='info:fedora/test:X'/>\n"
    + "  </rdf:Description>\n"
    + "  <rdf:Description rdf:about='info:fedora/" + TEST_PID + "/DS2" + "'>\n"
    + "     <foo:qux>quux</foo:qux>\n"
    + "  </rdf:Description>\n"
    + "</rdf:RDF>";

    private final String tmpDir = System.getProperty("java.io.tmpdir");

    @Test
    public void testValidUrls() throws Exception {
        String[] urls_managed = {"http://localhost",
                         "http://localhost:8080",
                         "uploaded:///tmp/foo.xml",
                         "file:///etc/passwd",
                         "file:/etc/passwd"
                        };
        for (String url : urls_managed) {
            ValidationUtility.validateURL(url,"M");
        }
        
        String[] urls = {"http://localhost",
                        "http://localhost:8080",
                        "uploaded:///tmp/foo.xml",
                        };
        
        for (String url : urls) {
           ValidationUtility.validateURL(url,"M");
        }
    }

    @Test
    public void testInvalidUrls(){
        String[] urls_management = {"", "a",
                         "temp:///etc/passwd",
                         "copy:///etc/passwd",
                         "temp://" + tmpDir + "/../etc/passwd",
                         "temp://" + tmpDir + "/../../etc/passwd",
                         "/etc/passwd",
                         "../../etc/passwd"};

        for (String url : urls_management) {
            try {
                ValidationUtility.validateURL(url,"M");
            } catch (Exception e) {
                assertTrue("Expected Exception of type "
                        + ValidationException.class.getName() + " but got: "
                        + e.getClass().getName(), e.getClass().getName()
                        .equals(ValidationException.class.getName()));
                continue;
            }
            fail("Expected " + ValidationException.class.getName()
                    + " for URL:" + url + " but got none");
        }

        String[] urls = {"", "a",
                "temp:///etc/passwd",
                "copy:///etc/passwd",
                "temp://" + tmpDir + "/../etc/passwd",
                "temp://" + tmpDir + "/../../etc/passwd",
                "/etc/passwd",
                "../../etc/passwd",
                "file:///etc/passwd",
                "file:/etc/passwd"};

        for (String url : urls) {
            try {
                ValidationUtility.validateURL(url,"R");
            } catch (Exception e) {
                assertTrue("Expected Exception of type "
                        + ValidationException.class.getName() + " but got: "
                        + e.getClass().getName(), e.getClass().getName()
                        .equals(ValidationException.class.getName()));
                continue;
            }
            fail("Expected " + ValidationException.class.getName()
                    + " for URL:" + url + " but got none");
        }

    }

    @Test(expected=NullPointerException.class)
    public void testValidatePolicyParserNotSet()
            throws IOException, SAXException, ValidationException {
        validatePolicy(null, POLICY_GOODENOUGH);
    }

    @Test(expected=ValidationException.class)
    public void testValidatePolicyBad()
            throws IOException, SAXException, ValidationException {
        validatePolicy(new MockPolicyParser(), POLICY_QUESTIONABLE);
    }

    @Test
    public void testValidatePolicyGood()
            throws IOException, SAXException, ValidationException {
        validatePolicy(new MockPolicyParser(), POLICY_GOODENOUGH);
    }

    @Test(expected=ValidationException.class)
    public void testValidateRelsExtBad() throws ValidationException {
        validateRels("RELS-EXT", "");
    }

    @Test(expected=ValidationException.class)
    public void testValidateRelsIntBad() throws ValidationException {
        validateRels("RELS-INT", "");
    }

    @Test
    public void testValidateRelsExtGood() throws ValidationException {
        validateRels("RELS-EXT", RELSEXT_GOOD);
    }

    @Test
    public void testValidateRelsIntGood() throws ValidationException {
        validateRels("RELS-INT", RELSINT_GOOD);
    }

    @Test
    public void testValidateReservedNone() throws ValidationException {
        validateReserved(null, new String[] { });
    }

    @Test(expected=ValidationException.class)
    public void testValidateReservedPolicyBad()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "POLICY", POLICY_QUESTIONABLE });
    }

    @Test
    public void testValidateReservedPolicyGood()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "POLICY", POLICY_GOODENOUGH });
    }

    @Test(expected=ValidationException.class)
    public void testValidateReservedRelsExtBad()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "RELS-EXT", "" });
    }

    @Test(expected=ValidationException.class)
    public void testValidateReservedRelsIntBad()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "RELS-INT", "" });
    }
    @Test
    public void testValidateReservedRelsExtGood()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "RELS-EXT", RELSEXT_GOOD });
    }

    @Test
    public void testValidateReservedRelsIntGood()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "RELS-INT", RELSINT_GOOD });
    }

    @Test
    public void testValidateReservedAllGood()
            throws IOException, SAXException, ValidationException {
        validateReserved(new MockPolicyParser(),
                         new String[] { "POLICY", POLICY_GOODENOUGH,
                                        "RELS-EXT", RELSEXT_GOOD,
                                        "RELS-INT", RELSINT_GOOD});
    }

    private static void validatePolicy(PolicyParser parser, String policy)
            throws IOException, SAXException, ValidationException {
        ValidationUtility.setPolicyParser(parser);
        ValidationUtility.validateReservedDatastream(PID.getInstance(TEST_PID),
                                                     "POLICY",
                                                     StreamUtility.getStream(policy));
    }

    private static void validateRels(String dsId, String rels)
            throws ValidationException {
        ValidationUtility.validateReservedDatastream(PID.getInstance(TEST_PID),
                                                     dsId,
                                                     StreamUtility.getStream(rels));
    }

    private static void validateReserved(PolicyParser parser, String[] dsData)
            throws ValidationException {
        ValidationUtility.setPolicyParser(parser);
        ValidationUtility.validateReservedDatastreams(getDOReader(dsData));
    }

    private static DOReader getDOReader(String[] dsData) {
        MockRepositoryReader repo = new MockRepositoryReader();
        try {
            DigitalObject obj = new BasicDigitalObject();
            obj.setPid(TEST_PID);
            for (int i = 0; i < dsData.length; i+=2) {
                ObjectBuilder.addXDatastream(obj, dsData[i], dsData[i+1]);
            }
            repo.putObject(obj);
            DOReader reader = repo.getReader(false, ReadOnlyContext.EMPTY, TEST_PID);
            return reader;
        } catch (Exception wontHappen) {
            throw new FaultException(wontHappen);
        }
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ValidationUtilityTest.class);
    }
}
