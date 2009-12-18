/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import fedora.utilities.install.Distribution;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;
import fedora.utilities.install.Installer;

public class ExistingTomcat50
        extends ExistingTomcat55 {

    public ExistingTomcat50(Distribution dist, InstallOptions options) {
        super(dist, options);
    }

    @Override
    public void install() throws InstallationFailedException {
        super.install();
        Installer.installJDBCDriver(getDist(), getOptions(), getCommonLib());
    }
}
