/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.validation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Collection;
import java.util.HashSet;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;

import org.trippi.RDFFormat;
import org.trippi.TripleIterator;

import junit.framework.TestCase;

import fedora.common.Constants;
import fedora.common.PID;
import fedora.common.rdf.SimpleLiteral;
import fedora.common.rdf.SimpleTriple;
import fedora.common.rdf.SimpleURIReference;

import fedora.server.errors.GeneralException;
import fedora.server.errors.ServerException;
import fedora.server.errors.ValidationException;

/**
 * Tests the RELS-EXT and RELS-INT datastream deserializer and validation.
 *
 * @author Edwin Shin
 * @author Stephen Bayliss
 */
public class RelsValidatorTest
        extends TestCase {

    private Collection<Triple> triples;

    private static byte[] RELS_EXT;
    private static byte[] RELS_INT;


    private PID pid;

    static {
        // create valid RELS-EXT and RELS-INT contents
        StringBuilder sbRelsExt  = new StringBuilder();
        StringBuilder sbRelsInt = new StringBuilder();
        sbRelsExt
                .append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                        + "         xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">"
                        + "  <rdf:Description rdf:about=\"info:fedora/demo:888\">"
                        + "     <rel:isMemberOf rdf:resource=\"info:fedora/demo:X\" />"
                        + "  </rdf:Description>" + "</rdf:RDF>");
        sbRelsInt
                .append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                        + "         xmlns:relint=\"http://www.example.org/rels-int#\">"
                        + "  <rdf:Description rdf:about=\"info:fedora/demo:888/DS1\">"
                        + "     <relint:someRelation rdf:resource=\"info:fedora/demo:X\" />"
                        + "  </rdf:Description>"
                        + "  <rdf:Description rdf:about=\"info:fedora/demo:888/DS2\">"
                        + "     <relint:someProperty>value</relint:someProperty>"
                        + "  </rdf:Description>"

                        + "</rdf:RDF>");

        try {
            RELS_EXT = sbRelsExt.toString().getBytes("UTF-8");
            RELS_INT = sbRelsInt.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
    }

    @Override
    public void setUp() {
        triples = new HashSet<Triple>();
    }

    public void testValidateValidRelsExt() throws Exception {
        pid = PID.getInstance("demo:888");
        InputStream in = new ByteArrayInputStream(RELS_EXT);
        RelsValidator validator = new RelsValidator();
        validator.validate(pid, "RELS-EXT", in);
    }
    public void testValidateValidRelsInt() throws Exception {
        pid = PID.getInstance("demo:888");
        InputStream in = new ByteArrayInputStream(RELS_INT);
        RelsValidator validator = new RelsValidator();
        validator.validate(pid, "RELS-INT", in);
    }
    public void testValidateInvalidRelsExt() throws Exception {
        pid = PID.getInstance("demo:888");
        // use RELS-INT sample as a RELS-EXT datastream
        InputStream in = new ByteArrayInputStream(RELS_INT);
        RelsValidator validator = new RelsValidator();
        try {
            validator.validate(pid, "RELS-EXT", in);
            fail("Multiple Description elements not allowed for RELS-EXT");
        } catch (ValidationException e) {}
    }

    public void testEmpty() throws Exception {
        testEmpty("RELS-EXT");
        testEmpty("RELS-INT");
    }

    private void testEmpty(String dsId) throws Exception {
        pid = PID.getInstance("demo:demo");
        InputStream in;
        String[] empties = {"", " ", "</>"};

        for (String s : empties) {
            in = new ByteArrayInputStream(s.getBytes());

            try {
                new RelsValidator().validate(pid, dsId, in);
                fail("Empty " + dsId + " datastream incorrectly passed"
                     + " validation: \"" + s + "\"");
            } catch (ValidationException e) {}
        }
    }

    public void testBadSubjectURI() throws Exception {
        pid = PID.getInstance("demo:foo");

        String s, p, o;

        s = PID.getInstance("demo:bar").toURI();
        p = "urn:p";
        o = "urn:o";
        triples.add(createTriple(s, p, o, false, null));

        try {
            validateAndClear("RELS-EXT");
            fail("RELS-EXT Assertion's subject URI different from digital object URI not allowed");
        } catch (ValidationException e) {
        }

        s = PID.getInstance("demo:foo").toURI();
        triples.add(createTriple(s, p, o, false, null));

        try {
            validateAndClear("RELS-INT");
            fail("RELS-INT Assertion's subject URI is digital object URI not allowed");
        } catch (ValidationException e) {
        }

        s = PID.getInstance("demo:bar").toURI() + "/DS1";
        triples.add(createTriple(s, p, o, false, null));

        try {
            validateAndClear("RELS-INT");
            fail("RELS-INT Assertion's subject URI is datastream in different digital object not allowed");
        } catch (ValidationException e) {
        }

        s = PID.getInstance("demo:foo").toURI() + "/";
        triples.add(createTriple(s, p, o, false, null));

        try {
            validateAndClear("RELS-INT");
            fail("RELS-INT Assertion's subject URI zero length datastream ID not allowed");
        } catch (ValidationException e) {
        }

        s = PID.getInstance("demo:foo") + "/DS1:Test";
        triples.add(createTriple(s, p, o, false, null));

        try {
            validateAndClear("RELS-INT");
            fail("RELS-INT Assertion's subject URI invalid datastream ID not allowed (colon)");
        } catch (ValidationException e) {
        }


    }
    public void testBadAssertions() throws Exception {
        testBadAssertions("RELS-EXT");
        testBadAssertions("RELS-INT");
    }

    // assertions that are invalid for both RELS-EXT and RELS-INT
    private void testBadAssertions(String dsId) throws Exception {
        pid = PID.getInstance("demo:foo");

        String p, o;

        // Model namespace
        p = Constants.MODEL.CONTROL_GROUP.uri;
        o = "demo:baz";
        triples.add(createTriple(pid, p, o, false, null));

        try {
            validateAndClear(dsId);
            fail(dsId + "Fedora Model namespace assertions not allowed");
        } catch (ValidationException e) {
        }
        // View namespace
        p = Constants.VIEW.DISSEMINATES.uri;
        triples.add(createTriple(pid, p, o, false, null));

        try {
            validateAndClear(dsId);
            fail(dsId + "Fedora View namespace assertions not allowed");
        } catch (ValidationException e) {
        }


    }

    // RELS-EXT specific assertions (RELS-INT allows these)
    public void testAssertionsRelsExt() throws Exception{
        pid = PID.getInstance("demo:foo");

        String p, o;

        p = "http://purl.org/dc/elements/1.1/title";
        o = "The God of Small Things";
        triples.add(createTriple(pid, p, o, true, null));

        try {
            validateAndClear("RELS-EXT");
            fail("RELS-EXT Dublin Core assertions not allowed");
        } catch (ValidationException e) {
        }

        // specific model relationships are allowed
        o = "urn:xyz";
        p = Constants.MODEL.HAS_SERVICE.uri;
        triples.add(createTriple(pid, p, o, false, null));
        try {
            validateAndClear("RELS-EXT");
        } catch (ValidationException e) {
            fail("RELS-EXT Model relationship " + p + " should be allowed");
        }

        p = Constants.MODEL.IS_CONTRACTOR_OF.uri;
        triples.add(createTriple(pid, p, o, false, null));
        try {
            validateAndClear("RELS-EXT");
        } catch (ValidationException e) {
            fail("RELS-EXT Model relationship " + p + " should be allowed");
        }

        p = Constants.MODEL.HAS_MODEL.uri;
        triples.add(createTriple(pid, p, o, false, null));
        try {
            validateAndClear("RELS-EXT");
        } catch (ValidationException e) {
            fail("RELS-EXT Model relationship " + p + " should be allowed");
        }

        p = Constants.MODEL.IS_DEPLOYMENT_OF.uri;
        triples.add(createTriple(pid, p, o, false, null));
        try {
            validateAndClear("RELS-EXT");
        } catch (ValidationException e) {
            fail("RELS-EXT Model relationship " + p + " should be allowed");
        }


    }

    // probably overkill to run these tests for RELS-EXT and RELS-INT, but just in case ...

    public void testResourceURI() throws Exception {
        testResourceURI("RELS-EXT");
        testResourceURI("RELS-INT");
    }
    private void testResourceURI(String dsId) throws Exception {
        pid = PID.getInstance("demo:foo");
        String s, p, o;

        if (dsId.equals("RELS-INT")) {
            s = pid.toURI() + "/DS1";
        } else {
            s = pid.toURI();
        }

        p = "urn:p";
        o = "urn:o";
        triples.add(createTriple(s, p, o, false, null));
        p = "urn:p";
        o = "urn:o";
        triples.add(createTriple(s, p, o, true, null));
        p = "urn:p";
        o = "1970-01-01T00:00:00Z";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        p = "urn:p";
        o = s;
        triples.add(createTriple(s, p, o, false, null));

        try {
            validateAndClear(dsId);
        } catch (ValidationException e) {
            fail(dsId + " Self-referential assertions should be allowed.");
        }
    }

    public void testDatatypes() throws Exception {
        testDatatypes("RELS-EXT");
        testDatatypes("RELS-INT");
    }
    private void testDatatypes(String dsId) throws Exception {
        pid = PID.getInstance("demo:foo");
        String s, p, o;

        if (dsId.equals("RELS-INT")) {
            s = pid.toURI() + "/DS1";
        } else {
            s = pid.toURI();
        }


        p = "urn:p";
        o = "abc:123";
        triples.add(createTriple(s, p, o, false, null));
        validateAndClear(dsId);

        o = "1";
        triples.add(createTriple(s, p, o, true, Constants.RDF_XSD.INT.uri));
        validateAndClear(dsId);

        o = "abc";
        triples.add(createTriple(s, p, o, true, Constants.RDF_XSD.INT.uri));
        try {
            validateAndClear(dsId);
            fail("Invalid integer value: " + o);
        } catch (ValidationException e) {
        }

        o = "-0001-01-01T00:00:00";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00.1";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00.01";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00.001";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "-0001-01-01T00:00:00Z";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00Z";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00.1Z";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00.01Z";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "1970-01-01T00:00:00.001Z";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        validateAndClear(dsId);

        o = "abc";
        triples.add(createTriple(s,
                                 p,
                                 o,
                                 true,
                                 Constants.RDF_XSD.DATE_TIME.uri));
        try {
            validateAndClear(dsId);
            fail("Invalid dateTime value: " + o);
        } catch (ValidationException e) {
        }
    }

    private void validateAndClear(String dsId) throws Exception {
        try {
            TripleIterator iter = new MockTripleIterator(triples);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            iter.toStream(out, RDFFormat.RDF_XML, false);
            new RelsValidator().validate(pid, dsId, new ByteArrayInputStream(out.toByteArray()));
        } finally {
            triples.clear();
        }
    }
    private static Triple createTriple(PID pid,
                                       String predicate,
                                       String object,
                                       boolean isLiteral,
                                       String datatype)
            throws ServerException {
        return createTriple(pid.toURI(), predicate, object, isLiteral, datatype);
    }

    private static Triple createTriple(String subject,
                                       String predicate,
                                       String object,
                                       boolean isLiteral,
                                       String datatype)
            throws ServerException {
        ObjectNode o = null;
        try {
            if (isLiteral) {
                if (datatype == null || datatype.length() == 0) {
                    o = new SimpleLiteral(object);
                } else {
                    o = new SimpleLiteral(object, new URI(datatype));
                }
            } else {
                o = new SimpleURIReference(new URI(object));
            }
            return new SimpleTriple(new SimpleURIReference(new URI(subject)),
                                    new SimpleURIReference(new URI(predicate)),
                                    o);
        } catch (URISyntaxException e) {
            throw new GeneralException(e.getMessage(), e);
        }
    }

}
