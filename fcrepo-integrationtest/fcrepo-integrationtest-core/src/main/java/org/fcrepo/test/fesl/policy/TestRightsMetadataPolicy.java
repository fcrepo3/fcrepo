package org.fcrepo.test.fesl.policy;

import static com.yourmediashelf.fedora.client.FedoraClient.getDatastream;
import static com.yourmediashelf.fedora.client.FedoraClient.ingest;
import static com.yourmediashelf.fedora.client.FedoraClient.modifyDatastream;

import java.io.InputStream;

import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        String pid = ingest().content(in).execute(client).getPid();
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
