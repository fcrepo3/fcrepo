/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.install.container;

import org.fcrepo.utilities.install.Distribution;
import org.fcrepo.utilities.install.InstallOptions;
import org.fcrepo.utilities.install.InstallationFailedException;
import org.fcrepo.utilities.install.Installer;

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
