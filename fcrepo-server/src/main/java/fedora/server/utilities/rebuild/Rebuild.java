/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities.rebuild;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fedora.common.Constants;

import fedora.server.Server;
import fedora.server.config.Configuration;
import fedora.server.config.ModuleConfiguration;
import fedora.server.config.Parameter;
import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;
import fedora.server.errors.InitializationException;
import fedora.server.errors.LowlevelStorageException;
import fedora.server.storage.lowlevel.FileSystem;
import fedora.server.storage.lowlevel.IListable;
import fedora.server.storage.lowlevel.ILowlevelStorage;
import fedora.server.storage.translation.DODeserializer;
import fedora.server.storage.translation.DOTranslationUtility;
import fedora.server.storage.translation.FOXML1_1DODeserializer;
import fedora.server.storage.types.BasicDigitalObject;
import fedora.server.storage.types.DigitalObject;
import fedora.server.utilities.ServerUtility;

import fedora.utilities.FileComparator;

/**
 * Entry-point for rebuilding various aspects of the repository.
 *
 * @author Chris Wilper
 */
public class Rebuild
        implements Constants {

    private static Server server;

    private FileSystem fs;

    private static FileComparator _REVERSE_FILE_COMPARATOR =
            new FileComparator(true);

    /**
     * Rebuilders that the rebuild utility knows about.
     */
    public static String[] REBUILDERS =
            new String[] {"fedora.server.resourceIndex.ResourceIndexRebuilder",
                    "fedora.server.utilities.rebuild.SQLRebuilder"};

    public Rebuild(Rebuilder rebuilder,
                   Map<String, String> options,
                   ServerConfiguration serverConfig)
            throws Exception {
        // set these here so DOTranslationUtility doesn't try to get a Server
        // instance
        System.setProperty("fedoraServerHost", serverConfig
                .getParameter("fedoraServerHost").getValue());
        System.setProperty("fedoraServerPort", serverConfig
                .getParameter("fedoraServerPort").getValue());
        System.setProperty("fedoraAppServerContext", serverConfig
                .getParameter("fedoraAppServerContext").getValue());
        boolean serverIsRunning = ServerUtility.pingServer("http", null, null);
        if (serverIsRunning && rebuilder.shouldStopServer()) {
            throw new Exception("The Fedora server appears to be running."
                    + "  It must be stopped before the rebuilder can run.");
        }
        if (options != null) {
            System.err.println();
            System.err.println("Rebuilding...");
            try {
                // ensure rebuilds are possible before trying anything,
                // as rebuilder.start() may be destructive!
                final String llPackage = "fedora.server.storage.lowlevel";
                String llstoreInterface = llPackage + ".ILowlevelStorage";
                String listableInterface = llPackage + ".IListable";
                ModuleConfiguration mcfg =
                        serverConfig.getModuleConfiguration(llstoreInterface);
                Class<?> clazz = Class.forName(mcfg.getClassName());
                boolean isListable = false;
                for (Class<?> iface : clazz.getInterfaces()) {
                    if (iface.getName().equals(listableInterface)) {
                        isListable = true;
                    }
                }
                if (!isListable) {
                    throw new Exception("ERROR: Rebuilds are not supported"
                            + " by " + clazz.getName()
                            + " because it does not implement the"
                            + " fedora.server.storage.lowlevel.IListable"
                            + " interface.");
                }

                // looks good, so init the rebuilder
                rebuilder.start(options);

                // add each object in llstore
                ILowlevelStorage llstore = (ILowlevelStorage)
                        getServer().getModule(llstoreInterface);
                Iterator<String> pids = ((IListable) llstore).listObjects();
                int total = 0;
                int errors = 0;
                while (pids.hasNext()) {
                    total++;
                    String pid = pids.next();
                    System.out.println("Adding object #" + total + ": " + pid);
                    if (!addObject(rebuilder, llstore, pid)) {
                        errors++;
                    }
                }
                if (errors == 0) {
                    System.out.println("SUCCESS: " + total + " objects rebuilt.");
                } else {
                    System.out.println("WARNING: " + errors + " of " + total + " objects failed to rebuild due to errors.");
                }
            } finally {
                rebuilder.finish();
                if (server != null) {
                    server.shutdown(null);
                    server = null;
                }
            }
            System.err.print("Finished.");
            System.err.println();
        }
    }

    private boolean addObject(Rebuilder rebuilder,
                              ILowlevelStorage llstore,
                              String pid) {
        InputStream in = null;
        try {
            in = llstore.retrieveObject(pid);
            DigitalObject obj = new BasicDigitalObject();
            DODeserializer deser = new FOXML1_1DODeserializer();
            deser.deserialize(in,
                              obj,
                              "UTF-8",
                              DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);
            rebuilder.addObject(obj);
            return true;
        } catch (Exception e) {
            System.out.println("WARNING: Skipped " + pid + " due to exception: ");
            e.printStackTrace();
            return false;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) { }
            }
        }
    }

    /**
     * Gets the instance of the server appropriate for rebuilding.
     * If no such instance has been initialized yet, initialize one.
     *
     * @return the server instance.
     * @throws InitializationException if initialization fails.
     */
    public static Server getServer() throws InitializationException {
        if (server == null) {
            server = RebuildServer.getRebuildInstance(
                    new File(Constants.FEDORA_HOME));
        }
        return server;
    }

    private static Map<String, String> getOptions(Map<String, String> descs)
            throws IOException {
        Map<String, String> options = new HashMap<String, String>();
        Iterator<String> iter = descs.keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            String desc = descs.get(name);
            options.put(name, getOptionValue(name, desc));
        }
        int c =
                getChoice("Start rebuilding with the above options?",
                          new String[] {"Yes",
                                  "No, let me re-enter the options.",
                                  "No, exit."});
        if (c == 0) {
            return options;
        }
        if (c == 1) {
            System.err.println();
            return getOptions(descs);
        }
        return null;
    }

    private static String getOptionValue(String name, String desc)
            throws IOException {
        System.err.println("[" + name + "]");
        System.err.println(desc);
        System.err.println();
        System.err.print("Enter a value --> ");
        String val =
                new BufferedReader(new InputStreamReader(System.in)).readLine();
        System.err.println();
        return val;
    }

    private static Rebuilder getRebuilder() throws Exception {
        String[] labels = new String[REBUILDERS.length + 1];
        Rebuilder[] rebuilders = new Rebuilder[REBUILDERS.length];
        int i = 0;
        for (i = 0; i < REBUILDERS.length; i++) {
            Rebuilder r =
                    (Rebuilder) Class.forName(REBUILDERS[i]).newInstance();
            labels[i] = r.getAction();
            rebuilders[i] = r;
        }
        labels[i] = "Exit";
        int choiceNum = getChoice("What do you want to do?", labels);
        if (choiceNum == i) {
            return null;
        } else {
            return rebuilders[choiceNum];
        }
    }

    private static int getChoice(String title, String[] labels)
            throws IOException {
        boolean validChoice = false;
        int choiceIndex = -1;
        System.err.println(title);
        System.err.println();
        for (int i = 1; i <= labels.length; i++) {
            System.err.println("  " + i + ") " + labels[i - 1]);
        }
        System.err.println();
        while (!validChoice) {
            System.err.print("Enter (1-" + labels.length + ") --> ");
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(System.in));
            String line = in.readLine();
            try {
                int choiceNum = Integer.parseInt(line);
                if (choiceNum > 0 && choiceNum <= labels.length) {
                    choiceIndex = choiceNum - 1;
                    validChoice = true;
                }
            } catch (NumberFormatException nfe) {
            }
        }
        return choiceIndex;
    }

    private InputStream getFile(File f, String searchString)
            throws IOException, LowlevelStorageException {

        /*
         * If we don't care about the existence of a search string, don't bother
         * looking
         */
        if (searchString == null) {
            return fs.read(f);
        }

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(fs.read(f)));
            String line = reader.readLine();
            while (line != null) {
                if (line.indexOf(searchString) != -1) {
                    return fs.read(f);
                } else {
                    line = reader.readLine();
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private static ServerConfiguration getServerConfig(File serverDir,
                                                       String profile)
            throws IOException {
        ServerConfigurationParser parser =
                new ServerConfigurationParser(new FileInputStream(new File(serverDir,
                                                                           "config/fedora.fcfg")));
        ServerConfiguration serverConfig = parser.parse();
        // set all the values according to the profile, if specified
        if (profile != null) {
            int c = setValuesForProfile(serverConfig, profile);
            c +=
                    setValuesForProfile(serverConfig.getModuleConfigurations(),
                                        profile);
            c +=
                    setValuesForProfile(serverConfig
                            .getDatastoreConfigurations(), profile);
            if (c == 0) {
                throw new IOException("Unrecognized server-profile: " + profile);
            }
        }
        return serverConfig;
    }

    private static int setValuesForProfile(Configuration config, String profile) {
        int c = 0;
        Iterator<Parameter> iter = config.getParameters().iterator();
        while (iter.hasNext()) {
            Parameter param = iter.next();
            String profileValue = param.getProfileValues().get(profile);
            if (profileValue != null) {
                param.setValue(profileValue);
                c++;
            }
        }
        return c;
    }

    private static int setValuesForProfile(List configs, String profile) {
        Iterator iter = configs.iterator();
        int c = 0;
        while (iter.hasNext()) {
            c += setValuesForProfile((Configuration) iter.next(), profile);
        }
        return c;
    }

    private static Map<String, String> getUserInput(Rebuilder rebuilder,
                                                    File serverDir,
                                                    ServerConfiguration serverConfig)
            throws Exception {
        if (rebuilder != null) {
            System.err.println();
            System.err.println(rebuilder.getAction());
            System.err.println();
            Map<String, String> options =
                    getOptions(rebuilder.init(serverDir, serverConfig));
            return options;
        } else {
            return new HashMap<String, String>();
        }
    }

    public static void fail(String message, boolean showUsage, boolean exit) {
        System.err.println("Error: " + message);
        System.err.println();
        if (showUsage) {
            System.err.println("Usage: fedora-rebuild [server-profile]");
            System.err.println();
            System.err
                    .println("server-profile : the argument you start Fedora with, such as 'mckoi'");
            System.err
                    .println("                 or 'oracle'.  If you start fedora with 'fedora-start'");
            System.err
                    .println("                 (without arguments), don't specify a server-profile here either.");
            System.err.println();
        }
        if (exit) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // tell commons-logging to use log4j
        System.setProperty("org.apache.commons.logging.LogFactory",
                           "org.apache.commons.logging.impl.Log4jFactory");
        System.setProperty("org.apache.commons.logging.Log",
                           "org.apache.commons.logging.impl.Log4JLogger");
        // log4j
        // File log4jConfig = new File(new File(homeDir), "config/log4j.xml");
        // DOMConfigurator.configure(log4jConfig.getPath());
        String profile = null;
        if (args.length > 0) {
            profile = args[0];
        }
        if (args.length > 1) {
            fail("Too many arguments", true, true);
        }
        try {
            File serverDir =
                    new File(new File(Constants.FEDORA_HOME), "server");
            ServerConfiguration serverConfig =
                    getServerConfig(serverDir, profile);
            System.err.println();
            System.err.println("                       Fedora Rebuild Utility");
            System.err
                    .println("                     ..........................");
            System.err.println();
            System.err
                    .println("WARNING: Live rebuilds are not currently supported.");
            System.err
                    .println("         Make sure your server is stopped before continuing.");
            System.err.println();
            System.err.println("Server directory is " + serverDir.toString());
            if (profile != null) {
                System.err.print("Server profile is " + profile);
            }
            System.err.println();
            System.err
                    .println("---------------------------------------------------------------------");
            System.err.println();
            Rebuilder rebuilder = getRebuilder();
            Map<String, String> options =
                    getUserInput(rebuilder, serverDir, serverConfig);
            new Rebuild(rebuilder, options, serverConfig);
        } catch (Throwable th) {
            String msg = th.getMessage();
            if (msg == null) {
                msg = th.getClass().getName();
            }
            fail(msg, false, false);
            th.printStackTrace();
        }
    }

}
