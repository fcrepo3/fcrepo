
package org.fcrepo.test.integration;

import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.fcrepo.server.access.FedoraAPIA;
import org.fcrepo.server.management.FedoraAPIM;
import org.fcrepo.server.types.gen.Datastream;
import org.fcrepo.server.types.gen.DatastreamDef;
import org.fcrepo.server.types.gen.ObjectProfile;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.Assert;
import org.junit.Test;

public class TestObjectLastModDate
        extends FedoraServerTestCase {

    private FedoraAPIA apia;

    private FedoraAPIM apim;

    private static final String FOXMLPATH = "fcrepo238.xml";

    private final DateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final String RESOURCEBASE =
            System.getProperty("fcrepo-integrationtest-core.classes") != null ? System
                    .getProperty("fcrepo-integrationtest-core.classes")
                    + "test-objects/foxml/cli-utils"
                    : "src/test/resources/test-objects/foxml/cli-utils";

    @Override
    protected void setUp() throws Exception {
        apia = getFedoraClient().getAPIA();
        apim = getFedoraClient().getAPIM();
        super.setUp();
    }

    @Test
    public void testFCREPO238() throws Exception {
        String pid =
                apim.ingest(IOUtils.toByteArray(new FileInputStream(RESOURCEBASE
                                    + "/" + FOXMLPATH)),
                            FOXML1_1.uri,
                            "testing fcrepo 238");
        ObjectProfile profile = apia.getObjectProfile(pid, null);
        Date objDate = dateFormat.parse(profile.getObjLastModDate());
        for (DatastreamDef dd : apia.listDatastreams(pid, null)) {
            Datastream ds = apim.getDatastream(pid, dd.getID(), null);
            Date dsDate = dateFormat.parse(ds.getCreateDate());
            System.out.print("object:" + dateFormat.format(objDate) + ", ");
            System.out.println("datastream: " + dateFormat.format(dsDate));
            Assert.assertTrue("object last modificaton date is before datastream's create date. check FCREPO-238",
                              objDate.compareTo(dsDate) > -1);
        }
        apim.purgeObject(pid, "removing testobject", true);
    }
}
