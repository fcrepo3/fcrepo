/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import org.jrdf.graph.AbstractTriple;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;

/**
 * A Triple with a convenient constructor.
 *
 * @author Chris Wilper
 */
public class SimpleTriple
        extends AbstractTriple {
    
    private static final long serialVersionUID = 1L;
    
    public SimpleTriple(SubjectNode subjectNode,
                      PredicateNode predicateNode,
                      ObjectNode objectNode) {
        this.subjectNode = subjectNode;
        this.predicateNode = predicateNode;
        this.objectNode = objectNode;
    }
    
}
