package org.fcrepo.test.api;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.fcrepo.test.DemoObjectTestSetup;
import org.fcrepo.test.FedoraServerTestCase;
import org.junit.After;
import org.xml.sax.InputSource;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import com.yourmediashelf.fedora.client.response.GetObjectHistoryResponse;

public class TestExampleWithMediashelfClient extends FedoraServerTestCase {

	private FedoraClient client;
	
    public static Test suite() {
        TestSuite suite = new TestSuite("Test Example With Mediashelf Client");
        suite.addTestSuite(TestExampleWithMediashelfClient.class);
        return new DemoObjectTestSetup(suite);
    }

	@Override
	public void setUp() throws Exception {
		client = new FedoraClient(new FedoraCredentials(getBaseURL(),
				getUsername(), getPassword()));
		Map<String, String> nsMap = new HashMap<String, String>();
		nsMap.put(ACCESS.prefix, ACCESS.uri);
		NamespaceContext ctx = new SimpleNamespaceContext(nsMap);
		XMLUnit.setXpathNamespaceContext(ctx);
	}

	public void testObjectHistory() throws Exception {
		GetObjectHistoryResponse response = FedoraClient.getObjectHistory(
				"demo:5").execute(client);
		assertXpathExists(
				"/access:fedoraObjectHistory/access:objectChangeDate",
				new InputSource(response.getEntityInputStream()));
	}

	@Override
	@After
	public void tearDown() {
		XMLUnit.setXpathNamespaceContext(SimpleNamespaceContext.EMPTY_CONTEXT);
	}
	
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestAPIA.class);
    }
}
