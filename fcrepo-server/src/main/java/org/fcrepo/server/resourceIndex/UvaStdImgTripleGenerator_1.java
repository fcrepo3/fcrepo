/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.resourceIndex;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import org.fcrepo.common.Constants;
import org.fcrepo.common.Models;
import org.fcrepo.common.PID;
import org.fcrepo.common.rdf.RDFName;
import org.fcrepo.common.rdf.SimpleLiteral;
import org.fcrepo.common.rdf.SimpleTriple;
import org.fcrepo.common.rdf.SimpleURIReference;

import org.fcrepo.server.errors.ResourceIndexException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.server.utilities.DCField;
import org.fcrepo.server.utilities.DCFields;



/**
 * Generates testing RDF triples for Fedora 3.0 objects implementing info:fedora/demo:UVA_STD_IMAGE_1.
 *
 * @author Chris Wilper
 * @author Benjamin Armintor
 */
public class UvaStdImgTripleGenerator_1
        extends TripleGeneratorBase
        implements Constants, TripleGenerator {

    public static final String TEST_PREDICATE = "info:fedora/fedora-system:test/tests#tripleGenerator";
    
    private static URI PREDICATE = URI.create(TEST_PREDICATE);
    /**
     * {@inheritDoc}
     */
    public Set<Triple> getTriplesForObject(DOReader reader)
            throws ResourceIndexException {

        Set<Triple> set = new HashSet<Triple>();
        try{
        set.add(new SimpleTriple(new SimpleURIReference(
                                                        new URI(Constants.FEDORA.uri.concat(reader.GetObjectPID()))),
                                                        new SimpleURIReference(PREDICATE),
                                                        new SimpleLiteral("true")));
        }
        catch (ServerException e){
            throw new ResourceIndexException(e.getLocalizedMessage(),e);
        }
        catch (URISyntaxException e){
            throw new ResourceIndexException(e.getLocalizedMessage(),e);
        }
        return set;
    }


}
