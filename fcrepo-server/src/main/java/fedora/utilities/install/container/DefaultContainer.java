/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.File;

import fedora.utilities.install.Distribution;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;

public class DefaultContainer
        extends Container {

    public DefaultContainer(Distribution dist, InstallOptions options) {
        super(dist, options);
    }

    @Override
    public void deploy(File war) throws InstallationFailedException {
        System.out.println("WARNING: Unable to deploy to this container.");
        System.out.println(war.getAbsolutePath()
                + " must be manually deployed.");
    }

    @Override
    public void install() {
        //Nothing to install for this container.
    }
}
