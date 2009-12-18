/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.config.webxml;

import java.util.ArrayList;
import java.util.List;

public class WelcomeFileList {

    private final List<String> welcomeFiles;

    public WelcomeFileList() {
        welcomeFiles = new ArrayList<String>();
    }

    public List<String> getWelcomeFiles() {
        return welcomeFiles;
    }

    public void addWelcomeFile(String welcomeFile) {
        welcomeFiles.add(welcomeFile);
    }
}
