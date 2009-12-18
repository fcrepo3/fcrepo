/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install.container;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.DocumentException;

import fedora.utilities.FileUtils;
import fedora.utilities.install.Distribution;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;

public class ExistingTomcat
        extends Tomcat {

    private final File installDir;
    private File commonLib;

    public ExistingTomcat(Distribution dist, InstallOptions options) {
        super(dist, options);
        installDir =
                new File(getOptions().getValue(InstallOptions.FEDORA_HOME)
                        + File.separator + "install" + File.separator);
    }

    @Override
    protected void installTomcat() throws InstallationFailedException {
        // nothing to do
    }

    @Override
    protected void installServerXML() throws InstallationFailedException {
        try {
            File distServerXML = new File(getConf(), "server.xml");
            TomcatServerXML serverXML =
                    new TomcatServerXML(distServerXML, getOptions());
            serverXML.update();

            File example = new File(installDir, "server.xml");
            serverXML.write(example.getAbsolutePath());
            System.out.println("Will not overwrite existing "
                    + distServerXML.getAbsolutePath() + ".\n"
                    + "Wrote example server.xml to: \n\t"
                    + example.getAbsolutePath());
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        } catch (DocumentException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    @Override
    protected void installIncludedKeystore() throws InstallationFailedException {
        String keystoreFile =
                getOptions().getValue(InstallOptions.KEYSTORE_FILE);
        if (keystoreFile == null
                || !keystoreFile.equals(InstallOptions.INCLUDED)) {
            // nothing to do
            return;
        }
        try {
            InputStream is = getDist().get(Distribution.KEYSTORE);

            File keystore = getIncludedKeystore();
            if (keystore.exists()) {
                System.out
                        .println("WARNING: A keystore file already exists at: "
                                + keystore.getAbsolutePath() + ".");
                keystore = new File(installDir, Distribution.KEYSTORE);
                System.out
                        .println("WARNING: The existing keystore will not be overwritten.");
                System.out
                        .println("WARNING: The installer-provided keystore will not be installed, it will be copied to: \n\t"
                                + keystore.getAbsolutePath());
            }
            if (!FileUtils.copy(is, new FileOutputStream(keystore))) {
                throw new InstallationFailedException("Copy to "
                        + keystore.getAbsolutePath() + " failed.");
            }
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

	@Override
	protected File getCommonLib() {
		return commonLib;
	}


	/**
	 * Tomcat 6 merged the common, shared and server lib directories of 
	 * Tomcat 5.x into a single lib directory. Unless otherwise configured in
	 * conf/catalina.properties, the lib directory is located at 
	 * $CATALINA_HOME/lib
	 */
	@Override
	protected void setCommonLib() {
		new File(getTomcatHome(), "lib" + File.separator);
	}
}
