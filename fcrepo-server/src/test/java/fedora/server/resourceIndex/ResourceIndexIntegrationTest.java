/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import org.junit.After;

import org.trippi.RDFFormat;
import org.trippi.RDFUtil;
import org.trippi.TripleIterator;
import org.trippi.TriplestoreConnector;

import fedora.common.Models;

import fedora.server.storage.DOReader;
import fedora.server.storage.MockRepositoryReader;
import fedora.server.storage.ServiceDefinitionReader;
import fedora.server.storage.ServiceDeploymentReader;
import fedora.server.storage.SimpleDOReader;
import fedora.server.storage.SimpleServiceDefinitionReader;
import fedora.server.storage.SimpleServiceDeploymentReader;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.ObjectBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Superclass for <code>ResourceIndex</code> integration tests.
 *
 * @author Chris Wilper
 */
public abstract class ResourceIndexIntegrationTest {

    private static final Logger LOG =
            Logger.getLogger(ResourceIndexIntegrationTest.class.getName());

    private static final String TEST_DIR = "target";

    private static final String DB_DRIVER =
            "org.apache.derby.jdbc.EmbeddedDriver";

    private static final String DB_URL = "jdbc:derby:test;create=true";

    private static final String DB_USERNAME = "test";

    private static final String DB_PASSWORD = "test";

    // needs to be set in order for object serializers/deserializers to work
    static {
        Datastream.defaultChecksumType = "DISABLED";
    }

    /**
     * The <code>ResourceIndexImpl</code> instance we'll be using.
     */
    private ResourceIndex _ri;

    /**
     * The flusher instance we'll use.
     */
    private Flusher _flusher;

    /**
     * Initialize the RI at the given level and return it. If the RI is already
     * initialized, it will be closed and re-initialized at the given level.
     */
    protected void initRI(int indexLevel) throws Exception {
        if (_ri != null) {
            try {
                _ri.close();
            } catch (Exception e) {
            }
        }
        TriplestoreConnector connector = getConnector();
        TripleGenerator generator = new ModelBasedTripleGenerator();

        _ri = new ResourceIndexImpl(connector, generator, indexLevel, false);
    }

    /**
     * Get the <code>TriplestoreConnector</code> to be used in conjunction
     * with the <code>ResourceIndexImpl</code>.
     *
     * @throws Exception
     *         if constructing the connector fails for any reason.
     */
    private static TriplestoreConnector getConnector() throws Exception {

        HashMap<String, String> config = new HashMap<String, String>();

        ///*
        config.put("backslashIsEscape", "false");
        config.put("ddlGenerator",
                   "org.nsdl.mptstore.impl.derby.DerbyDDLGenerator");
        config.put("autoFlushBufferSize", "1000");
        config.put("autoFlushDormantSeconds", "5");
        config.put("bufferFlushBatchSize", "1000");
        config.put("bufferSafeCapacity", "1000");
        config.put("fetchSize", "1000");
        config.put("jdbcDriver", DB_DRIVER);
        config.put("jdbcURL", DB_URL);
        config.put("username", DB_USERNAME);
        config.put("password", DB_PASSWORD);
        config.put("poolInitialSize", "5");
        config.put("poolMaxSize", "10");

        return TriplestoreConnector.init("org.trippi.impl.mpt.MPTConnector",
                                         config);
    }

    // Test tearDown

    @After
    public void tearDownTest() throws Exception {
        if (_ri != null) {
            tearDownTriplestore();
        }
    }

    private void tearDownTriplestore() throws Exception {

        // delete all triples from the RI
        File dump = new File(TEST_DIR + "/all-triples.txt");
        FileOutputStream out = null;
        try {
            // write all to temp file
            TripleIterator triples = _ri.findTriples(null, null, null, -1);
            out = new FileOutputStream(dump);
            triples.toStream(out, RDFFormat.TURTLE);
            try {
                out.close();
            } catch (Exception e) {
            }
            out = null;

            // load all from temp file
            triples =
                    TripleIterator.fromStream(new FileInputStream(dump),
                                              RDFFormat.TURTLE);
            _ri.delete(triples, true);
        } finally {
            if (out != null) {
                out.close();
            }
            dump.delete();
        }

        _ri.close();
    }

    // do test methods

    protected void doAddDelTest(int riLevel, DigitalObject obj)
            throws Exception {
        Set<DigitalObject> set = new HashSet<DigitalObject>();
        set.add(obj);
        doAddDelTest(riLevel, set);
    }

    protected void doAddDelTest(int riLevel, Set<DigitalObject> objects)
            throws Exception {

        initRI(riLevel);

        addAll(objects, true);
        assertTrue("Did not get expected triples after add",
                   sameTriples(getExpectedTriples(riLevel, objects),
                               getActualTriples(),
                               true));

        deleteAll(objects, true);
        assertEquals("Some triples remained after delete",
                     0,
                     getActualTriples().size());
    }

    protected void doModifyTest(int riLevel,
                                DigitalObject origObject,
                                DigitalObject modifiedObject) throws Exception {
        Set<DigitalObject> origObjects = new HashSet<DigitalObject>();
        origObjects.add(origObject);
        doModifyTest(riLevel, origObjects, modifiedObject);
    }

    // if riLevel is -1, assume original objects have already been added
    // and we don't need to change the ri level
    protected void doModifyTest(int riLevel,
                                Set<DigitalObject> origObjects,
                                DigitalObject modifiedObject) throws Exception {

        if (riLevel > -1) {
            initRI(riLevel);
            addAll(origObjects, true);
        }

        DigitalObject origObject = null;

        // get a set with the modified object in place of its old version
        Set<DigitalObject> newObjects = new HashSet<DigitalObject>();
        for (DigitalObject orig : origObjects) {
            if (orig.getPid().equals(modifiedObject.getPid())) {
                origObject = orig;
            } else {
                newObjects.add(orig);
            }
        }
        newObjects.add(modifiedObject);

        modify(origObject, modifiedObject, true);

        assertTrue("Did not get expected triples after modify",
                   sameTriples(getExpectedTriples(riLevel, newObjects),
                               getActualTriples(),
                               true));
    }

    // Utility methods for tests

    protected void modify(DigitalObject origObject,
                          DigitalObject modifiedObject,
                          boolean flush) throws Exception {

        _ri.modifyObject(getDOReader(origObject), getDOReader(modifiedObject));

        if (flush) {
            _ri.flushBuffer();
        }
    }

    protected ServiceDefinitionReader getServiceDefinitionReader(DigitalObject obj)
            throws Exception {
        return new SimpleServiceDefinitionReader(null,
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 obj);
    }

    protected ServiceDeploymentReader getServiceDeploymentReader(DigitalObject obj)
            throws Exception {
        return new SimpleServiceDeploymentReader(null,
                                                 null,
                                                 null,
                                                 null,
                                                 null,
                                                 obj);
    }

    protected DOReader getDOReader(DigitalObject obj) throws Exception {
        return new SimpleDOReader(null, null, null, null, null, obj);
    }

    protected void addObj(DigitalObject obj, boolean flush) throws Exception {
        Set<DigitalObject> set = new HashSet<DigitalObject>();
        set.add(obj);
        addAll(set, flush);
    }

    protected void addAll(Set<DigitalObject> objects, boolean flush)
            throws Exception {
        addOrDelAll(objects, flush, true);
    }

    protected void deleteAll(Set<DigitalObject> objects, boolean flush)
            throws Exception {
        addOrDelAll(objects, flush, false);
    }

    private void addOrDelAll(Set<DigitalObject> objects,
                             boolean flush,
                             boolean add) throws Exception {
        for (DigitalObject obj : objects) {
            if (add) {
                    _ri.addObject(getDOReader(obj));
            } else {
                    _ri.deleteObject(getDOReader(obj));
            }
        }
        if (flush) {
            _ri.flushBuffer();
        }
    }

    protected Set<Triple> getExpectedTriples(int riLevel,
                                             Set<DigitalObject> objects)
            throws Exception {

        // we can return early in this case
        if (riLevel == 0) {
            return new HashSet<Triple>();
        }

        // add all to a mock repository reader
        MockRepositoryReader repo = new MockRepositoryReader();
        for (DigitalObject obj : objects) {
            repo.putObject(obj);
        }

        // prepare appropriate MethodInfoStore and TripleGenerator
        TripleGenerator generator = new ModelBasedTripleGenerator();

        Set<Triple> expected = new HashSet<Triple>();

        for (DigitalObject obj : objects) {
            expected.addAll(generator.getTriplesForObject(getDOReader(obj)));
        }

        return expected;

    }

    protected boolean sameTriples(Set<Triple> expected,
                                  Set<Triple> actual,
                                  boolean logDiffs) {
        TreeSet<String> eStrings = new TreeSet<String>();
        for (Triple triple : expected) {
            eStrings.add(RDFUtil.toString(triple));
        }

        TreeSet<String> aStrings = new TreeSet<String>();
        for (Triple triple : actual) {
            aStrings.add(RDFUtil.toString(triple));
        }

        if (eStrings.equals(aStrings)) {
            return true;
        } else {
            if (logDiffs) {
                StringBuffer out = new StringBuffer();
                out.append("Triple sets differ.\n");
                out.append("Expected set has " + expected.size()
                        + " triples.\n");
                out.append("Actual set has " + actual.size() + " triples.\n\n");
                out.append("Expected triples:\n");
                for (String t : eStrings) {
                    out.append("  " + t + "\n");
                }
                out.append("\nActual triples:\n");
                for (String t : aStrings) {
                    out.append("  " + t + "\n");
                }
                LOG.warn(out.toString());
            }
            return false;
        }

    }

    protected Set<Triple> getActualTriples() throws Exception {
        return getActualTriples(null, null, null);
    }

    protected Set<Triple> getActualTriples(SubjectNode subject,
                                           PredicateNode predicate,
                                           ObjectNode object) throws Exception {
        Set<Triple> set = new HashSet<Triple>();
        TripleIterator iter = _ri.findTriples(subject, predicate, object, -1);
        while (iter.hasNext()) {
            set.add(iter.next());
        }
        iter.close();
        return set;
    }

    /**
     * Get the DC xml for an object.
     */
    protected String getDC(String content) {
        StringBuffer x = new StringBuffer();
        x.append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\"");
        x
                .append(" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n");
        x.append(content + "\n");
        x.append("</oai_dc:dc>");
        return x.toString();
    }



    /**
     * Get the METHODMAP xml for a sDef.
     */
    protected static String getMethodMap(Set<ParamDomainMap> methodDefs) {
        return getMethodMap(methodDefs, null, false);
    }

    /**
     * Get the METHODMAP xml for a sDef or sDep.
     */
    protected static String getMethodMap(Set<ParamDomainMap> methodDefs,
                                         Map<String, Set<String>> inputKeys,
                                         boolean forSDef) {
        StringBuffer xml = new StringBuffer();
        xml.append("<MethodMap name=\"MethodMap\" xmlns=\"http://fed"
                + "ora.comm.nsdlib.org/service/methodmap\">\n");
        for (ParamDomainMap methodDef : methodDefs) {
            String method = methodDef.getMethodName();
            xml.append("  <Method operationName=\"" + method + "\"");
            if (forSDef) {
                xml.append(" wsdlMsgName=\"" + method + "Request\"");
                xml.append(" wsdlMsgOutput=\"dissemResponse\"");
            }
            xml.append(">\n");
            for (String paramName : methodDef.keySet()) {
                ParamDomain domain = methodDef.get(paramName);
                xml.append("    <UserInputParm parmName=\"" + paramName
                        + "\" passBy=\"VALUE\" defaultValue=\"\" required=\""
                        + domain.isRequired() + "\">\n");
                if (domain.size() > 0) {
                    xml.append("      <ValidParmValues>\n");
                    for (String value : domain) {
                        xml.append("        <ValidParm value=\"" + value
                                + "\"/>\n");
                    }
                    xml.append("      </ValidParmValues>\n");
                }
                xml.append("    </UserInputParm>\n");
            }
            if (forSDef) {
                Set<String> keys = inputKeys.get(method);
                if (keys != null) {
                    for (String key : keys) {
                        xml.append("    <DatastreamInputParm parmName=\"" + key
                                + "\" passBy=\"URL_REF\"/>\n");
                    }
                }

                xml.append("    <MethodReturnType wsdlMsgName=\""
                        + "dissemResponse\" wsdlMsgTOMIME=\""
                        + "application/octet-stream\"/>\n");
            }
            xml.append("  </Method>\n");
        }
        xml.append("</MethodMap>");
        return xml.toString();
    }

    /**
     * Get the DSINPUTSPEC xml for a sDef.
     */
    protected static String getInputSpec(String sDefPID,
                                         Map<String, Set<String>> inputTypes) {
        StringBuffer xml = new StringBuffer();
        xml.append("<DSInputSpec xmlns=\"http://fedora.comm.nsdlib.org/"
                + "service/bindspec\" label=\"InputSpec\">\n");
        for (String key : inputTypes.keySet()) {
            xml.append("  <DSInput DSMin=\"1\" DSMax=\"1\" DSOrdinality=\""
                    + "false\" wsdlMsgPartName=\"" + key + "\">\n");
            xml.append("    <DSInputLabel>label</DSInputLabel>\n");
            for (String mimeType : inputTypes.get(key)) {
                xml.append("    <DSMIME>" + mimeType + "</DSMIME>\n");
            }
            xml.append("    <DSInputInstruction>inst</DSInputInstruction>\n");
            xml.append("  </DSInput>\n");
        }
        xml.append("</DSInputSpec>\n");
        return xml.toString();
    }

    private static void addXSDType(String name, StringBuffer xml) {
        xml.append("      <xsd:simpleType name=\"" + name + "\">\n");
        xml.append("        <xsd:restriction base=\"xsd:string\"/>\n");
        xml.append("      </xsd:simpleType>\n");
    }

    /**
     * Get the WSDL xml for a sDef.
     */
    protected static String getWSDL(Set<ParamDomainMap> methodDefs,
                                    Map<String, Set<String>> inputKeys,
                                    Map<String, Set<String>> outputTypes) {
        StringBuffer xml = new StringBuffer();

        xml.append("<definitions xmlns=\"http://schemas.xmlsoap.org/wsdl/\""
                + " xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\""
                + " xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\""
                + " xmlns:this=\"MyService\""
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " name=\"Name\" targetNamespace=\"MyService\">\n");

        //
        // xsd type definitions
        //

        xml.append("  <types>\n");
        xml.append("    <xsd:schema targetNamespace=\"MyService\">\n");
        Set<String> addedTypes = new HashSet<String>();
        for (ParamDomainMap methodDef : methodDefs) {
            // one type def per distinct user param name
            for (String name : methodDef.keySet()) {
                if (addedTypes.add(name + "Type")) {
                    addXSDType(name + "Type", xml);
                }
            }

            // one type def per distinct ds input key
            for (String key : inputKeys.get(methodDef.getMethodName())) {
                if (addedTypes.add(key + "Type")) {
                    addXSDType(key + "Type", xml);
                }
            }
        }
        xml.append("    </xsd:schema>\n");
        xml.append("  </types>\n");

        //
        // message definitions
        //

        // one request message per method
        for (ParamDomainMap methodDef : methodDefs) {
            String method = methodDef.getMethodName();
            xml.append("  <message name=\"" + method + "Request\">\n");
            // one part per user param
            for (String name : methodDef.keySet()) {
                xml.append("    <part name=\"" + name + "\" type=\"this:"
                        + name + "Type\"/>\n");
            }
            // one part per ds input key
            for (String key : inputKeys.get(method)) {
                xml.append("    <part name=\"" + key + "\" type=\"this:" + key
                        + "Type\"/>\n");
            }
            xml.append("  </message>\n");
        }
        // one dissemResponse output message
        xml.append("  <message name=\"dissemResponse\">\n");
        xml.append("    <part name=\"response\" type=\"xsd:base64Binary\"/>\n");
        xml.append("  </message>\n");

        //
        // port type (per-method input/output messages)
        //
        xml.append("  <portType name=\"MyServicePortType\">\n");
        for (ParamDomainMap methodDef : methodDefs) {
            String method = methodDef.getMethodName();
            xml.append("    <operation name=\"" + method + "\">\n");
            xml.append("      <input message=\"this:" + method
                    + "Request\"/>\n");
            xml.append("      <output message=\"this:dissemResponse\"/>\n");
            xml.append("    </operation>\n");
        }
        xml.append("  </portType>\n");

        //
        // service location
        //
        xml.append("  <service name=\"MyService\">\n");
        xml.append("    <port binding=\"this:MyService_http\" "
                + "name=\"MyService_port\">\n");
        xml
                .append("      <http:address location=\"http://example.org/MyService/\"/>\n");
        xml.append("    </port>\n");
        xml.append("  </service>\n");

        //
        // operation locations and input/output bindings
        //
        xml
                .append("  <binding name=\"MyService_http\" type=\"this:MyServicePortType\">\n");
        xml.append("    <http:binding verb=\"GET\"/>\n");
        for (ParamDomainMap methodDef : methodDefs) {
            String method = methodDef.getMethodName();
            xml.append("    <operation name=\"" + method + "\">\n");

            // location = ..?userParm1=(userParm1)&key1=KEY1..etc
            StringBuffer location = new StringBuffer();
            location.append(method + "?");
            boolean first = true;
            for (String name : methodDef.keySet()) {
                if (!first) {
                    location.append("&amp;");
                }
                location.append(name + "=(" + name + ")");
                first = false;
            }
            for (String key : inputKeys.get(method)) {
                if (!first) {
                    location.append("&amp;");
                }
                location.append(key.toLowerCase() + "=(" + key + ")");
                first = false;
            }
            xml.append("      <http:operation location=\""
                    + location.toString() + "\"/>\n");

            // input is always urlReplacement
            xml.append("      <input><http:urlReplacement/></input>\n");

            // output lists all possible output mime types
            xml.append("      <output>\n");
            for (String mimeType : outputTypes.get(method)) {
                xml.append("        <mime:content type=\"" + mimeType
                        + "\"/>\n");
            }
            xml.append("      </output>\n");

            xml.append("    </operation>\n");
        }
        xml.append("  </binding>\n");

        xml.append("</definitions>\n");
        return xml.toString();
    }

    protected static Set<DigitalObject> getTestObjects(int num,
                                                       int datastreamsPerObject) {
        Set<DigitalObject> set = new HashSet<DigitalObject>(num);
        for (int i = 0; i < num; i++) {
            DigitalObject obj = getTestObject("test:" + i, "label" + i);
            for (int j = 0; j < datastreamsPerObject; j++) {
                addEDatastream(obj, "DS" + j);
            }
            set.add(obj);
        }
        return set;
    }

    protected TripleIterator spo(String query) throws Exception {
        return _ri.findTriples("spo", query, -1, false);
    }

    protected static void addEDatastream(DigitalObject obj, String id) {
        ObjectBuilder.addEDatastream(obj, id);
    }

    protected static void addRDatastream(DigitalObject obj, String id) {
        ObjectBuilder.addRDatastream(obj, id);
    }

    protected static void addXDatastream(DigitalObject obj,
                                         String id,
                                         String xml) {
        ObjectBuilder.addXDatastream(obj, id, xml);
    }

    protected static void addMDatastream(DigitalObject obj, String id) {
        ObjectBuilder.addMDatastream(obj, id);
    }

    protected static DigitalObject getTestObject(String pid, String label) {
        return ObjectBuilder.getTestObject(pid, label);
    }

    protected static DigitalObject getTestSDef(String pid,
                                               String label,
                                               Set<ParamDomainMap> methodDefs) {
        Date now = new Date();
        URIReference[] models = {Models.SERVICE_DEFINITION_3_0};
        DigitalObject obj = ObjectBuilder.getTestObject(pid,
                                                        models,
                                                        "A",
                                                        "someOwnerId",
                                                        label,
                                                        now,
                                                        now);
        addXDatastream(obj, "METHODMAP", getMethodMap(methodDefs));
        return obj;
    }

    protected static DigitalObject getTestSDep(String pid,
                                                String label,
                                                String sDefPID,
                                                Set<ParamDomainMap> methodDefs,
                                                Map<String, Set<String>> inputKeys,
                                                Map<String, Set<String>> inputTypes,
                                                Map<String, Set<String>> outputTypes) {

        Date now = new Date();
        URIReference[] models = {Models.SERVICE_DEPLOYMENT_3_0};
        DigitalObject obj = ObjectBuilder.getTestObject(pid,
                                                        models,
                                                        "A",
                                                        "someOwnerId",
                                                        label,
                                                        now,
                                                        now);

        String methodMapXML = getMethodMap(methodDefs, inputKeys, true);
        addXDatastream(obj, "METHODMAP", methodMapXML);

        String inputSpecXML = getInputSpec(sDefPID, inputTypes);
        addXDatastream(obj, "DSINPUTSPEC", inputSpecXML);

        String wsdlXML = getWSDL(methodDefs, inputKeys, outputTypes);
        addXDatastream(obj, "WSDL", wsdlXML);

        return obj;
    }

    // sdef:1 has one no-parameter method
    protected static DigitalObject getSDefOne() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);
        return getTestSDef("test:sdef1", "sdef1", methodDefs);
    }

    // sdef:1b has two no-parameter methods
    protected static DigitalObject getSDefOneB() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        methodDefs.add(methodTwo);
        return getTestSDef("test:sdef1b", "sdef1b", methodDefs);
    }

    // sdef:1c has one no-parameter method (same as sdef:1)
    protected static DigitalObject getSDefOneC() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);
        return getTestSDef("test:sdef1c", "sdef1c", methodDefs);
    }

    // sdef:2 has one required one-parameter method with two possible values
    protected static DigitalObject getSDefTwo() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", true);
        argOneDomain.add("val1");
        argOneDomain.add("val2");
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        return getTestSDef("test:sdef2", "sdef2", methodDefs);
    }

    // sdef:2b has two required one-parameter methods with two possible values
    protected static DigitalObject getSDefTwoB() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", true);
        argOneDomain.add("val1");
        argOneDomain.add("val2");
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        methodTwo.put("argOne", argOneDomain);
        methodDefs.add(methodTwo);
        return getTestSDef("test:sdef2b", "sdef2b", methodDefs);
    }

    // sdef:3 has one optional one-parameter method with any possible value
    protected static DigitalObject getSDefThree() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", false);
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        return getTestSDef("test:sdef3", "sdef3", methodDefs);
    }

    // sdef:3b has two optional one-parameter methods with any possible value
    protected static DigitalObject getSDefThreeB() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", false);
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        methodTwo.put("argOne", argOneDomain);
        methodDefs.add(methodTwo);
        return getTestSDef("test:sdef3b", "sdef3b", methodDefs);
    }

    // sdef:4 has two one-parameter methods, one required with two possible
    //        values and the other optional with any possible value
    protected static DigitalObject getSDefFour() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", true);
        argOneDomain.add("val1");
        argOneDomain.add("val2");
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        argOneDomain = new ParamDomain("argOne", false);
        methodTwo.put("argOne", argOneDomain);
        methodDefs.add(methodTwo);
        return getTestSDef("test:sdef4", "sdef4", methodDefs);
    }

    // construct a map representing key-to-ds bindings with 1 or 2 keys
    protected static Map<String, Set<String>> getMap(String key1,
                                                     String[] values1,
                                                     String key2,
                                                     String[] values2) {
        Set<String> valueSet1 = new HashSet<String>();
        for (String value : values1) {
            valueSet1.add(value);
        }
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(key1, valueSet1);
        if (key2 != null) {
            Set<String> valueSet2 = new HashSet<String>();
            for (String value : values2) {
                valueSet2.add(value);
            }
            map.put(key2, valueSet2);
        }
        return map;
    }

    // sdep:1 implements sdef:1 and takes one datastream
    protected static DigitalObject getSDepOne() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);

        return getTestSDep("test:sdep1",
                            "sdep1",
                            "test:sdef1",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   null,
                                   null),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   null,
                                   null),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   null,
                                   null));
    }

    // sdep:1b implements sdef:1b and takes one datastream
    protected static DigitalObject getSDepOneB() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        methodDefs.add(methodTwo);

        return getTestSDep("test:sdep1b",
                            "sdep1b",
                            "test:sdef1b",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   "methodTwo",
                                   new String[] {"KEY2"}),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   "KEY2",
                                   new String[] {"text/xml"}),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   "methodTwo",
                                   new String[] {"text/xml"}));
    }

    // sdep:1c implements sdef:1c and takes one datastream
    protected static DigitalObject getSDepOneC() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);

        return getTestSDep("test:sdep1c",
                            "sdep1c",
                            "test:sdef1c",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   null,
                                   null),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   null,
                                   null),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   null,
                                   null));
    }

    // sdep:1d implements sdef:1 and takes TWO datastreams
    protected static DigitalObject getSDepOneD() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        methodDefs.add(methodOne);

        return getTestSDep("test:sdep1d",
                            "sdep1d",
                            "test:sdef1",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1", "KEY2"},
                                   null,
                                   null),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   "KEY2",
                                   new String[] {"text/xml"}),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   null,
                                   null));
    }

    // sdep:2 implements sdef:2 and takes one datastream
    protected static DigitalObject getSDepTwo() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", true);
        argOneDomain.add("val1");
        argOneDomain.add("val2");
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);

        return getTestSDep("test:sdep2",
                            "sdep2",
                            "test:sdef2",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   null,
                                   null),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   null,
                                   null),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   null,
                                   null));
    }

    // sdep:2b implements zdef:2b and takes one datastream
    protected static DigitalObject getSDepTwoB() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", true);
        argOneDomain.add("val1");
        argOneDomain.add("val2");
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        methodTwo.put("argOne", argOneDomain);
        methodDefs.add(methodTwo);

        return getTestSDep("test:sdep2b",
                            "sdep2b",
                            "test:zdef2b",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   "methodTwo",
                                   new String[] {"KEY2"}),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   "KEY2",
                                   new String[] {"text/xml"}),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   "methodTwo",
                                   new String[] {"text/xml"}));
    }

    // sdep:3 implements sdef:3 and takes one datastream
    protected static DigitalObject getSDepThree() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", false);
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);

        return getTestSDep("test:sdep3",
                            "sdep3",
                            "test:sdef3",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   null,
                                   null),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   null,
                                   null),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   null,
                                   null));
    }

    // sdep:3b implements sdef:3b and takes one datastream
    protected static DigitalObject getSDepThreeB() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", false);
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        methodTwo.put("argOne", argOneDomain);
        methodDefs.add(methodTwo);

        return getTestSDep("test:sdep3b",
                            "sdep3b",
                            "test:sdef3b",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   "methodTwo",
                                   new String[] {"KEY2"}),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   "KEY2",
                                   new String[] {"text/xml"}),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   "methodTwo",
                                   new String[] {"text/xml"}));
    }

    // adep:4 implements sdef:4 and takes one datastream
    protected static DigitalObject getSDepFour() {
        Set<ParamDomainMap> methodDefs = new HashSet<ParamDomainMap>();
        ParamDomainMap methodOne = new ParamDomainMap("methodOne");
        ParamDomain argOneDomain = new ParamDomain("argOne", true);
        argOneDomain.add("val1");
        argOneDomain.add("val2");
        methodOne.put("argOne", argOneDomain);
        methodDefs.add(methodOne);
        ParamDomainMap methodTwo = new ParamDomainMap("methodTwo");
        argOneDomain = new ParamDomain("argOne", false);
        methodTwo.put("argOne", argOneDomain);
        methodDefs.add(methodTwo);

        return getTestSDep("test:sdep4",
                            "sdep4",
                            "test:sdef4",
                            methodDefs,
                            getMap("methodOne",
                                   new String[] {"KEY1"},
                                   "methodTwo",
                                   new String[] {"KEY2"}),
                            getMap("KEY1",
                                   new String[] {"text/xml"},
                                   "KEY2",
                                   new String[] {"text/xml"}),
                            getMap("methodOne",
                                   new String[] {"text/xml"},
                                   "methodTwo",
                                   new String[] {"text/xml"}));
    }

    protected Map<String, String> getBindings(int numKeys) {
        Map<String, String> bindings = new HashMap<String, String>();
        for (int i = 1; i <= numKeys; i++) {
            bindings.put("KEY" + i, "DS1");
        }
        return bindings;
    }

    // get a set containing three digital objects
    protected Set<DigitalObject> getObjectSet(DigitalObject o1,
                                              DigitalObject o2,
                                              DigitalObject o3) {
        Set<DigitalObject> set = new HashSet<DigitalObject>();
        set.add(o1);
        set.add(o2);
        set.add(o3);
        return set;
    }

    public void startFlushing(int sleepMS) throws Exception {
        if (_flusher != null) {
            try {
                finishFlushing();
            } catch (Exception e) {
                System.err.println("Error stopping old flusher!!");
                e.printStackTrace();
            }
            throw new Exception("Flusher was already running!");
        }
        _flusher = new Flusher(_ri, sleepMS);
        _flusher.start();
    }

    // finish async flushing and do a final flush
    public void finishFlushing() throws Exception {
        _flusher.finish();
        _ri.flushBuffer();
        _flusher = null;
    }

    // Inner classes for tests

    /**
     * A Thread that continuously flushes the buffer.
     */
    public class Flusher
            extends Thread {

        private final ResourceIndex _ri;

        private final int _sleepMS;

        private boolean _shouldFinish = false;

        private Exception _error;

        /**
         * Construct a flusher that sleeps the given number of milliseconds
         * between flush attempts.
         *
         * @param sleepMS
         *        milliseconds to sleep. Will simply yield between flush
         *        attempts if less than 1.
         */
        public Flusher(ResourceIndex ri, int sleepMS) {
            _ri = ri;
            _sleepMS = sleepMS;
        }

        /**
         * Set signal for flusher to finish and wait for it.
         *
         * @throws Exception
         *         if the flusher encountered an error any time while it was
         *         running.
         */
        public void finish() throws Exception {
            _shouldFinish = true;
            while (isAlive()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            if (_error != null) {
                throw _error;
            }
        }

        /**
         * Flush the buffer until the finish signal arrives from another thread.
         */
        @Override
        public void run() {
            try {
                while (!_shouldFinish) {
                    if (_sleepMS > 0) {
                        try {
                            Thread.sleep(_sleepMS);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        Thread.yield();
                    }
                    _ri.flushBuffer();
                }
            } catch (Exception e) {
                _error = e;
            }
        }
    }
}
