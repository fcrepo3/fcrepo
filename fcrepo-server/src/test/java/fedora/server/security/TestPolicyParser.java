/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.security;

import java.io.IOException;

import org.junit.Test;

import org.xml.sax.SAXException;

import fedora.common.Constants;

import fedora.server.errors.ValidationException;
import fedora.server.utilities.StreamUtility;

/**
 * Unit tests for PolicyParser.
 */
public class TestPolicyParser {

    public static final String POLICY_GOODENOUGH
            = "<Policy PolicyId='foo' RuleCombiningAlgId='urn:oasis:names:tc:"
            + "xacml:1.0:rule-combining-algorithm:first-applicable'/>";

    public static final String POLICY_QUESTIONABLE
            = "<Policy unexpectedAttribute='thisOne' PolicyId='foo' RuleCombi"
            + "ningAlgId='urn:oasis:names:tc:xacml:1.0:rule-combining-algorit"
            + "hm:first-applicable'/>";

    public static final String SCHEMA_GOODENOUGH
            = "<schema xmlns='" + Constants.XML_XSD.uri + "'><element name='P"
            + "olicy'><complexType><sequence><any minOccurs='0' maxOccurs='unb"
            + "ounded' processContents='skip'/></sequence><attribute name='Pol"
            + "icyId'/><attribute name='RuleCombiningAlgId'/></complexType></e"
            + "lement></schema>";

    private static final String POLICY_BADROOT = "<not-a-policy/>";

    private static final String POLICY_GOODROOT_NOCONTENT = "<Policy/>";

    private static final String POLICY_MALFORMEDXML = "notxml";

    private static final String SCHEMA_BAD = "<not-a-schema/>";

    @Test (expected=SAXException.class)
    public void testConstructWithBadSchema() throws IOException, SAXException {
        new PolicyParser(StreamUtility.getStream(SCHEMA_BAD));
    }

    @Test
    public void testConstructWithValidSchema() throws IOException, SAXException {
        new MockPolicyParser();
    }

    @Test (expected=ValidationException.class)
    public void testParseMalformedXMLValidationFalse()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_MALFORMEDXML), false);
    }

    @Test (expected=ValidationException.class)
    public void testParseMalformedXMLValidationTrue()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_MALFORMEDXML), true);
    }

    @Test (expected=ValidationException.class)
    public void testParseBadRootValidationFalse()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_BADROOT), false);
    }

    @Test (expected=ValidationException.class)
    public void testParseBadRootValidationTrue()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_BADROOT), true);
    }

    @Test (expected=ValidationException.class)
    public void testParseGoodRootNoContentValidationFalse()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_GOODROOT_NOCONTENT), false);
    }

    @Test (expected=ValidationException.class)
    public void testParseGoodRootNoContentValidationTrue()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_GOODROOT_NOCONTENT), true);
    }

    @Test
    public void testParseGoodRootQuestionableContentValidationFalse()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_QUESTIONABLE), false);
    }

    @Test (expected=ValidationException.class)
    public void testParseGoodRootQuestionableContentValidationTrue()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_QUESTIONABLE), true);
    }

    @Test
    public void testParseGoodRootGoodContentValidationFalse()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_GOODENOUGH), false);
    }

    @Test
    public void testParseGoodRootGoodContentValidationTrue()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.parse(StreamUtility.getStream(POLICY_GOODENOUGH), true);
    }

    @Test
    public void testParseGoodRootGoodContentValidationTrueWithCopy()
            throws IOException, SAXException, ValidationException {
        PolicyParser parser = new MockPolicyParser();
        parser.copy().parse(StreamUtility.getStream(POLICY_GOODENOUGH), true);
    }

    // Supports legacy test runners
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestPolicyParser.class);
    }
}
