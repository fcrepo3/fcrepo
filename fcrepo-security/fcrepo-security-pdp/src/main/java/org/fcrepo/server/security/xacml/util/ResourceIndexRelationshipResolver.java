package org.fcrepo.server.security.xacml.util;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;

import org.trippi.TripleIterator;
import org.trippi.TrippiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fcrepo.common.MalformedPIDException;
import org.fcrepo.common.rdf.SimpleURIReference;

import org.fcrepo.server.resourceIndex.ResourceIndex;
import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pdp.MelcoePDPException;

        
        /**
         * A RelationshipResolver that resolves relationships via
         * {@link ResourceIndex#findTriples(SubjectNode, PredicateNode, ObjectNode, int)}.
         *
         * @author Benjamin Armintor
         */
public class ResourceIndexRelationshipResolver
        extends RelationshipResolverImpl {
    private static final Logger logger =
        LoggerFactory.getLogger(RelationshipResolverImpl.class);

    private ResourceIndex m_resourceIndex;
        
    public ResourceIndexRelationshipResolver(){
        super();
    }
    
    /**
     * Constructor that takes a map of parent-child predicates (relationships).
     * {@link ContextHandlerImpl} builds the map from the relationship-resolver
     * section of config-melcoe-pep.xml (in WEB-INF/classes) or the Spring config.
     *
     * @param options
     * @throws MelcoePDPException
     */
    public ResourceIndexRelationshipResolver(Map<String, String> options) {
        super(options);
    }
    public void setResourceIndex(ResourceIndex resourceIndex) {
        m_resourceIndex = resourceIndex;
    }

    @Override
    public Map<String, Set<String>> getRelationships(final String subject)
            throws MelcoeXacmlException {
        try{
            SubjectNode snode = new SimpleURIReference(new URI(getSubjectURI(subject)));
            TripleIterator triples = m_resourceIndex.findTriples(snode, null, null, 0);
            Map<String, Set<String>> result = new HashMap<String, Set<String>>();
            while (triples.hasNext()){
                Triple triple = triples.next();
                String predicate = triple.getPredicate().stringValue();
                String object = triple.getObject().stringValue();
                if (!result.containsKey(predicate)) result.put(predicate, new HashSet<String>());
                result.get(predicate).add(object);
            }
            return result;
        }
        catch (MalformedPIDException mpe){
            throw new MelcoeXacmlException(mpe);
        }
        catch (URISyntaxException use){
            throw new MelcoeXacmlException(use);
        }
        catch (TrippiException te){
            throw new MelcoeXacmlException(te);
        }
    }
    
    
}

    