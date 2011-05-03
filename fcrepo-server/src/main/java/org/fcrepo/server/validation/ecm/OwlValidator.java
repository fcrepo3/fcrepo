package org.fcrepo.server.validation.ecm;

import org.fcrepo.server.Context;
import org.fcrepo.server.errors.LowlevelStorageException;
import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.DOReader;
import org.fcrepo.server.storage.RepositoryReader;
import org.fcrepo.server.storage.types.Datastream;
import org.fcrepo.server.storage.types.RelationshipTuple;
import org.fcrepo.server.storage.types.Validation;
import org.fcrepo.server.validation.ecm.jaxb.DsCompositeModel;
import org.fcrepo.server.validation.ecm.jaxb.DsTypeModel;
import org.fcrepo.utilities.xml.DOM;
import org.fcrepo.utilities.xml.XPathSelector;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Aug 4, 2010
 * Time: 10:50:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class OwlValidator {
    private RepositoryReader doMgr;


    XPathSelector xpathselector = DOM.createXPathSelector("fedora-model", "info:fedora/fedora-system:def/model#",
                                                          "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                                                          "owl", "http://www.w3.org/2002/07/owl#",
                                                          "rdfs", "http://www.w3.org/2000/01/rdf-schema#");


    public OwlValidator(RepositoryReader doMgr) {
        //To change body of created methods use File | Settings | File Templates.
        this.doMgr = doMgr;
    }


    /**
     * This is one complex method.
     * <p/>
     * 1. retrieve the list of content models from the object.
     * 2. retrieve the ontology datastreams from each of these content models
     * 3.
     *
     * @param context
     * @param asOfDateTime
     * @param currentObjectReader
     * @param validation
     * @throws org.fcrepo.server.errors.ServerException
     *
     */
    public void validate(Context context, Date asOfDateTime, DOReader currentObjectReader,
                         Validation validation) throws ServerException {


        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();

        List<String> contentmodels = currentObjectReader.getContentModels();

        Map<String, InputStream> ontologies = new HashMap<String, InputStream>();
        for (String contentmodel : contentmodels) {
            contentmodel = contentmodel.substring("info:fedora/".length());
            DOReader contentmodelReader;
            try {
                contentmodelReader = doMgr.getReader(false, context, contentmodel);
            } catch (LowlevelStorageException e) {//content model could not be found
                continue;
            }

            if (asOfDateTime != null) { //disregard content models created after the asOfDateTime
                if (!contentmodelReader.getCreateDate().before(asOfDateTime)) {
                    continue;
                }
            }

            Datastream ontologyDS = contentmodelReader.GetDatastream("ONTOLOGY", asOfDateTime);

            if (ontologyDS == null) {//No ontology in the content model, continue
                continue;
            }
            InputStream ontologyStream = ontologyDS.getContentStream();
            try {
                owlManager.loadOntologyFromOntologyDocument(ontologyStream);
            } catch (OWLOntologyCreationException e) {
                //TODO
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        OWLOntologyMerger merger = new OWLOntologyMerger(owlManager);
        IRI mergedOntologyIRI = IRI.create("http://www.semanticweb.com/mymergedont");
        OWLOntology mergedOntology = null;
        try {
            mergedOntology = merger.createMergedOntology(owlManager, mergedOntologyIRI);
        } catch (OWLOntologyCreationException e) {
            //TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        //Make a new restrition visitor.
        RestrictionVisitor restrictionVisitor =
                new RestrictionVisitor(Collections.singleton(mergedOntology));
        Set<RelationshipTuple> relations = currentObjectReader.getRelationships();


        for (String contentmodel : contentmodels) {
            List<String> datastreamNames = getDatastreamNames(context, contentmodel, asOfDateTime);
            for (String datastreamName : datastreamNames) {
                IRI datastreamDeclaration = toIRI(contentmodel, datastreamName);
                OWLClass datastreamClass = owlManager.getOWLDataFactory().getOWLClass(datastreamDeclaration);
                for (OWLSubClassOfAxiom ax : mergedOntology.getSubClassAxiomsForSubClass(datastreamClass)) {
                    OWLClassExpression superCls = ax.getSuperClass();
                    // Ask our superclass to accept a visit from the RestrictionVisitor - if it is an
                    // existential restiction then our restriction visitor will answer it - if not our
                    // visitor will ignore it
                    superCls.accept(restrictionVisitor);
                }
                String datastream = "info:fedora/" + currentObjectReader.GetObjectPID() + "/" + datastreamName;
/*
                NodeList relationsNodes = xpathselector
                        .selectNodeList(relsint, "/rdf:RDF/rdf:Description[@rdf:about='" + datastream + "']*/
/*");
*/
                Set<RelationshipTuple> relationsAbout = getRelationsSubjectTo(relations, datastream);

                checkMinCardinality(datastream, relationsAbout, restrictionVisitor, validation);
                checkMaxCardinality(datastream, relationsAbout, restrictionVisitor, validation);
                checkExactCardinality(datastream, relationsAbout, restrictionVisitor, validation);
                checkSomeValuesFrom(datastream, relationsAbout, restrictionVisitor, validation, context);
                checkAllValuesFrom(datastream, relationsAbout, restrictionVisitor, validation, context);
                restrictionVisitor.reset();
            }

        }


        for (String contentmodel : contentmodels) {
            IRI objectDeclaration = toIRI(contentmodel, null);
            OWLClass objectClass = owlManager.getOWLDataFactory().getOWLClass(objectDeclaration);
            for (OWLSubClassOfAxiom ax : mergedOntology.getSubClassAxiomsForSubClass(objectClass)) {
                OWLClassExpression superCls = ax.getSuperClass();
                // Ask our superclass to accept a visit from the RestrictionVisitor - if it is an
                // existential restiction then our restriction visitor will answer it - if not our
                // visitor will ignore it
                superCls.accept(restrictionVisitor);
            }

            String pid = "info:fedora/" + currentObjectReader.GetObjectPID();
            Set<RelationshipTuple> relationsAbout = getRelationsSubjectTo(relations, pid);

            checkMinCardinality(pid, relationsAbout, restrictionVisitor, validation);
            checkMaxCardinality(pid, relationsAbout, restrictionVisitor, validation);
            checkExactCardinality(pid, relationsAbout, restrictionVisitor, validation);
            checkSomeValuesFrom(pid, relationsAbout, restrictionVisitor, validation, context);
            checkAllValuesFrom(pid, relationsAbout, restrictionVisitor, validation, context);
            restrictionVisitor.reset();
        }

    }

    private Set<RelationshipTuple> getRelationsSubjectTo(Set<RelationshipTuple> relations, String datastream) {
        HashSet<RelationshipTuple> found = new HashSet<RelationshipTuple>();
        for (RelationshipTuple relation : relations) {
            if (relation.subject.equals(datastream)){
                found.add(relation);
            }
        }
        return found;
    }

    private void checkAllValuesFrom(String subject, Set<RelationshipTuple> relations,
                                    RestrictionVisitor restrictionVisitor, Validation validation, Context context)
            throws ServerException {

        Map<OWLObjectProperty, OWLClass> map = restrictionVisitor.getAllValuesFrom();
        for (OWLObjectProperty owlObjectProperty : map.keySet()) {
            String ontologyrelation = owlObjectProperty.getIRI().toString();
            OWLClass requiredclass = map.get(owlObjectProperty);
            String requiredTarget = requiredclass.getIRI().toString();

            for (RelationshipTuple relation : relations) {
                String objectRelationName = relation.predicate;
                if (objectRelationName.equals(ontologyrelation)) {//This is one of the restricted relations

                    String target = relation.object;
                    List<String> classes = getClassesOfTarget(target, context);
                    boolean found = false;
                    for (String aClass : classes) {
                        if (aClass.equals(requiredclass.getIRI().toString())){
                            found = true;
                            break;
                        }
                    }
                    if (found == false) {
                        validation.setValid(false);
                        validation.getObjectProblems().add(Errors.allValuesFromViolation(subject, ontologyrelation,requiredTarget));

                    }
                }
            }
        }
    }

    private List<String> getClassesOfTarget(String target, Context context) throws ServerException {

        List<String> classes = new ArrayList<String>();
        if (!target.startsWith("info:fedora/")) {
            return new ArrayList<String>();
        } else {
            target = target.substring("info:fedora/".length());
        }
        int lastIndexOfSlash = target.lastIndexOf("/");
        String targetPid;

        String dsname = "";

        if (lastIndexOfSlash > 0) {//target is a datastream

            targetPid = target.substring(0, lastIndexOfSlash);
            dsname = "datastreams/"+target.substring(lastIndexOfSlash+1)+"/";
        } else { //target is an object
            targetPid = target;
        }
        DOReader targetReader = doMgr.getReader(false, context, targetPid);
        List<String> targetContentModels = targetReader.getContentModels();
        for (String targetContentModel : targetContentModels) {
            targetContentModel = targetContentModel+"#" +dsname+"class";
            classes.add(targetContentModel);
        }
        return classes;

    }

    private void checkSomeValuesFrom(String subject, Set<RelationshipTuple> relations,
                                     RestrictionVisitor restrictionVisitor, Validation validation, Context context)
            throws ServerException {

        Map<OWLObjectProperty, OWLClass> map = restrictionVisitor.getSomeValuesFrom();
        for (OWLObjectProperty owlObjectProperty : map.keySet()) {
            String ontologyrelation = owlObjectProperty.getIRI().toString();
            OWLClass requiredclass = map.get(owlObjectProperty);
            String requiredTarget = requiredclass.getIRI().toString();

            int count = countRelations(ontologyrelation, relations);
            if (count < 1) {
                validation.setValid(false);

                validation.getObjectProblems().add(Errors.someValuesFromViolationNoSuchRelation(subject, ontologyrelation,requiredTarget));
                continue;
            }


            boolean found = false;
            for (RelationshipTuple relation : relations) {
                String objectRelationName = relation.predicate;
                if (objectRelationName.equals(ontologyrelation)) {//This is one of the restricted relations

                    String target = relation.object;
                    List<String> classes = getClassesOfTarget(target, context);

                    for (String aClass : classes) {
                        if (aClass.equals(requiredclass.getIRI().toString())){
                            found = true;
                            break;
                        }
                    }
                    if (found == false) {
                        validation.setValid(false);
                        validation.getObjectProblems().add(
                                Errors.someValuesFromViolationWrongClassOfTarget(subject,
                                                                                 ontologyrelation,
                                                                                 requiredTarget));
                    }
                    if (found) {
                        break;
                    }
                }
            }
        }


    }


    private void checkMinCardinality(String subject, Set<RelationshipTuple> relations,
                                     RestrictionVisitor restrictionVisitor, Validation validation) {

        Map<OWLObjectProperty, Integer> map = restrictionVisitor.getMinCardinality();
        for (OWLObjectProperty owlObjectProperty : map.keySet()) {
            String ontologyrelation = owlObjectProperty.getIRI().toString();
            int count = countRelations(ontologyrelation, relations);
            int min = map.get(owlObjectProperty);
            if (count < min) {
                validation.setValid(false);
                validation.getObjectProblems()
                        .add(Errors.minCardinalityViolation(subject, ontologyrelation,min));
            }
        }


    }

    private void checkMaxCardinality(String subject, Set<RelationshipTuple> relations,
                                     RestrictionVisitor restrictionVisitor, Validation validation) {

        Map<OWLObjectProperty, Integer> map = restrictionVisitor.getMaxCardinality();
        for (OWLObjectProperty owlObjectProperty : map.keySet()) {
            String ontologyrelation = owlObjectProperty.getIRI().toString();
            int count = countRelations(ontologyrelation, relations);
            int max = map.get(owlObjectProperty);
            if (count > max) {
                validation.setValid(false);
                validation.getObjectProblems()
                        .add(Errors.maxCardinalityViolation(subject, ontologyrelation,max));
            }
        }


    }


    private void checkExactCardinality(String subject, Set<RelationshipTuple> relations,
                                       RestrictionVisitor restrictionVisitor, Validation validation) {

        Map<OWLObjectProperty, Integer> map = restrictionVisitor.getCardinality();
        for (OWLObjectProperty owlObjectProperty : map.keySet()) {
            String ontologyrelation = owlObjectProperty.getIRI().toString();
            int count;
            count = countRelations(ontologyrelation, relations);
            Integer exact = map.get(owlObjectProperty);
            if (count != exact) {
                validation.setValid(false);
                validation.getObjectProblems()
                        .add(Errors.exactCardinalityViolation(subject, ontologyrelation,exact));
            }
        }


    }


    /**
     * Private utility method. Counts the number of relations with a given name in a list of relatiosn
     *
     * @param relationName    the relation name
     * @param objectRelations the list of relations
     * @return the number of relations with relationName in the list
     */
    private int countRelations(String relationName, Set<RelationshipTuple> objectRelations) {

        int count = 0;
        if (objectRelations == null) {
            return 0;
        }
        for (RelationshipTuple objectRelation : objectRelations) {
            if (objectRelation.predicate.equals(relationName)) {//This is one of the restricted relations
                count++;
            }

        }
        return count;
    }


    private IRI toIRI(String contentmodel, String datastreamName) {
        if (!contentmodel.startsWith("info:fedora/")) {
            contentmodel = "info:fedora/" + contentmodel;
        }
        if (datastreamName != null) {
            datastreamName = "datastreams/" + datastreamName+"/";
        } else {
            datastreamName = "";
        }
        return IRI.create(contentmodel + "#"+datastreamName+"class");
    }

    private List<String> getDatastreamNames(Context context, String contentmodel, Date asOfDateTime)
            throws ServerException {

        ArrayList<String> names = new ArrayList<String>();
        if (contentmodel.startsWith("info:fedora/")) {
            contentmodel = contentmodel.substring("info:fedora/".length());
        }
        DOReader reader = doMgr.getReader(false, context, contentmodel);
        Datastream dscompmodelDS = reader.GetDatastream("DS-COMPOSITE-MODEL", asOfDateTime);
        if (dscompmodelDS == null) {//NO ds composite model, thats okay, continue to next content model
            return names;
        }
        DsCompositeModel dscompobject = JAXB.unmarshal(dscompmodelDS.getContentStream(context), DsCompositeModel.class);

        for (DsTypeModel typeModel : dscompobject.getDsTypeModel()) {
            names.add(typeModel.getID());
        }
        return names;
    }


    /**
     * Visits restrictions and collects the properties which are restricted
     */
    private static class RestrictionVisitor extends OWLClassExpressionVisitorAdapter {

        private boolean processInherited = true;

        private Set<OWLClass> processedClasses;


        private Map<OWLObjectProperty, OWLClass> someValuesFrom;
        private Map<OWLObjectProperty, OWLClass> allValuesFrom;
        private Map<OWLObjectProperty, Integer> minCardinality;
        private Map<OWLObjectProperty, Integer> cardinality;
        private Map<OWLObjectProperty, Integer> maxCardinality;


        private Set<OWLOntology> onts;

        public RestrictionVisitor(Set<OWLOntology> onts) {
            someValuesFrom = new HashMap<OWLObjectProperty, OWLClass>();
            allValuesFrom = new HashMap<OWLObjectProperty, OWLClass>();
            minCardinality = new HashMap<OWLObjectProperty, Integer>();
            cardinality = new HashMap<OWLObjectProperty, Integer>();
            maxCardinality = new HashMap<OWLObjectProperty, Integer>();

            processedClasses = new HashSet<OWLClass>();
            this.onts = onts;
        }


        public void setProcessInherited(boolean processInherited) {
            this.processInherited = processInherited;
        }


        public void visit(OWLClass desc) {
            if (processInherited && !processedClasses.contains(desc)) {
                // If we are processing inherited restrictions then
                // we recursively visit named supers. Note that we
                // need to keep track of the classes that we have processed
                // so that we don't get caught out by cycles in the taxonomy
                processedClasses.add(desc);
                for (OWLOntology ont : onts) {
                    for (OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass(desc)) {
                        ax.getSuperClass().accept(this);
                    }
                }
            }
        }


        public void reset() {
            processedClasses.clear();
            someValuesFrom.clear();
            allValuesFrom.clear();
            minCardinality.clear();
            cardinality.clear();
            maxCardinality.clear();
        }


        public void visit(OWLObjectExactCardinality desc) {
            cardinality.put(desc.getProperty().asOWLObjectProperty(), desc.getCardinality());
        }

        public void visit(OWLObjectMaxCardinality desc) {
            maxCardinality.put(desc.getProperty().asOWLObjectProperty(), desc.getCardinality());
        }

        public void visit(OWLObjectMinCardinality desc) {
            minCardinality.put(desc.getProperty().asOWLObjectProperty(), desc.getCardinality());
        }

        public void visit(OWLObjectAllValuesFrom desc) {
            allValuesFrom.put(desc.getProperty().asOWLObjectProperty(), desc.getFiller().asOWLClass());
        }

        public void visit(OWLObjectSomeValuesFrom desc) {
            // This method gets called when a class expression is an
            // existential (someValuesFrom) restriction and it asks us to visit it
            someValuesFrom.put(desc.getProperty().asOWLObjectProperty(), desc.getFiller().asOWLClass());
        }


        public void visit(OWLDataSomeValuesFrom desc) {
        }


        public void visit(OWLDataAllValuesFrom desc) {
        }


        public void visit(OWLDataMinCardinality desc) {

        }


        public void visit(OWLDataExactCardinality desc) {
        }


        public void visit(OWLDataMaxCardinality desc) {
        }


        public Map<OWLObjectProperty, OWLClass> getSomeValuesFrom() {
            return someValuesFrom;
        }

        public Map<OWLObjectProperty, OWLClass> getAllValuesFrom() {
            return allValuesFrom;
        }

        public Map<OWLObjectProperty, Integer> getMinCardinality() {
            return minCardinality;
        }

        public Map<OWLObjectProperty, Integer> getCardinality() {
            return cardinality;
        }

        public Map<OWLObjectProperty, Integer> getMaxCardinality() {
            return maxCardinality;
        }
    }

}
