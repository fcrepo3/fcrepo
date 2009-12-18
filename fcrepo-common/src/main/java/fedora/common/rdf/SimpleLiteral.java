/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import java.net.URI;

import org.jrdf.graph.AbstractLiteral;

/**
 * A Literal with convenient constructors.
 *
 * @author Chris Wilper
 */
public class SimpleLiteral
        extends AbstractLiteral {
    
    private static final long serialVersionUID = 1L;
    
    public SimpleLiteral(String lexicalForm) {
        super(lexicalForm);
    }
    
    public SimpleLiteral(String lexicalForm, String language) {
        super(lexicalForm, language);
    }
    
    public SimpleLiteral(String lexicalForm, URI datatypeURI) {
        super(lexicalForm, datatypeURI);
    }
    
}
