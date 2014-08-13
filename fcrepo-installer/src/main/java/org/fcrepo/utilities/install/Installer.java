/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utilities.install;

import org.apache.commons.io.IOUtils;
import org.fcrepo.utilities.FileUtils;
import org.fcrepo.utilities.LogConfig;
import org.fcrepo.utilities.Zip;
import org.fcrepo.utilities.install.container.Container;
import org.fcrepo.utilities.install.container.ContainerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


public class Installer {

    private final Distribution _dist;

    private final InstallOptions _opts;

    private final File fedoraHome;

    private final File installDir;

    public Installer(Distribution dist, InstallOptions opts) {
        _dist = dist;
        _opts = opts;
        fedoraHome = new File(_opts.getValue(InstallOptions.FEDORA_HOME));
        installDir = new File(fedoraHome, "install" + File.separator);
    }

    /**
     * Install the distribution based on the options.
     */
    public void install() throws InstallationFailedException {
        installDir.mkdirs();

        // Write out the install options used to a properties file in the install directory
        try {
            OutputStream out =
                    new FileOutputStream(new File(installDir,
                                                  "install.properties"));
            _opts.dump(out);
            out.close();
        } catch (Exception e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
        new FedoraHome(_dist, _opts).install();

        if (!_opts.getValue(InstallOptions.INSTALL_TYPE)
                .equals(InstallOptions.INSTALL_CLIENT)) {
            Container container = ContainerFactory.getContainer(_dist, _opts);
            container.install();
            container.deploy(buildWAR());
            if (_opts.getBooleanValue(InstallOptions.DEPLOY_LOCAL_SERVICES,
                                      true)) {
                deployLocalService(container, Distribution.FOP_WAR);
                deployLocalService(container, Distribution.IMAGEMANIP_WAR);
                deployLocalService(container, Distribution.SAXON_WAR);
                deployLocalService(container, Distribution.DEMO_WAR);
            }

            Database database = new Database(_dist, _opts);
            database.install();
        }

        System.out.println("Installation complete.");
        if (!_opts.getValue(InstallOptions.INSTALL_TYPE)
                .equals(InstallOptions.INSTALL_CLIENT)
                && _opts.getValue(InstallOptions.SERVLET_ENGINE)
                        .equals(InstallOptions.OTHER)) {
            System.out
                    .println("\n"
                            + "----------------------------------------------------------------------\n"
                            + "The Fedora Installer cannot automatically deploy the Web ARchives to  \n"
                            + "the selected servlet container. You must deploy the WAR files         \n"
                            + "manually. You can find fedora.war plus several sample back-end        \n"
                            + "services and a demonstration object package in:                       \n"
                            + "\t" + fedoraHome.getAbsolutePath()
                            + File.separator + "install");
        }
        System.out
                .println("\n"
                        + "----------------------------------------------------------------------\n"
                        + "Before starting Fedora, please ensure that any required environment\n"
                        + "variables are correctly defined\n"
                        + "\t(e.g. FEDORA_HOME, JAVA_HOME, JAVA_OPTS, CATALINA_HOME).\n"
                        + "For more information, please consult the Installation & Configuration\n"
                        + "Guide in the online documentation.\n"
                        + "----------------------------------------------------------------------\n");
    }

    private File buildWAR() throws InstallationFailedException {
        String fedoraWarName = _opts.getValue(InstallOptions.FEDORA_APP_SERVER_CONTEXT) ;
        System.out.println("Preparing " + fedoraWarName + ".war...");
        try {
            File outputFile = new File(installDir, fedoraWarName + ".war");
            String driver = _opts.getValue(InstallOptions.DATABASE_DRIVER);
            File stagingDir = stage(_dist.get(Distribution.FEDORA_WAR));
            if (!driver.equals(InstallOptions.INCLUDED)) {
                addLibrary(stagingDir, driver, outputFile);
            }
            addContext(stagingDir);
            repackage(stagingDir, outputFile);
            return outputFile;
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
		}
    }

    private File stage(InputStream inputStream) throws IOException {
        File stagingDir = new File(installDir, "fedorawar");
        stagingDir.mkdirs();
        Zip.unzip(inputStream, stagingDir);
        return stagingDir;
    }
    private File repackage(File stagingDir, File outputFile) throws IOException {
        Zip.zip(outputFile, stagingDir.listFiles());
        FileUtils.delete(stagingDir);
        return outputFile;
    }
    private void addLibrary(File stagingDir, String libraryPath,
            File outputFile) throws IOException {
        // unzip, add file, re-zip, and remove staging dir
        File sourceFile = new File(libraryPath);
        File destFile = new File(stagingDir, "WEB-INF/lib/" + sourceFile.getName());
        FileUtils.copy(sourceFile, destFile);
    }
    private void addContext(File stagingDir) throws IOException {
        File metaInf = new File(stagingDir, "META-INF");
        metaInf.mkdirs();
        File contextFile = new File(metaInf, "context.xml");
        String content =
                IOUtils.toString(this.getClass()
                        .getResourceAsStream("/resources/context.xml"))
                        .replace("_FEDORA_HOME_",
                                 _opts.getValue(InstallOptions.FEDORA_HOME));
        FileWriter writer = new FileWriter(contextFile);
        IOUtils.write(content, writer);
        writer.close();
    }
    private void deployLocalService(Container container, String filename)
            throws InstallationFailedException {
        try {
            File war = new File(installDir, filename);
            FileOutputStream out = new FileOutputStream(war);
            if (!FileUtils.copy(_dist.get(filename), out)) {
                throw new InstallationFailedException("Copy to "
                        + war.getAbsolutePath() + " failed.");
            }
            out.close();
            container.deploy(war);
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    /**
     * Command-line entry point.
     */
    public static void main(String[] args) {
        LogConfig.initMinimal();
        try {
            Distribution dist = new ClassLoaderDistribution();
            InstallOptions opts = null;

            if (args.length == 0) {
                opts = new InstallOptions(dist);
            } else {
                Map<String, String> props = new HashMap<String, String>();
                for (String file : args) {
                    props.putAll(FileUtils.loadMap(new File(file)));
                }
                opts = new InstallOptions(dist, props);
            }

            // set fedora.home
            System.setProperty("fedora.home", opts
                    .getValue(InstallOptions.FEDORA_HOME));
            new Installer(dist, opts).install();

        } catch (Exception e) {
            printException(e);
            System.exit(1);
        }
    }

    /**
     * Print a message appropriate for the given exception in as human-readable
     * way as possible.
     */
    private static void printException(Exception e) {

        if (e instanceof InstallationCancelledException) {
            System.out.println("Installation cancelled.");
            return;
        }

        boolean recognized = false;
        String msg = "ERROR: ";
        if (e instanceof InstallationFailedException) {
            msg += "Installation failed: " + e.getMessage();
            recognized = true;
        } else if (e instanceof OptionValidationException) {
            OptionValidationException ove = (OptionValidationException) e;
            msg +=
                    "Bad value for '" + ove.getOptionId() + "': "
                            + e.getMessage();
            recognized = true;
        }

        if (recognized) {
            System.err.println(msg);
            if (e.getCause() != null) {
                System.err.println("Caused by: ");
                e.getCause().printStackTrace(System.err);
            }
        } else {
            System.err.println(msg + "Unexpected error; installation aborted.");
            e.printStackTrace();
        }
    }
}
