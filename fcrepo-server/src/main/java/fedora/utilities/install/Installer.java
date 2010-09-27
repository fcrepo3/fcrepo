/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities.install;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import fedora.utilities.FileUtils;
import fedora.utilities.Zip;
import fedora.utilities.install.container.Container;
import fedora.utilities.install.container.ContainerFactory;
import fedora.utilities.install.container.FedoraWebXML;

public class Installer {

    static {
        //send all log4j (WARN only) output to STDOUT
        Properties props = new Properties();
        props.setProperty("log4j.appender.STDOUT",
                          "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.STDOUT.layout",
                          "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.STDOUT.layout.ConversionPattern",
                          "%p (%c{1}) %m%n");
        props.setProperty("log4j.rootLogger", "WARN, STDOUT");
        PropertyConfigurator.configure(props);

        //tell commons-logging to use log4j
        final String pfx = "org.apache.commons.logging.";
        if (System.getProperty(pfx + "LogFactory") == null) {
            System.setProperty(pfx + "LogFactory", pfx + "impl.Log4jFactory");
            System.setProperty(pfx + "Log", pfx + "impl.Log4JLogger");
        }
    }

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
        // build a staging area in FEDORA_HOME
        try {
            File warStage = new File(installDir, "fedorawar" + File.separator);
            warStage.mkdirs();
            Zip.unzip(_dist.get(Distribution.FEDORA_WAR), warStage);

            // modify web.xml
            System.out.println("Processing web.xml");
            File distWebXML = new File(warStage, "WEB-INF/web.xml");
            FedoraWebXML webXML =
                    new FedoraWebXML(distWebXML.getAbsolutePath(), _opts);
            Writer outputWriter =
                    new BufferedWriter(new FileWriter(distWebXML));
            webXML.write(outputWriter);
            outputWriter.close();

            // Remove commons-collections, commons-dbcp, and commons-pool
            // from fedora.war if using Tomcat 5.0
            String container = _opts.getValue(InstallOptions.SERVLET_ENGINE);
            File webinfLib = new File(warStage, "WEB-INF/lib/");
            if (container.equals(InstallOptions.INCLUDED)
                    || container.equals(InstallOptions.EXISTING_TOMCAT)) {
                File tomcatHome =
                        new File(_opts.getValue(InstallOptions.TOMCAT_HOME));
                File dbcp55 =
                        new File(tomcatHome,
                                 "common/lib/naming-factory-dbcp.jar");
                File dbcp6 = new File(tomcatHome, "lib/tomcat-dbcp.jar");

                if (!dbcp55.exists() && !dbcp6.exists()) {
                    new File(webinfLib, Distribution.COMMONS_COLLECTIONS)
                            .delete();
                    new File(webinfLib, Distribution.COMMONS_DBCP).delete();
                    new File(webinfLib, Distribution.COMMONS_POOL).delete();
                    // JDBC driver installation into common/lib for Tomcat 5.0 is
                    // handled by ExistingTomcat50
                } else {
                    installJDBCDriver(_dist, _opts, webinfLib);
                }
            } else {
                installJDBCDriver(_dist, _opts, webinfLib);
            }

            // Remove log4j if using JBoss Application Server
            if (container.equals(InstallOptions.OTHER)
                    && _opts.getValue(InstallOptions.USING_JBOSS)
                            .equals("true")) {
                new File(webinfLib, Distribution.LOG4J).delete();
            }
            
            // FeSL configuration
            if (_opts.getBooleanValue(InstallOptions.FESL_ENABLED, false)) {
            	File originalWsdd = new File(warStage, "WEB-INF/server-config.wsdd");
            	originalWsdd.renameTo(new File(warStage, "WEB-INF/server-config.wsdd.backup.original"));
            	
            	File feslWsdd = new File(warStage, "WEB-INF/melcoe-pep-server-config.wsdd");
            	feslWsdd.renameTo(new File(warStage, "WEB-INF/server-config.wsdd"));
            }
            
            File fedoraWar = new File(installDir, fedoraWarName + ".war");
            Zip.zip(fedoraWar, warStage.listFiles());
            return fedoraWar;

        } catch (FileNotFoundException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
		}
    }

    public static void installJDBCDriver(Distribution dist,
                                         InstallOptions opts,
                                         File destDir)
            throws InstallationFailedException {
        String databaseDriver = opts.getValue(InstallOptions.DATABASE_DRIVER);
        String database = opts.getValue(InstallOptions.DATABASE);
        InputStream is;
        File driver = null;
        boolean success = true;
        try {
            if (databaseDriver.equals(InstallOptions.INCLUDED)) {
                if (database.equals(InstallOptions.INCLUDED)) {
                    is = dist.get(Distribution.JDBC_DERBY);
                    driver = new File(destDir, Distribution.JDBC_DERBY);
                    success = FileUtils.copy(is, new FileOutputStream(driver));
                } else if (database.equals(InstallOptions.DERBY)) {
                    is = dist.get(Distribution.JDBC_DERBY_NETWORK);
                    driver = new File(destDir, Distribution.JDBC_DERBY_NETWORK);
                    success = FileUtils.copy(is, new FileOutputStream(driver));
                } else if (database.equals(InstallOptions.MCKOI)) {
                    is = dist.get(Distribution.JDBC_MCKOI);
                    driver = new File(destDir, Distribution.JDBC_MCKOI);
                    success = FileUtils.copy(is, new FileOutputStream(driver));
                } else if (database.equals(InstallOptions.MYSQL)) {
                    is = dist.get(Distribution.JDBC_MYSQL);
                    driver = new File(destDir, Distribution.JDBC_MYSQL);
                    success = FileUtils.copy(is, new FileOutputStream(driver));
                } else if (database.equals(InstallOptions.POSTGRESQL)) {
                    is = dist.get(Distribution.JDBC_POSTGRESQL);
                    driver = new File(destDir, Distribution.JDBC_POSTGRESQL);
                    success = FileUtils.copy(is, new FileOutputStream(driver));
                }
            } else {
                File f =
                        new File(opts.getValue(InstallOptions.DATABASE_DRIVER));
                driver = new File(destDir, f.getName());
                success = FileUtils.copy(f, driver);
            }

            if (!success) {
                throw new InstallationFailedException("Copy to "
                        + driver.getAbsolutePath() + " failed.");
            }
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    private void deployLocalService(Container container, String filename)
            throws InstallationFailedException {
        try {
            File war = new File(installDir, filename);
            if (!FileUtils.copy(_dist.get(filename), new FileOutputStream(war))) {
                throw new InstallationFailedException("Copy to "
                        + war.getAbsolutePath() + " failed.");
            }
            container.deploy(war);
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    /**
     * Command-line entry point.
     */
    public static void main(String[] args) {
        try {
            Distribution dist = new ClassLoaderDistribution();
            InstallOptions opts = null;

            if (args.length == 0) {
                opts = new InstallOptions(dist);
            } else if (args.length == 1) {
                Map<String, String> props =
                        FileUtils.loadMap(new File(args[0]));
                opts = new InstallOptions(dist, props);
            } else {
                System.err.println("ERROR: Too many arguments.");
                System.err
                        .println("Usage: java -jar fedora-install.jar [options-file]");
                System.exit(1);
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
