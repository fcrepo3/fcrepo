package org.fcrepo.server.security.xacml.test;

import java.io.InputStream;

import org.fcrepo.server.security.xacml.pdp.finder.policy.RightsMetadataDocument;
import org.junit.Test;


public class RightsMetadataDocumentTest {

    @Test
    public void test() throws Exception {

        InputStream in =
                this.getClass().getClassLoader().getResourceAsStream(
                        "rightsMetadata.xml");
        RightsMetadataDocument rmd = new RightsMetadataDocument(in);
        rmd.getActionSubjectMap();
    }

}
