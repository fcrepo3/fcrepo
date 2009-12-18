/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

/**
 * 
 */
package fedora.utilities.install.container;

import java.io.File;

import fedora.utilities.install.Distribution;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;

/**
 * Abstract class representing a servlet container.
 * 
 * @author Edwin Shin
 */
public abstract class Container {

    private final Distribution dist;

    private final InstallOptions options;

    /**
     * 
     * @param dist
     * @param options
     */
    public Container(Distribution dist, InstallOptions options) {
        this.dist = dist;
        this.options = options;
    }

    public abstract void deploy(File war) throws InstallationFailedException;

    public abstract void install() throws InstallationFailedException;

    protected final Distribution getDist() {
        return dist;
    }

    protected final InstallOptions getOptions() {
        return options;
    }
}
