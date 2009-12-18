/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package fedora.server.storage.translation;

import java.util.Date;

import org.custommonkey.xmlunit.XMLTestCase;

import org.jrdf.graph.URIReference;

import org.junit.Before;

import fedora.common.Constants;
import fedora.common.PID;

import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DSBinding;
import fedora.server.storage.types.DSBindingMap;
import fedora.server.storage.types.DatastreamReferencedContent;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.DigitalObject;
import fedora.server.storage.types.Disseminator;

/**
 * Convenience superclass for serializer and deserializer tests.
 *
 * @author Chris Wilper
 */
@SuppressWarnings("deprecation")
public abstract class TranslationTest
        extends XMLTestCase {

    protected static final String TEST_PID = "test:pid";

    //---
    // Setup/Teardown
    //---

    @Override
    @Before
    public void setUp() {
        // HACK: make DOTranslationUtility happy
        System.setProperty("fedoraServerHost", "localhost");
        System.setProperty("fedoraServerPort", "8080");
        System.setProperty("fedoraAppServerContext", Constants.FEDORA_DEFAULT_APP_CONTEXT);
    }

    //---
    // Static helpers
    //---

    protected static DigitalObject createTestObject(URIReference... contentModelURIs) {
        DigitalObject obj = new BasicDigitalObject();
        obj.setPid(TEST_PID);
        DatastreamXMLMetadata ds = createXDatastream("RELS-EXT");

        StringBuilder rdf = new StringBuilder();
        rdf
                .append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                        + "xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">\n"
                        + "<rdf:Description rdf:about=\"");
        rdf.append(PID.getInstance(TEST_PID).toURI() + "\">\n");

        for (URIReference model : contentModelURIs) {
            rdf.append("<fedora-model:hasModel rdf:resource=\""
                    + model.getURI().toString()
                    + "\"></fedora-model:hasModel>\n");
        }
        rdf.append("</rdf:Description></rdf:RDF>");
        ds.xmlContent = rdf.toString().getBytes();

        obj.addDatastreamVersion(ds, false);
        obj.setCreateDate(new Date());
        obj.setLastModDate(new Date());
        obj.setCreateDate(new Date());
        obj.setLastModDate(new Date());
        return obj;
    }

    protected static DatastreamXMLMetadata createXDatastream(String id) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
        ds.DatastreamID = id;
        ds.DSVersionID = id + ".0";
        ds.DSControlGrp = "X";
        ds.xmlContent = "<doc/>".getBytes();
        ds.DSCreateDT = new Date();
        return ds;
    }

    protected static DatastreamReferencedContent createRDatastream(String id,
                                                                   String url) {
        DatastreamReferencedContent ds = new DatastreamReferencedContent();
        ds.DatastreamID = id;
        ds.DSVersionID = id + ".0";
        ds.DSControlGrp = "R";
        ds.DSLocation = url;
        return ds;
    }

    protected static Disseminator createDisseminator(String id, int numBindings) {
        Disseminator diss = new Disseminator();
        diss.dissID = id;
        diss.dissVersionID = id + ".0";
        diss.bDefID = TEST_PID + "bdef";
        diss.sDepID = TEST_PID + "bmech";
        diss.dsBindMap = new DSBindingMap();
        // the following is only needed for METS
        diss.dsBindMapID = id + "bindMap";
        DSBinding[] dsBindings = new DSBinding[numBindings];
        for (int i = 1; i <= numBindings; i++) {
            dsBindings[i - 1] = new DSBinding();
            dsBindings[i - 1].bindKeyName = "KEY" + i;
            dsBindings[i - 1].datastreamID = "DS" + i;
        }
        diss.dsBindMap.dsBindings = dsBindings;
        return diss;
    }

}
