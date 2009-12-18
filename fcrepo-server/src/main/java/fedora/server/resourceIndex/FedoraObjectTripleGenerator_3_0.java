/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.net.URI;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import fedora.common.Constants;
import fedora.common.PID;
import fedora.common.rdf.RDFName;
import fedora.common.rdf.SimpleLiteral;
import fedora.common.rdf.SimpleTriple;
import fedora.common.rdf.SimpleURIReference;

import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.DOReader;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DatastreamXMLMetadata;
import fedora.server.storage.types.RelationshipTuple;
import fedora.server.utilities.DCField;
import fedora.server.utilities.DCFields;

/**
 * Generates basic RDF triples for Fedora 3.0 objects.
 *
 * @author Chris Wilper
 */
public class FedoraObjectTripleGenerator_3_0
        extends TripleGeneratorBase
        implements Constants, TripleGenerator {

    /**
     * {@inheritDoc}
     */
    public Set<Triple> getTriplesForObject(DOReader reader)
            throws ResourceIndexException {

        Set<Triple> set = new HashSet<Triple>();

        addCommonTriples(reader, set);

        return set;
    }

    /**
     * Add the common core and datastream triples for the given object.
     */
    private URIReference addCommonTriples(DOReader reader, Set<Triple> set)
            throws ResourceIndexException {

        try {

            URIReference objURI = new SimpleURIReference(
                    new URI(PID.toURI(reader.GetObjectPID())));

            addCoreObjectTriples(reader, objURI, set);

            Datastream[] datastreams = reader.GetDatastreams(null, null);
            for (Datastream ds : datastreams) {
                addCoreDatastreamTriples(ds, objURI, set);
                if (ds.DatastreamID.equals("DC")) {
                    addDCTriples((DatastreamXMLMetadata) ds, objURI, set);
                }
            }

            addRelationshipTriples(reader, objURI, set);

            return objURI;
        } catch (ResourceIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceIndexException("Error generating triples", e);
        }
    }

    /**
     * For the given object, add the common core system metadata triples. This
     * will include:
     * <ul>
     * <li> object <i>model:createdDate</i></li>
     * <li> object <i>model:label</i></li>
     * <li> object <i>model:owner</i></li>
     * <li> object <i>model:state</i></li>
     * <li> object <i>view:lastModifiedDate</i></li>
     * </ul>
     */
    private void addCoreObjectTriples(DOReader r,
                                      URIReference objURI,
                                      Set<Triple> set) throws Exception {

        add(objURI, MODEL.CREATED_DATE, r.getCreateDate(), set);
        add(objURI, MODEL.LABEL, r.GetObjectLabel(), set);
        add(objURI, MODEL.OWNER, r.getOwnerId(), set);
        add(objURI, MODEL.STATE, getStateResource(r.GetObjectState()), set);
        add(objURI, VIEW.LAST_MODIFIED_DATE, r.getLastModDate(), set);
    }

    /**
     * For the given datastream, add the triples that are common for all
     * datastreams. This will include:
     * <ul>
     * <li> object <i>view:disseminates</i> datastream</li>
     * <li> datastream <i>view:disseminationType</i></li>
     * <li> datastream <i>view:isVolatile</i></li>
     * <li> datastream <i>view:lastModifiedDate</i></li>
     * <li> datastream <i>view:mimeType</i></li>
     * <li> datastream <i>model:state</i></li>
     * </ul>
     */
    private void addCoreDatastreamTriples(Datastream ds,
                                          URIReference objURI,
                                          Set<Triple> set) throws Exception {

        URIReference dsURI = new SimpleURIReference(
                new URI(objURI.getURI().toString() + "/" + ds.DatastreamID));

        add(objURI, VIEW.DISSEMINATES, dsURI, set);

        URIReference dsDissType = new SimpleURIReference(
                new URI(FEDORA.uri + "*/" + ds.DatastreamID));

        add(dsURI, VIEW.DISSEMINATION_TYPE, dsDissType, set);

        boolean isVolatile =
                ds.DSControlGrp.equals("E") || ds.DSControlGrp.equals("R");

        add(dsURI, VIEW.IS_VOLATILE, isVolatile, set);
        add(dsURI, VIEW.LAST_MODIFIED_DATE, ds.DSCreateDT, set);
        add(dsURI, VIEW.MIME_TYPE, ds.DSMIME, set);
        add(dsURI, MODEL.STATE, getStateResource(ds.DSState), set);

    }

    /**
     * Add a statement about the object for each predicate, value pair expressed
     * in the DC datastream.
     */
    private void addDCTriples(DatastreamXMLMetadata ds,
                              URIReference objURI,
                              Set<Triple> set) throws Exception {
        DCFields dc = new DCFields(ds.getContentStream());
        Map<RDFName, List<DCField>> map = dc.getMap();
        for (RDFName predicate : map.keySet()) {
            for (DCField dcField : map.get(predicate)) {
                String lang = dcField.getLang();
                if (lang == null) {
                    add(objURI, predicate, dcField.getValue(), set);
                } else {
                    add(objURI, predicate, dcField.getValue(), lang, set);
                }
            }
        }
    }

    /**
     * Adds all triples given by reader.getRelationships(null, null).
     * <p>
     * This includes everything in RELS-EXT and RELS-INT as well as the implicit
     * basic content model assertion, if any.
     */
    private void addRelationshipTriples(DOReader reader,
                                        URIReference objURI,
                                        Set<Triple> set)
            throws Exception {
        for (RelationshipTuple tuple : reader.getRelationships()) {
            ObjectNode oNode;
            if (tuple.isLiteral) {
                if (tuple.datatype != null) {
                    oNode = new SimpleLiteral(tuple.object,
                                              new URI(tuple.datatype));
                } else {
                    oNode = new SimpleLiteral(tuple.object);
                }
            } else {
                oNode = new SimpleURIReference(new URI(tuple.object));
            }
            set.add(new SimpleTriple(new SimpleURIReference(
                                             new URI(tuple.subject)),
                                     new SimpleURIReference(
                                             new URI(tuple.predicate)),
                                     oNode));
        }
    }

}
