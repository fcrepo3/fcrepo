/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.resourceIndex;

import java.util.Date;
import java.util.Set;

import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;

import fedora.common.rdf.RDFName;
import fedora.common.rdf.SimpleLiteral;
import fedora.common.rdf.SimpleTriple;

import fedora.server.errors.ResourceIndexException;
import fedora.server.utilities.DateUtility;

import static fedora.common.Constants.MODEL;
import static fedora.common.Constants.RDF_XSD;

public abstract class TripleGeneratorBase {
    // Helper methods for creating RDF components

    protected RDFName getStateResource(String state)
            throws ResourceIndexException {
        if (state == null) {
            throw new ResourceIndexException("State cannot be null");
        } else if (state.equals("A")) {
            return MODEL.ACTIVE;
        } else if (state.equals("D")) {
            return MODEL.DELETED;
        } else if (state.equals("I")) {
            return MODEL.INACTIVE;
        } else {
            throw new ResourceIndexException("Unrecognized state: " + state);
        }
    }

    // Helper methods for adding triples

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       ObjectNode object,
                       Set<Triple> set) throws ResourceIndexException {
        set.add(new SimpleTriple(subject, predicate, object));
    }

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       String lexicalValue,
                       Set<Triple> set) throws Exception {
        if (lexicalValue != null) {
            set.add(new SimpleTriple(subject, predicate, new SimpleLiteral(lexicalValue)));
        }
    }

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       String lexicalValue,
                       String lang,
                       Set<Triple> set) throws Exception {
        if (lexicalValue != null) {
            set.add(new SimpleTriple(subject, predicate, new SimpleLiteral(lexicalValue, lang)));
        }
    }

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       Date dateValue,
                       Set<Triple> set) throws Exception {
        if (dateValue != null) {
            String lexicalValue = DateUtility.convertDateToXSDString(dateValue);
            ObjectNode object = new SimpleLiteral(lexicalValue,
                                                  RDF_XSD.DATE_TIME.getURI());
            set.add(new SimpleTriple(subject, predicate, object));
        }
    }

    protected void add(SubjectNode subject,
                       RDFName predicate,
                       boolean booleanValue,
                       Set<Triple> set) throws Exception {
        add(subject, predicate, Boolean.toString(booleanValue), set);
    }
}
