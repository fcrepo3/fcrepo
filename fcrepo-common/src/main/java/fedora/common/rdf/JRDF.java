/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.common.rdf;

import java.net.URI;

import org.jrdf.graph.Literal;
import org.jrdf.graph.Node;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.URIReference;

/**
 * Utility methods for working with JRDF.
 * 
 * @author Chris Wilper
 */
public abstract class JRDF {

    /**
     * Tells whether the given nodes are equivalent.
     * <p>
     * Nodes are equivalent if they are both resources or both literals and they
     * match according to the rules of those types.
     * 
     * @param n1
     *        first node.
     * @param n2
     *        second node.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameNode(Node n1, Node n2) {
        if (n1 instanceof URIReference && n2 instanceof URIReference) {
            return sameResource((URIReference) n1, (URIReference) n2);
        } else if (n1 instanceof Literal && n2 instanceof Literal) {
            return sameLiteral((Literal) n1, (Literal) n2);
        } else {
            return false;
        }
    }

    /**
     * Tells whether the given resources are equivalent.
     * <p>
     * Two resources are equivalent if their URIs match.
     * 
     * @param u1
     *        first resource.
     * @param u2
     *        second resource.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameResource(URIReference u1, URIReference u2) {
        return sameResource(u1, u2.getURI().toString());
    }

    /**
     * Tells whether the given resources are equivalent, with one given as a URI
     * string.
     * 
     * @param u1
     *        first resource.
     * @param u2
     *        second resource, given as a URI string.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameResource(URIReference u1, String u2) {
        return u1.getURI().toString().equals(u2);
    }

    /**
     * Tells whether the given literals are equivalent.
     * <p>
     * Two literals are equivalent if they have the same lexical value, language
     * (which may be unspecified), and datatype (which may be unspecified).
     * 
     * @param l1
     *        first literal.
     * @param l2
     *        second literal.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameLiteral(Literal l1, Literal l2) {
        String type = null;
        URI l2Type = l2.getDatatypeURI();
        if (l2Type != null) {
            type = l2Type.toString();
        }
        return sameLiteral(l1, l2.getLexicalForm(), type, l2.getLanguage());
    }

    /**
     * Tells whether the given literals are equivalent, with one given as a set
     * of simple values.
     * 
     * @param l1
     *        first literal.
     * @param l2
     *        second literal's lexical value.
     * @param type
     *        second literal's datatype URI string, if applicable.
     * @param lang
     *        second literal's language tag string, if applicable.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameLiteral(Literal l1,
                                      String l2,
                                      String type,
                                      String lang) {
        if (l1.getLexicalForm().equals(l2) && eq(l1.getLanguage(), lang)) {
            if (l1.getDatatypeURI() == null) {
                return type == null;
            } else {
                return type != null
                        && type.equals(l1.getDatatypeURI().toString());
            }
        } else {
            return false;
        }
    }

    /**
     * Tells whether the given subjects are equivalent.
     * <p>
     * Two subjects are equivalent if they are both resources and they match
     * according to the rules for resources.
     * 
     * @param s1
     *        first subject.
     * @param s2
     *        second subject.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameSubject(SubjectNode s1, SubjectNode s2) {
        if (s1 instanceof URIReference && s2 instanceof URIReference) {
            return sameResource((URIReference) s1, (URIReference) s2);
        } else {
            return false;
        }
    }

    /**
     * Tells whether the given subjects are equivalent, with one given as a URI
     * string.
     * 
     * @param s1
     *        first subject.
     * @param s2
     *        second subject, given as a URI string.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameSubject(SubjectNode s1, String s2) {
        if (s1 instanceof URIReference) {
            return sameResource((URIReference) s1, s2);
        } else {
            return false;
        }
    }

    /**
     * Tells whether the given predicates are equivalent.
     * <p>
     * Two predicates are equivalent if they match according to the rules for
     * resources.
     * 
     * @param p1
     *        first predicate.
     * @param p2
     *        second predicate.
     * @return true if equivalent, false otherwise.
     */
    public static boolean samePredicate(PredicateNode p1, PredicateNode p2) {
        return sameResource((URIReference) p1, (URIReference) p2);
    }

    /**
     * Tells whether the given predicates are equivalent, with one given as a
     * URI string.
     * 
     * @param p1
     *        first predicate.
     * @param p2
     *        second predicate, given as a URI string.
     * @return true if equivalent, false otherwise.
     */
    public static boolean samePredicate(PredicateNode p1, String p2) {
        return sameResource((URIReference) p1, p2);
    }

    /**
     * Tells whether the given objects are equivalent.
     * <p>
     * Two objects are equivalent if they match according to the rules for
     * nodes.
     * 
     * @param o1
     *        first object.
     * @param o2
     *        second object.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameObject(ObjectNode o1, ObjectNode o2) {
        return sameNode(o1, o2);
    }

    /**
     * Tells whether the given objects are equivalent, with one given as a set
     * of simple values.
     * 
     * @param o1
     *        first node.
     * @param o2
     *        second node URI (if isLiteral is false) or lexical value (if
     *        isLiteral is true)
     * @param isLiteral
     *        whether the second node is a literal.
     * @param type
     *        second literal's datatype URI string, if applicable.
     * @param lang
     *        second literal's language tag string, if applicable.
     * @return true if equivalent, false otherwise.
     */
    public static boolean sameObject(ObjectNode o1,
                                     String o2,
                                     boolean isLiteral,
                                     String type,
                                     String lang) {
        if (o1 instanceof URIReference) {
            return sameResource((URIReference) o1, o2);
        } else if (o1 instanceof Literal || isLiteral) {
            return sameLiteral((Literal) o1, o2, type, lang);
        } else {
            return false;
        }
    }

    // test for equality, accounting for null values
    private static boolean eq(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return b != null && a.equals(b);
        }
    }

}
