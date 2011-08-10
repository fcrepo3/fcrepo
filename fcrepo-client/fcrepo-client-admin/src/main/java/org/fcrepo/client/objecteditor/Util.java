/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.client.objecteditor;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.io.IOException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.math.BigInteger;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.fcrepo.client.Administrator;
import org.fcrepo.client.objecteditor.types.DatastreamInputSpec;
import org.fcrepo.client.objecteditor.types.MethodDefinition;

import org.fcrepo.server.types.mtom.gen.ComparisonOperator;
import org.fcrepo.server.types.mtom.gen.Condition;
import org.fcrepo.server.types.mtom.gen.FieldSearchQuery;
import org.fcrepo.server.types.mtom.gen.FieldSearchResult;
import org.fcrepo.server.types.mtom.gen.FieldSearchResult.ResultList;
import org.fcrepo.server.types.mtom.gen.ObjectFactory;
import org.fcrepo.server.types.mtom.gen.ObjectFields;
import org.fcrepo.server.utilities.TypeUtility;

/**
 * Some static utility methods that might be needed across several classes in
 * this package.
 */
public abstract class Util {

    public static Map getSDefLabelMap() throws IOException {
        try {
            HashMap labelMap = new HashMap();
            FieldSearchQuery query = new FieldSearchQuery();
            Condition[] conditions = new Condition[1];
            conditions[0] = new Condition();
            conditions[0].setProperty("fType");
            conditions[0].setOperator(ComparisonOperator.fromValue("eq"));
            conditions[0].setValue("D");
            FieldSearchQuery.Conditions conds =
                    new FieldSearchQuery.Conditions();
            conds.getCondition().addAll(Arrays.asList(conditions));
            ObjectFactory factory = new ObjectFactory();
            query.setConditions(factory.createFieldSearchQueryConditions(conds));
            String[] fields = new String[] {"pid", "label"};
            if (true) {
                /* FIXME: find some other way to do this */
                throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
            }
            FieldSearchResult result =
                    Administrator.APIA.findObjects(TypeUtility.convertStringtoAOS(fields),
                                                   new BigInteger("50"),
                                                   query);
            while (result != null) {
                ResultList resultList = result.getResultList();
                if (resultList != null && resultList.getObjectFields() != null) {
                    for (ObjectFields element : resultList.getObjectFields()) {
                        labelMap.put(element.getPid(), element.getLabel());
                    }
                }
                if (result.getListSession() != null) {
                    result =
                            Administrator.APIA.resumeFindObjects(result
                                    .getListSession().getValue().getToken());
                } else {
                    result = null;
                }
            }
            return labelMap;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Get a map of pid-to-label of service deployments that implement the
     * service defined by the indicated sDef.
     */
    public static Map getDeploymentLabelMap(String sDefPID) throws IOException {
        try {
            HashMap labelMap = new HashMap();
            FieldSearchQuery query = new FieldSearchQuery();
            Condition[] conditions = new Condition[2];
            conditions[0] = new Condition();
            conditions[0].setProperty("fType");
            conditions[0].setOperator(ComparisonOperator.fromValue("eq"));
            conditions[0].setValue("M");
            conditions[1] = new Condition();
            conditions[1].setProperty("bDef");
            conditions[1].setOperator(ComparisonOperator.fromValue("has"));
            conditions[1].setValue(sDefPID);
            FieldSearchQuery.Conditions conds =
                    new FieldSearchQuery.Conditions();
            conds.getCondition().addAll(Arrays.asList(conditions));
            ObjectFactory factory = new ObjectFactory();
            query.setConditions(factory.createFieldSearchQueryConditions(conds));
            String[] fields = new String[] {"pid", "label"};
            if (true) {
                /*
                 * FIXME: find some other way to do this, if we care. it uses
                 * fType and bDef, which are no longer in field search,
                 */
                throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
            }
            FieldSearchResult result =
                    Administrator.APIA
                            .findObjects(TypeUtility.convertStringtoAOS(fields),
                                         new BigInteger("50"),
                                         query);
            while (result != null) {
                ResultList resultList = result.getResultList();
                if (resultList != null && resultList.getObjectFields() != null) {
                    for (ObjectFields element : resultList.getObjectFields()) {
                        labelMap.put(element.getPid(), element.getLabel());
                    }
                }
                if (result.getListSession() != null) {
                    result =
                            Administrator.APIA.resumeFindObjects(result
                                    .getListSession().getValue().getToken());
                } else {
                    result = null;
                }
            }
            return labelMap;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public static Map getInputSpecMap(Set deploymentPIDs) throws IOException {
        HashMap specMap = new HashMap();
        Iterator iter = deploymentPIDs.iterator();
        while (iter.hasNext()) {
            String pid = (String) iter.next();
            specMap.put(pid, getInputSpec(pid));
        }
        return specMap;
    }

    public static DatastreamInputSpec getInputSpec(String deploymentPID)
            throws IOException {
        HashMap hash = new HashMap();
        hash.put("itemID", "DSINPUTSPEC");
        /*
         * return DatastreamInputSpec.parse(
         * Administrator.DOWNLOADER.getDissemination( deploymentPID,
         * "fedora-system:3", "getItem", hash, null) );
         */
        return DatastreamInputSpec
                .parse(Administrator.DOWNLOADER
                        .getDatastreamDissemination(deploymentPID,
                                                    "DSINPUTSPEC",
                                                    null));

    }

    /**
     * Get the list of MethodDefinition objects defined by the indicated service
     * definition.
     */
    public static java.util.List getMethodDefinitions(String sDefPID)
            throws IOException {
        HashMap parms = new HashMap();
        parms.put("itemID", "METHODMAP");
        return MethodDefinition.parse(Administrator.DOWNLOADER
                .getDatastreamDissemination(sDefPID, "METHODMAP", null));
    }

    /**
     * Get the indicated fields of the indicated object from the repository.
     */
    public static ObjectFields getObjectFields(String pid, String[] fields)
            throws IOException {
        FieldSearchQuery query = new FieldSearchQuery();
        Condition condition = new Condition();
        condition.setProperty("pid");
        condition.setOperator(ComparisonOperator.fromValue("eq"));
        condition.setValue(pid);
        FieldSearchQuery.Conditions conds = new FieldSearchQuery.Conditions();
        conds.getCondition().add(condition);
        ObjectFactory factory = new ObjectFactory();
        query.setConditions(factory.createFieldSearchQueryConditions(conds));
        FieldSearchResult result =
                Administrator.APIA
                        .findObjects(TypeUtility.convertStringtoAOS(fields),
                                     new BigInteger("1"),
                                     query);
        ResultList resultList = result.getResultList();
        if (resultList == null || resultList.getObjectFields() == null
                && resultList.getObjectFields().size() == 0) {
            throw new IOException("Object not found in repository");
        }
        return resultList.getObjectFields().get(0);
    }

    /**
     * Layout the provided components in two columns, each left-aligned, where
     * the left column's cells are as narrow as possible. If north is true, all
     * cells will be laid out to the NORTHwest. This is useful when some rows'
     * cells aren't the same size vertically. If allowStretching is true,
     * components on the right will be stretched if they can be.
     */
    public static void addRows(JComponent[] left,
                               JComponent[] right,
                               GridBagLayout gridBag,
                               Container container,
                               boolean north,
                               boolean allowStretching) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 4, 4, 4);
        if (north) {
            c.anchor = GridBagConstraints.NORTHWEST;
        } else {
            c.anchor = GridBagConstraints.WEST;
        }
        for (int i = 0; i < left.length; i++) {
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE; //reset to default
            c.weightx = 0.0; //reset to default
            gridBag.setConstraints(left[i], c);
            container.add(left[i]);

            c.gridwidth = GridBagConstraints.REMAINDER; //end row
            if (right[i] instanceof JComboBox) {
                if (allowStretching) {
                    c.fill = GridBagConstraints.HORIZONTAL;
                }
            } else {
                c.fill = GridBagConstraints.HORIZONTAL;
            }
            c.weightx = 1.0;
            gridBag.setConstraints(right[i], c);
            container.add(right[i]);
        }

    }

}