package fedora.utilities.install;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fedora.utilities.FileUtils;

public class OptionDefinitionTest {

	@Test
	public void testGet() throws Exception {
		Map<String, String> oMap = new HashMap<String, String>();
		oMap.put(InstallOptions.APIA_AUTH_REQUIRED, Boolean.toString(false));
		InstallOptions installOpts = new InstallOptions(new MockDistribution(),
				oMap);
		OptionDefinition opt = OptionDefinition.get(
				InstallOptions.APIA_AUTH_REQUIRED, installOpts);
		assertNotNull(opt);
	}

	/*
	 * @Test public void testGetId() { fail("Not yet implemented"); }
	 * 
	 * @Test public void testGetLabel() { fail("Not yet implemented"); }
	 * 
	 * @Test public void testGetDescription() { fail("Not yet implemented"); }
	 * 
	 * @Test public void testGetValidValues() { fail("Not yet implemented"); }
	 * 
	 * @Test public void testGetDefaultValue() { fail("Not yet implemented");
	 * 
	 * //should test FEDORA_HOME, TOMCAT_HOME, FESL_DBXML_HOME in particular }
	 * 
	 * @Test public void testValidateValueString() {
	 * fail("Not yet implemented"); }
	 * 
	 * @Test public void testValidateValueStringBoolean() {
	 * fail("Not yet implemented"); }
	 */
}
