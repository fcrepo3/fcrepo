package org.fcrepo.test.fesl.policy;

import static com.yourmediashelf.fedora.client.FedoraClient.getDatastream;
import static com.yourmediashelf.fedora.client.FedoraClient.ingest;
import static com.yourmediashelf.fedora.client.FedoraClient.modifyDatastream;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.fcrepo.test.FedoraServerTestCase;
import org.fcrepo.test.FedoraTestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.fcrepo.server.access.FedoraAPIAMTOM;
import org.fcrepo.server.management.FedoraAPIMMTOM;
import org.fcrepo.server.utilities.TypeUtility;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;

public class TestRightsMetadataPolicy extends FedoraServerTestCase {

    private FedoraClient client;

    private FedoraClient researcher1;

    private FedoraClient archivist1;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        client =
                new FedoraClient(new FedoraCredentials(getBaseURL(),
                        getUsername(), getPassword()));

        researcher1 =
                new FedoraClient(new FedoraCredentials(getBaseURL(),
                        "researcher1", getPassword()));

        archivist1 =
                new FedoraClient(new FedoraCredentials(getBaseURL(),
                        "archivist1", getPassword()));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {
        InputStream in =
                this.getClass().getClassLoader().getResourceAsStream(
                        "rightsMetadata_1.foxml.xml");
        java.io.File file = new java.io.File("/Users/benjamin/github/fcrepo/fcrepo-security/fcrepo-security-pdp/target/test-classes/rightsMetadata_1.foxml.xml");
        in = new java.io.FileInputStream(file);
        org.fcrepo.client.FedoraClient client = FedoraTestCase.getFedoraClient();
        FedoraAPIMMTOM apim = client.getAPIMMTOM();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte [] buf = new byte[1024];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            bos.write(buf,0,len);
        }
        String pid = apim.ingest(TypeUtility.convertBytesToDataHandler(bos.toByteArray()), FOXML1_1.uri, "rights metadata example");
//        String pid = ingest().content(in).execute(client).getPid();
        assertEquals(pid, "test:rightsMetadata1");

        assertEquals(200, getDatastream(pid, "DC").execute(researcher1)
                .getStatus());

        assertEquals(401, modifyDatastream(pid, "DC")
                .dsLabel("This request should not work")
                .logMessage(
                        "modifyDatastream request as researcher1 that should fail")
                .execute(researcher1));

        assertEquals(201, modifyDatastream(pid, "DC").dsLabel(
                "This request should work").logMessage(
                "modifyDatastream request as archivist1 that should succeed")
                .execute(archivist1));

    }

}
