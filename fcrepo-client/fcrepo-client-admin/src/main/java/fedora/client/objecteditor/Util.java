/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.client.objecteditor;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.apache.axis.types.NonNegativeInteger;

import fedora.client.Administrator;
import fedora.client.objecteditor.types.DatastreamInputSpec;
import fedora.client.objecteditor.types.MethodDefinition;

import fedora.server.types.gen.ComparisonOperator;
import fedora.server.types.gen.Condition;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ObjectFields;

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
            query.setConditions(conditions);
            String[] fields = new String[] {"pid", "label"};
            if (true) {
                /* FIXME: find some other way to do this */
                throw new UnsupportedOperationException("This operation uses obsolete field search semantics");
            }
            FieldSearchResult result =
                    Administrator.APIA
                            .findObjects(fields,
                                         new NonNegativeInteger("50"),
                                         query);
            while (result != null) {
                ObjectFields[] resultList = result.getResultList();
                for (ObjectFields element : resultList) {
                    labelMap.put(element.getPid(), element.getLabel());
                }
                if (result.getListSession() != null) {
                    result =
                            Administrator.APIA.resumeFindObjects(result
                                    .getListSession().getToken());
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
            query.setConditions(conditions);
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
                            .findObjects(fields,
                                         new NonNegativeInteger("50"),
                                         query);
            while (result != null) {
                ObjectFields[] resultList = result.getResultList();
                for (ObjectFields element : resultList) {
                    labelMap.put(element.getPid(), element.getLabel());
                }
                if (result.getListSession() != null) {
                    result =
                            Administrator.APIA.resumeFindObjects(result
                                    .getListSession().getToken());
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
        Condition[] conditions = new Condition[1];
        conditions[0] = new Condition();
        conditions[0].setProperty("pid");
        conditions[0].setOperator(ComparisonOperator.fromValue("eq"));
        conditions[0].setValue(pid);
        query.setConditions(conditions);
        FieldSearchResult result =
                Administrator.APIA.findObjects(fields,
                                               new NonNegativeInteger("1"),
                                               query);
        ObjectFields[] resultList = result.getResultList();
        if (resultList == null || resultList.length == 0) {
            throw new IOException("Object not found in repository");
        }
        return resultList[0];
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