/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.utilities.install;

import java.io.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.fcrepo.server.config.ModuleConfiguration;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.config.ServerConfigurationParser;
import org.fcrepo.server.resourceIndex.ResourceIndex;
import org.fcrepo.server.security.BESecurityConfig;
import org.fcrepo.server.security.DefaultRoleConfig;
import org.fcrepo.server.security.servletfilters.xmluserfile.FedoraUsers;
import org.fcrepo.server.security.servletfilters.xmluserfile.User;

import org.fcrepo.utilities.ExecUtility;
import org.fcrepo.utilities.FileUtils;
import org.fcrepo.utilities.Zip;

public class FedoraHome {

    private final Distribution _dist;

    private final InstallOptions _opts;

    private final File _installDir;

    private final boolean _clientOnlyInstall;

    private InetAddress _host;

    private boolean _usingAkubra;

    public FedoraHome(Distribution dist, InstallOptions opts) {
        _dist = dist;
        _opts = opts;
        _installDir = new File(_opts.getValue(InstallOptions.FEDORA_HOME));
        _clientOnlyInstall =
                _opts.getValue(InstallOptions.INSTALL_TYPE)
                        .equals(InstallOptions.INSTALL_CLIENT);
    }

    public void install() throws InstallationFailedException {
        unpack();

        if (!_clientOnlyInstall) {
            configure();
        }
    }

    /**
     * Unpacks the contents of the FEDORA_HOME directory from the Distribution.
     *
     * @throws InstallationFailedException
     */
    private void unpack() throws InstallationFailedException {
        System.out.println("Preparing FEDORA_HOME...");

        if (!_installDir.exists() && !_installDir.mkdirs()) {
            throw new InstallationFailedException("Unable to create FEDORA_HOME: "
                    + _installDir.getAbsolutePath());
        }
        if (!_installDir.isDirectory()) {
            throw new InstallationFailedException(_installDir.getAbsolutePath()
                    + " is not a directory");
        }
        try {
            Zip.unzip(_dist.get(Distribution.FEDORA_HOME), _installDir);
            setScriptsExecutable(new File(_installDir, "client"
                    + File.separator + "bin"));

            File serverDir = new File(_installDir, "server");
            if (_clientOnlyInstall) {
                FileUtils.delete(serverDir);
            } else {
                setScriptsExecutable(new File(serverDir, "bin"));
            }
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    /**
     * Sets various configuration files based on InstallOptions
     *
     * @throws InstallationFailedException
     */
    private void configure() throws InstallationFailedException {
        configureFCFG();
        if (_usingAkubra) {
            configureAkubra();
        }
        configureSpringProperties();
        configureSpringAuth();
        configureFedoraUsers();
        configureBeSecurity();
        configureSpringTestConfigs();
    }

    private void configureFCFG() throws InstallationFailedException {
        System.out.println("\tConfiguring fedora.fcfg");
        File fcfgBase =
                new File(_installDir,
                         "server/fedora-internal-use/config/fedora-base.fcfg");
        File fcfg = new File(_installDir, "server/config/fedora.fcfg");

        Properties props = new Properties();
        if (_opts.getValue(InstallOptions.TOMCAT_HTTP_PORT) != null) {
            props.put("server:fedoraServerPort",
                      _opts.getValue(InstallOptions.TOMCAT_HTTP_PORT));
        }
        if (_opts.getValue(InstallOptions.TOMCAT_SHUTDOWN_PORT) != null) {
            props.put("server:fedoraShutdownPort",
                      _opts.getValue(InstallOptions.TOMCAT_SHUTDOWN_PORT));
        }
        if (_opts.getValue(InstallOptions.TOMCAT_SSL_PORT) != null) {
            props.put("server:fedoraRedirectPort",
                      _opts.getValue(InstallOptions.TOMCAT_SSL_PORT));
        }
        if (_opts.getValue(InstallOptions.FEDORA_SERVERHOST) != null) {
            props.put("server:fedoraServerHost",
                      _opts.getValue(InstallOptions.FEDORA_SERVERHOST));
        }

        if (_opts.getValue(InstallOptions.FEDORA_APP_SERVER_CONTEXT) != null) {
            props.put("server:fedoraAppServerContext",
                      _opts.getValue(InstallOptions.FEDORA_APP_SERVER_CONTEXT));
        }

        String database = _opts.getValue(InstallOptions.DATABASE);
        String dbPoolName = "";
        String backslashIsEscape = "true";
        if (database.equals(InstallOptions.DERBY)
                || database.equals(InstallOptions.INCLUDED)) {
            dbPoolName = "localDerbyPool";
            backslashIsEscape = "false";
        } else if (database.equals(InstallOptions.MYSQL)) {
            dbPoolName = "localMySQLPool";
        } else if (database.equals(InstallOptions.ORACLE)) {
            dbPoolName = "localOraclePool";
            backslashIsEscape = "false";
        } else if (database.equals(InstallOptions.POSTGRESQL)) {
            dbPoolName = "localPostgreSQLPool";
        } else {
            throw new InstallationFailedException("unable to configure for unknown database: "
                    + database);
        }
        props.put("module.org.fcrepo.server.storage.DOManager:storagePool",
                  dbPoolName);
        props.put("module.org.fcrepo.server.search.FieldSearch:connectionPool",
                  dbPoolName);
        props.put("module.org.fcrepo.server.storage.ConnectionPoolManager:poolNames",
                  dbPoolName);
        props.put("module.org.fcrepo.server.storage.ConnectionPoolManager:defaultPoolName",
                  dbPoolName);
        props.put("module.org.fcrepo.server.storage.lowlevel.ILowlevelStorage:backslash_is_escape",
                  backslashIsEscape);
        props.put("datastore." + dbPoolName + ":jdbcURL",
                  _opts.getValue(InstallOptions.DATABASE_JDBCURL));
        props.put("datastore." + dbPoolName + ":dbUsername",
                  _opts.getValue(InstallOptions.DATABASE_USERNAME));
        props.put("datastore." + dbPoolName + ":dbPassword",
                  _opts.getValue(InstallOptions.DATABASE_PASSWORD));
        props.put("datastore." + dbPoolName + ":jdbcDriverClass",
                  _opts.getValue(InstallOptions.DATABASE_DRIVERCLASS));

        if (_opts.getBooleanValue(InstallOptions.XACML_ENABLED, true)) {
            props.put("module.org.fcrepo.server.security.Authorization:ENFORCE-MODE",
                      "enforce-policies");
        } else {
            props.put("module.org.fcrepo.server.security.Authorization:ENFORCE-MODE",
                      "permit-all-requests");
        }

        if (_opts.getBooleanValue(InstallOptions.RI_ENABLED, true)) {
            props.put("module.org.fcrepo.server.resourceIndex.ResourceIndex:level",
                      String.valueOf(ResourceIndex.INDEX_LEVEL_ON));
        } else {
            props.put("module.org.fcrepo.server.resourceIndex.ResourceIndex:level",
                      String.valueOf(ResourceIndex.INDEX_LEVEL_OFF));
        }

        if (_opts.getBooleanValue(InstallOptions.MESSAGING_ENABLED, false)) {
            props.put("module.org.fcrepo.server.messaging.Messaging:enabled",
                      String.valueOf(true));
            props.put("module.org.fcrepo.server.messaging.Messaging:java.naming.provider.url",
                      _opts.getValue(InstallOptions.MESSAGING_URI));
        } else {
            props.put("module.org.fcrepo.server.messaging.Messaging:enabled",
                      String.valueOf(false));
        }

        props.put("module.org.fcrepo.server.access.Access:doMediateDatastreams",
                  _opts.getValue(InstallOptions.APIA_AUTH_REQUIRED));

        // FeSL AuthZ needs a management decorator for syncing the policy cache with policies in objects
        if (_opts.getBooleanValue(InstallOptions.FESL_AUTHZ_ENABLED, false)) {
            // NOTE: assumes messaging decorator only is present in fedora-base.fcfg as decorator1
            props.put("module.org.fcrepo.server.management.Management:decorator2",
                      "org.fcrepo.server.security.xacml.pdp.decorator.PolicyIndexInvocationHandler");
        }

        try {
            FileInputStream fis = new FileInputStream(fcfgBase);
            ServerConfiguration config =
                    new ServerConfigurationParser(fis).parse();
            config.applyProperties(props);

            // If using akubra-fs, set the class of the module and clear params.
            String llStoreType = _opts.getValue(InstallOptions.LLSTORE_TYPE);
            if (llStoreType == null || llStoreType.equals("akubra-fs")) {
                ModuleConfiguration mConfig =
                        config.getModuleConfiguration("org.fcrepo.server.storage.lowlevel.ILowlevelStorage");
                config.getModuleConfigurations().remove(mConfig);
                _usingAkubra = true;
            }

            config.serialize(new FileOutputStream(fcfg));
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    private void configureAkubra() throws InstallationFailedException {
        // Rewrite server/config/akubra-llstore.xml replacing the
        // /tmp/[object|datastream]Store constructor-arg values
        // with $FEDORA_HOME/data/[object|datastream]Store
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            File file =
                    new File(_installDir,
                             "server/config/spring/akubra-llstore.xml");
            reader =
                    new BufferedReader(new InputStreamReader(new FileInputStream(file),
                                                             "UTF-8"));

            File dataDir = new File(_installDir, "data");
            String oPath = dataDir.getPath() + File.separator + "objectStore";
            String dPath =
                    dataDir.getPath() + File.separator + "datastreamStore";
            StringBuilder xml = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                if (line.indexOf("/tmp/objectStore") != -1) {
                    line = "    <constructor-arg value=\"" + oPath + "\"/>";
                } else if (line.indexOf("/tmp/datastreamStore") != -1) {
                    line = "    <constructor-arg value=\"" + dPath + "\"/>";
                }
                xml.append(line + "\n");
                line = reader.readLine();
            }
            reader.close();

            writer =
                    new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                           "UTF-8"));
            writer.print(xml.toString());
            writer.close();
        } catch (IOException e) {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
            throw new InstallationFailedException(e.getClass().getName() + ":"
                    + e.getMessage());
        }
    }

    private void configureSpringProperties() throws InstallationFailedException {
        Properties springProps = new Properties();

        /* Set up ssl configuration */
        springProps
                .put("fedora.port",
                     _opts.getValue(InstallOptions.TOMCAT_HTTP_PORT, "8080"));
        if (_opts.getBooleanValue(InstallOptions.SSL_AVAILABLE, false)) {
            springProps.put("fedora.port.secure", _opts
                    .getValue(InstallOptions.TOMCAT_SSL_PORT, "8443"));
        } else {
            springProps.put("fedora.port.secure", _opts
                    .getValue(InstallOptions.TOMCAT_HTTP_PORT, "8080"));
        }

        springProps
                .put("security.ssl.api.access", _opts
                        .getBooleanValue(InstallOptions.APIA_SSL_REQUIRED,
                                         false) ? "REQUIRES_SECURE_CHANNEL"
                        : "ANY_CHANNEL");
        springProps
                .put("security.ssl.api.management", _opts
                        .getBooleanValue(InstallOptions.APIM_SSL_REQUIRED,
                                         false) ? "REQUIRES_SECURE_CHANNEL"
                        : "ANY_CHANNEL");
        springProps.put("security.ssl.api.default", "ANY_CHANNEL");

        springProps.put("security.fesl.authN.jaas.apia.enabled", _opts
                        .getValue(InstallOptions.APIA_AUTH_REQUIRED, "false"));

        /* Set up authN, authZ filter configuration */
        StringBuilder filters = new StringBuilder();
        if (_opts.getBooleanValue(InstallOptions.FESL_AUTHN_ENABLED, false)) {
            filters.append("AuthFilterJAAS");
        } else {
            filters.append("SetupFilter,XmlUserfileFilter,EnforceAuthnFilter,FinalizeFilter");
        }

        if (_opts.getBooleanValue(InstallOptions.FESL_AUTHZ_ENABLED, false)) {
            filters.append(",PEPFilter");
        }

        springProps.put("security.auth.filters", filters.toString());

        FileOutputStream out = null;
        try {
            out =
                    new FileOutputStream(new File(_installDir,
                                                  "server/config/spring/web/web.properties"));
            springProps.store(out, "Spring override properties");
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /*
     * This is an ugly workaround for the fact that spring security namespace
     * config does not support property substitution into lists of beans. It is
     * also ugly because
     */
    private void configureSpringAuth() throws InstallationFailedException {
        String PATTERN = "${security.auth.filters}";
        String PATTERN_APIA = "${security.auth.filters.apia}";

        StringBuilder filters = new StringBuilder();
        StringBuilder filters_apia = null;

        boolean needsbugFix = false;

        if (_opts.getBooleanValue(InstallOptions.FESL_AUTHN_ENABLED, false)) {
            filters.append("AuthFilterJAAS");
        } else {
            filters.append("SetupFilter,XmlUserfileFilter,EnforceAuthnFilter,FinalizeFilter");
            needsbugFix = true;
        }

        if (_opts.getBooleanValue(InstallOptions.APIA_AUTH_REQUIRED, false)) {
            filters_apia = new StringBuilder(filters.toString());
        } else {
            filters_apia = new StringBuilder();
        }

        if (_opts.getBooleanValue(InstallOptions.FESL_AUTHZ_ENABLED, false)) {
            filters.append(",PEPFilter");
            filters_apia.append(",PEPFilter");
        }

        FileInputStream springConfig = null;
        PrintWriter writer = null;
        try {
            File xmlFile =
                    new File(_installDir,
                             "server/config/spring/web/security.xml");
            springConfig = new FileInputStream(xmlFile);
            String content =
                    IOUtils.toString(springConfig).replace(PATTERN, filters)
                            .replace(PATTERN_APIA, filters_apia);

            if (!needsbugFix) {
                /* Delete classic authN bugfix when not applicable */
                content = content.replaceFirst("(?s)<!-- BUG.+?/BUG -->", "");
            }

            springConfig.close();

            writer =
                    new PrintWriter(new OutputStreamWriter(new FileOutputStream(xmlFile),
                                                           "UTF-8"));
            writer.print(content);
            writer.close();
        } catch (Exception e) {
            IOUtils.closeQuietly(springConfig);
            IOUtils.closeQuietly(writer);
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    private void configureSpringTestConfigs()
            throws InstallationFailedException {
        if (_opts.getBooleanValue(InstallOptions.TEST_SPRING_CONFIGS, false)) {
            FileInputStream springConfig = null;
            PrintWriter writer = null;
            try {
                File springDir = new File(_installDir, "server/config/spring");
                for (File file : springDir.listFiles()) {
                    if (file.isFile()) {
                        springConfig = new FileInputStream(file);
                        String content = IOUtils.toString(springConfig);
                        content =
                                content.replaceAll("(?s)<!-- TESTONLY(.+?)/TESTONLY -->",
                                                   "$1");
                        springConfig.close();

                        writer =
                                new PrintWriter(new OutputStreamWriter(new FileOutputStream(file),
                                                                       "UTF-8"));
                        writer.print(content);
                        writer.close();
                    }
                }
            } catch (Exception e) {
                IOUtils.closeQuietly(springConfig);
                IOUtils.closeQuietly(writer);
                throw new InstallationFailedException(e.getMessage(), e);
            }
        }
    }

    private void configureFedoraUsers() throws InstallationFailedException {
        FedoraUsers fu = FedoraUsers.getInstance();
        for (User user : fu.getUsers()) {
            if (user.getName().equals("fedoraAdmin")) {
                user.setPassword(_opts
                        .getValue(InstallOptions.FEDORA_ADMIN_PASS));
            }
        }

        try {
            Writer outputWriter =
                    new BufferedWriter(new FileWriter(FedoraUsers.fedoraUsersXML));
            fu.write(outputWriter);
            outputWriter.close();
        } catch (IOException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
    }

    private void configureBeSecurity() throws InstallationFailedException {
        System.out.println("\tInstalling beSecurity");
        File beSecurity =
                new File(_installDir, "/server/config/beSecurity.xml");
        boolean apiaAuth =
                _opts.getBooleanValue(InstallOptions.APIA_AUTH_REQUIRED, false);
        boolean apiaSSL =
                _opts.getBooleanValue(InstallOptions.APIA_SSL_REQUIRED, false);
        //boolean apimSSL = _opts.getBooleanValue(InstallOptions.APIM_SSL_REQUIRED, false);

        String[] ipList;
        String host = _opts.getValue(InstallOptions.FEDORA_SERVERHOST);
        if (host != null && host.length() != 0
                && !(host.equals("localhost") || host.equals("127.0.01"))) {
            ipList = new String[] {"127.0.0.1", getHost()};
        } else {
            ipList = new String[] {"127.0.0.1"};
        }

        PrintWriter pwriter;
        try {
            pwriter = new PrintWriter(new FileOutputStream(beSecurity));
        } catch (FileNotFoundException e) {
            throw new InstallationFailedException(e.getMessage(), e);
        }
        BESecurityConfig becfg = new BESecurityConfig();

        becfg.setDefaultConfig(new DefaultRoleConfig());
        becfg.setInternalBasicAuth(new Boolean(apiaAuth));
        becfg.setInternalIPList(ipList);
        becfg.setInternalPassword("changeme");
        becfg.setInternalSSL(new Boolean(apiaSSL));
        becfg.setInternalUsername("fedoraIntCallUser");
        becfg.write(true, true, pwriter);
        pwriter.close();
    }

    private String getHost() throws InstallationFailedException {
        if (_host == null) {
            String host = _opts.getValue(InstallOptions.FEDORA_SERVERHOST);
            try {
                _host = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new InstallationFailedException(e.getMessage(), e);
            }
        }
        return _host.getHostAddress();
    }

    /**
     * Make scripts (ending with .sh) executable on *nix systems.
     */
    public static void setScriptsExecutable(File dir) {
        String os = System.getProperty("os.name");
        if (os != null && !os.startsWith("Windows")) {
            FileFilter filter = FileUtils.getSuffixFileFilter(".sh");
            setExecutable(dir, filter);
        }
    }

    private static void setExecutable(File dir, FileFilter filter) {
        File[] files;
        if (filter != null) {
            files = dir.listFiles(filter);
        } else {
            files = dir.listFiles();
        }
        for (File element : files) {
            ExecUtility.exec(new String[] {"chmod", "+x",
                    element.getAbsolutePath()});
        }
    }
}
