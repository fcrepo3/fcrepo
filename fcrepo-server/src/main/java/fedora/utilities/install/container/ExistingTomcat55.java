/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.File;

import fedora.utilities.install.Distribution;
import fedora.utilities.install.InstallOptions;

public class ExistingTomcat55
        extends ExistingTomcat {

	private File commonLib;
	
	public ExistingTomcat55(Distribution dist, InstallOptions options) {
        super(dist, options);
    }

	@Override
	protected File getCommonLib() {
		return commonLib;
	}

	@Override
	protected void setCommonLib() {
		commonLib = new File(getTomcatHome(), 
				"common" + File.separator + "lib" + File.separator);
	}
}
