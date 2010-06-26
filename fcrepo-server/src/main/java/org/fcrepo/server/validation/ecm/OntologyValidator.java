package org.fcrepo.server.validation.ecm;


import org.fcrepo.server.Context;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.utilities.xml.DOM;
import org.fcrepo.utilities.xml.XPathSelector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.Map;

/**
 * The Ontology Validator
 *
 * @author Asger Askov Blekinge
 */
public class OntologyValidator {
    private RepositoryReader doMgr;
    private Context context;

    XPathSelector xpathselector = DOM.createXPathSelector("fedora-model", "info:fedora/fedora-system:def/model#",
                                                          "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                                          "owl", "http://www.w3.org/2002/07/owl#",
                                                          "rdfs", "http://www.w3.org/2000/01/rdf-schema#");

    public OntologyValidator(RepositoryReader doMgr, Context context) {
        this.doMgr = doMgr;
        this.context = context;
    }

    /**
     * Validate the RELS-EXT stream against the ontologies from the objects content models.
     *
     * @param rdf        the content of the RELS-EXT stream
     * @param ontologies a map of contentmodelPids to their Ontology streams
     * @param validation the Validation object, to update along the way
     * @param objid      the PID of the object to validate
     * @throws ServerException if something failed.
     */
    public void validate(InputStream rdf, Map<String, InputStream> ontologies, Validation validation, String objid)
            throws ServerException {
        //First, assume that all the given ontologies are relevant in their entirety.

        //Generate list of relations in the object
        Document rdfDoc = DOM.streamToDOM(rdf, true);

        NodeList relationsNodelist = xpathselector
                .selectNodeList(rdfDoc, "/rdf:RDF/rdf:Description[@rdf:about='info:fedora/" + objid + "']/*");
        //This is the list of relation nodes to process


        for (String cm : ontologies.keySet()) {
            InputStream ontologyStream = ontologies.get(cm);
            //Then, parse the ontologies
            Document ontologyDoc = DOM.streamToDOM(ontologyStream, true);

            //Check the object against the ontology
            checkOntology(relationsNodelist, ontologyDoc, cm, validation);
        }
    }

    /**
     * Check the object relations against a specific ontology stream from a content model
     *
     * @param relations    the object relations
     * @param ontology     the ontology document
     * @param contentModel the content model pid
     * @param validation   the validation object to update
     * @throws ServerException if something failed
     */
    void checkOntology(NodeList relations, Document ontology, String contentModel, Validation validation)
            throws ServerException {
        // Identify the class
        Node classNode = xpathselector
                .selectNode(ontology, "/rdf:RDF/owl:Class[@rdf:about='info:fedora/" + contentModel + "_class']");

        // Identify the declared relations for that class
        NodeList objectRelationDeclarations = xpathselector
                .selectNodeList(ontology, "/rdf:RDF/owl:ObjectProperty/@rdf:about");

        //Interate through the relation declarations
        for (int i = 0; i < objectRelationDeclarations.getLength(); i++) {

            //Find the relation name
            String relationName = objectRelationDeclarations.item(i).getNodeValue();

            //The restrictions imposed on this relation
            NodeList restrictions =
                    xpathselector.selectNodeList(classNode,
                                                 "rdfs:subClassOf/" +
                                                 "owl:Restriction" +
                                                 "[owl:onProperty/@rdf:resource='" + relationName + "']");
            //Iterate the restrictions
            for (int j = 0; j < restrictions.getLength(); j++) {
                Node restriction = restrictions.item(j);


                //Each restriction can be only one of these types
                String allValuesFromRestriction =
                        xpathselector.selectString(restriction, "owl:allValuesFrom/@rdf:resource");
                String someValuesFromRestriction =
                        xpathselector.selectString(restriction, "owl:someValuesFrom/@rdf:resource");
                String cardinality = selectTextualNode(restriction, "owl:cardinality");
                String minCardinality = selectTextualNode(restriction, "owl:minCardinality");
                String maxCardinality = selectTextualNode(restriction, "owl:maxCardinality");

                // Check these restrictions.
                if (!allValuesFromRestriction.isEmpty()) {
                    checkAllValuesFrom(allValuesFromRestriction, relationName, relations, validation, contentModel);
                } else if (!someValuesFromRestriction.isEmpty()) {
                    checkSomeValuesFrom(someValuesFromRestriction, relationName, relations, validation, contentModel);
                } else if (!cardinality.isEmpty()) {
                    checkCardinality(cardinality, relationName, relations, validation, contentModel);
                } else if (!minCardinality.isEmpty()) {
                    checkMinCardinality(minCardinality, relationName, relations, validation, contentModel);
                } else if (!maxCardinality.isEmpty()) {
                    checkMaxCardinality(maxCardinality, relationName, relations, validation, contentModel);
                }
            }
        }
    }

    /**
     * Check a max cardinality restriction.
     *
     * @param maxCardinality  the max cardinality (String to be parsed to int)
     * @param relationName    the name of the restricted relation
     * @param objectRelations the object relations
     * @param validation      the validate object to update
     * @param contentModel    the content model pid (for reporting)
     */
    private void checkMaxCardinality(String maxCardinality, String relationName, NodeList objectRelations,
                                     Validation validation, String contentModel) {
        int count = countRelations(relationName, objectRelations);
        int cardint = Integer.parseInt(maxCardinality);
        if (count > cardint) {
            validation.setValid(false);
            validation.getObjectProblems()
                    .add("Contentmodel '" + contentModel + "' require that the object must have less than " +
                         "'" + cardint + "' relations with the name '" + relationName + "' but" +
                         " the object has '" + count + "'");
        }

    }

    /**
     * Check a min cardinality restriction.
     *
     * @param minCardinality the min cardinality (String to be parsed to int)
     * @param relationName   the name of the restricted relation
     * @param relations      the object relations
     * @param validation     the validate object to update
     * @param contentModel   the content model pid (for reporting)
     */
    private void checkMinCardinality(String minCardinality, String relationName, NodeList relations,
                                     Validation validation, String contentModel) {
        int count = countRelations(relationName, relations);
        int cardint = Integer.parseInt(minCardinality);
        if (count < cardint) {
            validation.setValid(false);
            validation.getObjectProblems()
                    .add("Contentmodel '" + contentModel + "' require that the object must have " +
                         "'" + cardint + "' or more relations with the name '" + relationName + "' but" +
                         " the object has only '" + count + "'");
        }

    }

    /**
     * Check an exact cardinality restriction.
     *
     * @param cardinality  the min cardinality (String to be parsed to int)
     * @param relationName the name of the restricted relation
     * @param relations    the object relations
     * @param validation   the validate object to update
     * @param contentModel the content model pid (for reporting)
     */
    private void checkCardinality(String cardinality, String relationName, NodeList relations,
                                  Validation validation, String contentModel) {
        int count = countRelations(relationName, relations);
        int cardint = Integer.parseInt(cardinality);
        if (count != cardint) {
            validation.setValid(false);
            validation.getObjectProblems()
                    .add("Contentmodel '" + contentModel + "' require that the object must have " +
                         "exactly '" + cardint + "' relations with the name '" + relationName + "' but" +
                         " the object has '" + count + "'");
        }
    }

    /**
     * Private utility method. Counts the number of relations with a given name in a list of relatiosn
     *
     * @param relationName    the relation name
     * @param objectRelations the list of relations
     * @return the number of relations with relationName in the list
     */
    private int countRelations(String relationName, NodeList objectRelations) {
        int count = 0;
        for (int i = 0; i < objectRelations.getLength(); i++) {
            Node relation = objectRelations.item(i);
            String objectRelationName = relation.getNamespaceURI() + relation.getLocalName();//Qualified name
            if (objectRelationName.equals(relationName)) {//This is one of the restricted relations
                count++;
            }
        }
        return count;

    }

    /**
     * The the somevaluesfrom restrictrion. This restriction is odd, in that it requires that
     * <ul>
     * <li>There must be at least one relation with the given name
     * <li>Of of the relations should refer to a target with the correct content model
     * </ul>
     *
     * @param someValuesFromRestriction the target content model
     * @param relationName              the restrictid relation
     * @param objectRelations           the objects relations
     * @param validation                the validation object
     * @param contentModel              the content model
     * @throws ServerException if something failed
     */
    private void checkSomeValuesFrom(String someValuesFromRestriction, String relationName,
                                     NodeList objectRelations, Validation validation, String contentModel)
            throws ServerException {
        if (someValuesFromRestriction.endsWith("_class")) {
            someValuesFromRestriction =
                    someValuesFromRestriction.substring(0, someValuesFromRestriction.length() - "_class".length());
        }
        boolean foundGoodRelation = false;
        for (int i = 0; i < objectRelations.getLength(); i++) {
            Node relation = objectRelations.item(i);
            String objectRelationName = relation.getNamespaceURI() + relation.getLocalName();//Qualified name
            if (objectRelationName.equals(relationName)) {//This is one of the restricted relations
                String target = xpathselector.selectString(relation, "@rdf:resource");
                if (target.isEmpty()) {//This was a literal relation, disregard
                    continue;
                }
                DOReader targetObject = doMgr.getReader(false, context, toPid(target));
                if (targetObject.getContentModels().contains(someValuesFromRestriction)) {
                    foundGoodRelation = true;
                    break;
                }
            }
        }
        if (!foundGoodRelation) {
            validation.setValid(false);
            validation.getObjectProblems()
                    .add("Contentmodel '" + contentModel + "' require that this object should have " +
                         "at least one relation of the name '" + relationName +
                         "' to an object with the contentmodel '" + someValuesFromRestriction + "'");
        }

    }

    /**
     * The allValuesFrom restriction. Require that all relations with the given name refer to objects with the given content model
     *
     * @param allValuesFromRestriction the required content model
     * @param relationName             the restricted relation
     * @param objectRelations          the object relations
     * @param validation               the validation object
     * @param contentModel             the content model pid
     * @throws ServerException if something failed.
     */
    private void checkAllValuesFrom(String allValuesFromRestriction, String relationName,
                                    NodeList objectRelations, Validation validation, String contentModel)
            throws ServerException {
        if (allValuesFromRestriction.endsWith("_class")) {
            allValuesFromRestriction =
                    allValuesFromRestriction.substring(0, allValuesFromRestriction.length() - "_class".length());
        }
        for (int i = 0; i < objectRelations.getLength(); i++) {
            Node relation = objectRelations.item(i);
            String objectRelationName = relation.getNamespaceURI() + relation.getLocalName();
            if (objectRelationName.equals(relationName)) {//This is one of the restricted relations
                String target = xpathselector.selectString(relation, "@rdf:resource");
                if (target.isEmpty()) {//This was a literal relation, disregard
                    continue;
                }
                DOReader targetObject = doMgr.getReader(false, context, toPid(target));
                if (!targetObject.getContentModels().contains(allValuesFromRestriction)) {
                    validation.setValid(false);
                    validation.getObjectProblems().add("Relation '" + relationName + "' refers to resource '"
                                                       + targetObject.GetObjectPID() + "' which, by content model '" +
                                                       contentModel + "' should be of the type '" +
                                                       allValuesFromRestriction + "'");

                }
            }
        }
    }

    private String toPid(String target) {
        if (target != null && target.startsWith("info:fedora/")) {
            return target.substring("info:fedora/".length());
        }
        return target;
    }


    private String selectTextualNode(Node doc, String xpath) {
        Node cardinalityRestrictionNode =
                xpathselector.selectNode(doc, xpath);
        String cardinalityRestriction;
        if (cardinalityRestrictionNode != null) {
            cardinalityRestriction = cardinalityRestrictionNode.getTextContent();
            return cardinalityRestriction;
        }
        return "";
    }

}
