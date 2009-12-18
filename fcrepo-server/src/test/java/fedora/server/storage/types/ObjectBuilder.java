/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.Date;
import java.util.Iterator;

import org.jrdf.graph.URIReference;

import fedora.common.Models;
import fedora.common.PID;

import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.translation.FOXML1_1DODeserializer;
import fedora.server.storage.translation.FOXML1_1DOSerializer;

/**
 * Utility for creating various <code>DigitalObject</code> instances for testing.
 */
public abstract class ObjectBuilder {

    public static void addEDatastream(DigitalObject obj, String id) {
        DatastreamReferencedContent ds = new DatastreamReferencedContent();
        ds.DSControlGrp = "E";
        ds.DSMIME = "text/plain";
        ds.DSLocation = "http://www.example.org/e.txt";
        ds.DSLocationType = "URL";
        ds.DSSize = 1;
        addDatastream(obj, id, ds);
    }

    public static void addRDatastream(DigitalObject obj, String id) {
        DatastreamReferencedContent ds = new DatastreamReferencedContent();
        ds.DSControlGrp = "R";
        ds.DSMIME = "text/plain";
        ds.DSLocation = "http://www.example.org/r.txt";
        ds.DSLocationType = "URL";
        ds.DSSize = 2;
        addDatastream(obj, id, ds);
    }

    public static void addXDatastream(DigitalObject obj,
                                         String id,
                                         String xml) {
        DatastreamXMLMetadata ds = new DatastreamXMLMetadata();
        ds.DSControlGrp = "X";
        ds.DSMIME = "text/xml";
        ds.DSSize = xml.length();
        try {
            ds.xmlContent = xml.getBytes("UTF-8");
        } catch (Exception e) {
        }
        addDatastream(obj, id, ds);
    }

    public static void addMDatastream(DigitalObject obj, String id) {
        DatastreamManagedContent ds = new DatastreamManagedContent();
        ds.DSControlGrp = "M";
        ds.DSMIME = "image/jpeg";
        ds.DSLocation = "bogusLocation";
        ds.DSLocationType = "INTERNAL";
        ds.DSSize = 4;
        addDatastream(obj, id, ds);
    }

    private static void addDatastream(DigitalObject obj,
                                      String id,
                                      Datastream ds) {
        int size = 0;
        for (Datastream d : obj.datastreams(id)) {
            size ++;
        }
        ds.DatastreamID = id;
        ds.DSState = "A";
        ds.DSVersionable = true;
        ds.DSVersionID = id + "." + size;
        ds.DSLabel = "ds label";
        ds.DSCreateDT = new Date();
        obj.addDatastreamVersion(ds, true);
    }

    public static DigitalObject getTestObject(String pid, String label) {
        Date now = new Date();
        URIReference[] models = {Models.FEDORA_OBJECT_3_0};
        return getTestObject(pid,
                             models,
                             "A",
                             "someOwnerId",
                             label,
                             now,
                             now);
    }

    public static DigitalObject getTestObject(String pid,
                                              URIReference[] models,
                                              String state,
                                              String ownerId,
                                              String label,
                                              Date createDate,
                                              Date lastModDate) {
        DigitalObject obj = new BasicDigitalObject();
        obj.setPid(pid);

        StringBuilder rdf = new StringBuilder();
        rdf.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                        + "xmlns:fedora-model=\"info:fedora/fedora-system:def/model#\">\n"
                        + "<rdf:Description rdf:about=\"");
        rdf.append(PID.getInstance(pid).toURI() + "\">\n");
        for (URIReference model : models) {
            rdf.append("<fedora-model:hasModel rdf:resource=\""
                    + model.getURI().toString()
                    + "\"></fedora-model:hasModel>\n");
        }
        rdf.append("</rdf:Description></rdf:RDF>");

        addXDatastream(obj, "RELS-EXT", rdf.toString());

        obj.setState(state);
        obj.setOwnerId(ownerId);
        obj.setLabel(label);
        obj.setCreateDate(createDate);
        obj.setLastModDate(lastModDate);
        return obj;
    }

    /**
     * Make a deep copy of the given digital object.
     */
    public static DigitalObject deepCopy(DigitalObject obj) throws Exception {

        // make sure DOTranslationUtility doesn't die
        if (System.getProperty("fedoraServerHost") == null
                || System.getProperty("fedoraServerPort") == null) {
            System.setProperty("fedoraServerHost", "localhost");
            System.setProperty("fedoraServerPort", "8080");
        }

        String charEncoding = "UTF-8";
        int transContext = DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FOXML1_1DOSerializer ser = new FOXML1_1DOSerializer();
        ser.serialize(obj, out, charEncoding, transContext);

        FOXML1_1DODeserializer deser = new FOXML1_1DODeserializer();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DigitalObject objCopy = new BasicDigitalObject();
        deser.deserialize(in, objCopy, charEncoding, transContext);

        // make sure dates of any to-be-added new components differ
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }

        return objCopy;
    }

    /**
     * Get the DC xml for an object.
     */
    public static String getDC(String content) {
        StringBuffer x = new StringBuffer();
        x.append("<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\"");
        x.append(" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\">\n");
        x.append(content + "\n");
        x.append("</oai_dc:dc>");
        return x.toString();
    }

    /**
     * Get the RELS-EXT xml for an object.
     */
    public static String getRELSEXT(String pid, String content) {
        StringBuffer x = new StringBuffer();
        x.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        x.append(" xmlns:foo=\"http://example.org/foo#\">\n");
        x.append("<rdf:Description rdf:about=\"" + PID.getInstance(pid).toURI() + "\">\n");
        x.append(content + "\n");
        x.append("</rdf:Description>\n");
        x.append("</rdf:RDF>");
        return x.toString();
    }

    /**
     * Get the RELS-INT xml for an object.
     */
    public static String getRELSINT(String pid, String content1, String content2) {
        StringBuffer x = new StringBuffer();
        x.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        x.append(" xmlns:foo=\"http://example.org/foo#\">\n");
        // relationship(s) from datastream DS1
        x.append("<rdf:Description rdf:about=\"" + PID.getInstance(pid).toURI() + "/DS1" + "\">\n");
        x.append(content1 + "\n");
        x.append("</rdf:Description>\n");
        // relationship(s) from datastream DS2
        x.append("<rdf:Description rdf:about=\"" + PID.getInstance(pid).toURI() + "/DS2" + "\">\n");
        x.append(content2 + "\n");
        x.append("</rdf:Description>\n");
        x.append("</rdf:RDF>");
        return x.toString();
    }
    /**
     * Sets any un-set dates in the object to the given date.
     */
    public static void setDates(DigitalObject obj, Date date) {
        if (obj.getCreateDate() == null) obj.setCreateDate(date);
        Iterator<String> dsIds = obj.datastreamIdIterator();
        while (dsIds.hasNext()) {
            String dsid = dsIds.next();
            for (Datastream ds : obj.datastreams(dsid)) {
                if (ds.DSCreateDT == null) {
                    ds.DSCreateDT = new Date();
                }
            }
        }
    }

}
