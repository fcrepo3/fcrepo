package org.fcrepo.test.api;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.util.HashMap;
import java.util.Map;

import junit.framework.JUnit4TestAdapter;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.xml.sax.InputSource;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import com.yourmediashelf.fedora.client.response.GetObjectHistoryResponse;

public class TestExampleWithMediashelfClient extends FedoraServerTestCase {

    private static org.fcrepo.client.FedoraClient s_client;
    
	private FedoraClient client;
	
	@BeforeClass
	public static void bootstrap() throws Exception {
	    s_client = getFedoraClient();
	    //TODO what directory is demo:5 in?
	    ingestSimpleDocumentDemoObjects( s_client);
	}
	
	@AfterClass
	public static void cleanUp() throws Exception {
	    purgeDemoObjects(s_client);
	    s_client.shutdown();
	}
	
	@Before
	public void setUp() throws Exception {
        client = new FedoraClient(new FedoraCredentials(getBaseURL(),
                getUsername(), getPassword()));
		Map<String, String> nsMap = new HashMap<String, String>();
		nsMap.put(ACCESS.prefix, ACCESS.uri);
		NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
		XMLUnit.setXpathNamespaceContext(ctx);
	}

	@Test
	public void testObjectHistory() throws Exception {
		GetObjectHistoryResponse response = FedoraClient.getObjectHistory(
				"demo:18").execute(client);
		assertXpathExists(
				"/access:fedoraObjectHistory/access:objectChangeDate",
				new InputSource(response.getEntityInputStream()));
	}

	@After
	public void tearDown() {
		XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
	}
	
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(TestExampleWithMediashelfClient.class);
    }

    public static void main(String[] args) {
        JUnitCore.runClasses(TestExampleWithMediashelfClient.class);
    }
}
