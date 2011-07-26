/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.install.container;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;

import org.fcrepo.utilities.FileUtils;
import org.fcrepo.utilities.install.Distribution;
import org.fcrepo.utilities.install.InstallOptions;
import org.fcrepo.utilities.install.InstallationFailedException;


public abstract class Tomcat
        extends Container {

    public static final String CONF = "conf";

    public static final String KEYSTORE = "keystore";

    private final File tomcatHome;

    private final File webapps;

    private final File conf;

    /**
     * Target location of the included keystore file.
     */
    private final File includedKeystore;

    Tomcat(Distribution dist, InstallOptions options) {
        super(dist, options);
        tomcatHome =
                new File(getOptions().getValue(InstallOptions.TOMCAT_HOME));
        webapps = new File(tomcatHome, "webapps" + File.separator);
        conf = new File(tomcatHome, CONF + File.separator);
        setCommonLib();
        includedKeystore = new File(conf, KEYSTORE);
    }

    @Override
    public void deploy(File war) throws InstallationFailedException {
        System.out.println("Deploying " + war.getName() + "...");
        File dest = new File(webapps, war.getName());
        if (!FileUtils.copy(war, dest)) {
            throw new InstallationFailedException("Deploy failed: unable to copy "
                    + war.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
    }

    @Override
    public void install() throws InstallationFailedException {
        installTomcat();
        installServerXML();
        installFedoraContext();
        installIncludedKeystore();
    }

    /**
     * Creates a Tomcat context for Fedora in
     * $CATALINA_HOME/conf/Catalina/localhost which sets the fedora.home system
     * property to the installer-provided value.
     */
    protected void installFedoraContext() throws InstallationFailedException {
        File contextDir =
            new File(getConf().getPath() + File.separator + "Catalina"
                    + File.separator + "localhost");
        contextDir.mkdirs();

        try {
            File fhome = new File(getOptions()
                                             .getValue(InstallOptions.FEDORA_HOME));
            String content =
                    IOUtils.toString(this.getClass()
                            .getResourceAsStream("/resources/context.xml"))
                            .replace("_FEDORA_HOME_",
                                     getOptions().getValue(InstallOptions.FEDORA_HOME));

            String name =
                    getOptions()
                            .getValue(InstallOptions.FEDORA_APP_SERVER_CONTEXT)
                            + ".xml";

            FileOutputStream out =
                    new FileOutputStream(new File(contextDir, name));

            out.write(content.getBytes());
            out.close();
        } catch (Exception e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }

    }

    protected abstract void installTomcat() throws InstallationFailedException;

    protected abstract void installServerXML()
            throws InstallationFailedException;

    protected abstract void installIncludedKeystore()
            throws InstallationFailedException;

    protected abstract void setCommonLib();

    protected abstract File getCommonLib();

    protected final File getTomcatHome() {
        return tomcatHome;
    }

    protected final File getWebapps() {
        return webapps;
    }

    protected final File getConf() {
        return conf;
    }

    protected final File getIncludedKeystore() {
        return includedKeystore;
    }


}
