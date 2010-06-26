package org.fcrepo.server.storage.types;

import java.util.*;

/**
 * This class represents the result of one validation run on an object.
 *
 * @author Asger Askov Blekinge
 * @version $Id$
 */
public class Validation {


    public Validation() {
    }

    public Validation(String pid) {
        this.pid = pid;
    }

    /**
     * The pid of the validated object
     */
    private String pid;


    /**
     * True or false, if the object was valid
     */
    private boolean valid = true;


    /**
     * The list of content models of the object
     */
    private List<String> contentModels = new ArrayList<String>();

    /**
     * The date, if specified, that the objects was regarded as
     */
    private Date asOfDateTime;

    /**
     * List of found problems concerning the object itself. Mostly this will be problems regarding the object relations.
     */
    private List<String> objectProblems = new ArrayList<String>();


    /**
     * Map of datastream names to found problems. This will be schema validation errors, and RELS-INT errors.
     */
    private Map<String, List<String>> datastreamProblems = new HashMap<String, List<String>>();


    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getContentModels() {
        return contentModels;
    }

    public void setContentModels(List<String> contentModels) {
        this.contentModels = contentModels;
    }

    public Date getAsOfDateTime() {
        return asOfDateTime;
    }

    public void setAsOfDateTime(Date asOfDateTime) {
        this.asOfDateTime = asOfDateTime;
    }

    public List<String> getObjectProblems() {
        return objectProblems;
    }

    public void setObjectProblems(List<String> objectProblems) {
        this.objectProblems = objectProblems;
    }

    public Map<String, List<String>> getDatastreamProblems() {
        return datastreamProblems;
    }

    public void setDatastreamProblems(Map<String, List<String>> datastreamProblems) {
        this.datastreamProblems = datastreamProblems;
    }

    public List<String> getDatastreamProblems(String datastreamID) {
        List<String> problems = datastreamProblems.get(datastreamID);
        if (problems == null) {
            problems = new ArrayList<String>();
            datastreamProblems.put(datastreamID, problems);
        }
        return problems;
    }
}
