/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.storage.types;

import fedora.common.Constants;

/**
 * DigitalObject utility methods.
 *
 * @author Chris Wilper
 */
public abstract class DigitalObjectUtil
        implements Constants {
   
    /**
     * Upgrades a legacy (pre-Fedora-3.0) object by setting the correct MIME
     * type and Format URI for all "reserved" datastreams.
     * 
     * @param obj the object to update.
     */
    @SuppressWarnings("deprecation")
    public static void updateLegacyDatastreams(DigitalObject obj) {
        final String xml = "text/xml";
        final String rdf = "application/rdf+xml";
        updateLegacyDatastream(obj, "DC", xml, OAI_DC2_0.uri);
        updateLegacyDatastream(obj, "RELS-EXT", rdf, RELS_EXT1_0.uri);
        updateLegacyDatastream(obj, "RELS-INT", rdf, RELS_INT1_0.uri);
        updateLegacyDatastream(obj, "POLICY", xml, XACML_POLICY1_0.uri);
        String fType = obj.getExtProperty(RDF.TYPE.uri);
        if (MODEL.BDEF_OBJECT.looselyMatches(fType, false)) {
            updateLegacyDatastream(obj,
                                   "METHODMAP",
                                   xml,
                                   SDEF_METHOD_MAP1_0.uri);
        } else if (MODEL.BMECH_OBJECT.looselyMatches(fType, false)) {
            updateLegacyDatastream(obj,
                                   "METHODMAP",
                                   xml,
                                   SDEP_METHOD_MAP1_1.uri);
            updateLegacyDatastream(obj,
                                   "DSINPUTSPEC",
                                   xml,
                                   DS_INPUT_SPEC1_1.uri);
            updateLegacyDatastream(obj,
                                   "WSDL",
                                   xml,
                                   WSDL.uri);
        }
    }
    
    private static void updateLegacyDatastream(DigitalObject obj,
                                               String dsId,
                                               String mimeType,
                                               String formatURI) {
        for (Datastream ds: obj.datastreams(dsId)) {
            ds.DSMIME = mimeType;
            ds.DSFormatURI = formatURI;
        }
    }
}
