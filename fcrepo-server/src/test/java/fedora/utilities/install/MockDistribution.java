package fedora.utilities.install;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A mock Distribution for testing.
 * 
 * @author Edwin Shin
 *
 */
public class MockDistribution extends Distribution {

	@Override
	public boolean contains(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InputStream get(String path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL(String path) {
		// TODO Auto-generated method stub
		return null;
	}

}
