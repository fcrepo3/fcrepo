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
import fedora.utilities.Zip;
import fedora.utilities.install.Distribution;
import fedora.utilities.install.FedoraHome;
import fedora.utilities.install.InstallOptions;
import fedora.utilities.install.InstallationFailedException;

/**
 * The profile for the servlet container bundled with the Fedora installer.
 * History: 
 *  Release 3.3 bundled Tomcat 6.0.20.
 * 	Release 3.0 bundled Tomcat 5.5.26. 
 * 	Release 2.2 bundled Tomcat 5.0.28.
 * 
 * @author Edwin Shin
 * @version $Id$
 */
public class BundledTomcat
        extends Tomcat {

	private File commonLib;
	
    public BundledTomcat(Distribution dist, InstallOptions options) {
        super(dist, options);
    }

    @Override
    public void install() throws InstallationFailedException {
        super.install();
    }

    @Override
    protected void installTomcat() throws InstallationFailedException {
        System.out.println("Installing Tomcat...");
        try {
            Zip.unzip(getDist().get(Distribution.TOMCAT), System
                    .getProperty("java.io.tmpdir"));
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
        File f =
                new File(System.getProperty("java.io.tmpdir"),
                         Distribution.TOMCAT_BASENAME);
        if (!FileUtils.move(f, getTomcatHome())) {
            throw new InstallationFailedException("Move to "
                    + getTomcatHome().getAbsolutePath() + " failed.");
        }
        FedoraHome.setScriptsExecutable(new File(getTomcatHome(), "bin"));
    }

    @Override
    protected void installServerXML() throws InstallationFailedException {
        try {
            File distServerXML = new File(getConf(), "server.xml");
            TomcatServerXML serverXML =
                    new TomcatServerXML(distServerXML, getOptions());
            serverXML.update();
            serverXML.write(distServerXML.getAbsolutePath());
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

	@Override
	protected void setCommonLib() {
		commonLib = new File(getTomcatHome(), "lib" + File.separator);
	}
    
}
