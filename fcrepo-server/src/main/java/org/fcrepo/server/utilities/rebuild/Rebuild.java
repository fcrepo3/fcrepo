/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.utilities.rebuild;

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

import org.slf4j.bridge.SLF4JBridgeHandler;

import org.fcrepo.common.Constants;

import org.fcrepo.server.Module;
import org.fcrepo.server.Server;
import org.fcrepo.server.config.Configuration;
import org.fcrepo.server.config.Parameter;
import org.fcrepo.server.config.ServerConfiguration;
import org.fcrepo.server.config.ServerConfigurationParser;
import org.fcrepo.server.errors.InitializationException;
import org.fcrepo.server.storage.lowlevel.IListable;
import org.fcrepo.server.storage.lowlevel.ILowlevelStorage;
import org.fcrepo.server.storage.translation.DODeserializer;
import org.fcrepo.server.storage.translation.DOTranslationUtility;
import org.fcrepo.server.storage.translation.FOXML1_1DODeserializer;
import org.fcrepo.server.storage.types.BasicDigitalObject;
import org.fcrepo.server.storage.types.DigitalObject;
import org.fcrepo.server.utilities.ServerUtility;

import org.fcrepo.utilities.LogConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry-point for rebuilding various aspects of the repository.
 *
 * @author Chris Wilper
 */
public class Rebuild implements Constants, Runnable {

    private static Server server;

    private static Logger logger = LoggerFactory.getLogger(Rebuild.class
            .getName());

    private static final String llstoreInterface = ILowlevelStorage.class
            .getName();

    private static final String listableInterface = IListable.class.getName();

    private final Rebuilder m_rebuilder;

    private final Map<String, String> m_options;

    public Rebuild(Rebuilder rebuilder, Map<String, String> options,
            Server server)
            throws Exception {
        // set these here so DOTranslationUtility doesn't try to get a Server
        // instance
        System.setProperty("fedoraServerHost", server
                .getParameter("fedoraServerHost"));
        System.setProperty("fedoraServerPort", server
                .getParameter("fedoraServerPort"));
        System.setProperty("fedoraAppServerContext", server
                .getParameter("fedoraAppServerContext"));
        boolean serverIsRunning = ServerUtility.pingServer("http", null, null);
        if (serverIsRunning && rebuilder.shouldStopServer()) {
            throw new Exception("The Fedora server appears to be running."
                    + "  It must be stopped before the rebuilder can run.");
        }
        m_options = options;
        m_rebuilder = rebuilder;
        if (options != null) {
            try {
                // ensure rebuilds are possible before trying anything,
                // as rebuilder.start() may be destructive!
                Module mod = server.getBean(llstoreInterface, Module.class);
                Class<?> clazz = mod.getClass();
                boolean isListable = false;
                for (Class<?> iface : clazz.getInterfaces()) {
                    if (iface.getName().equals(listableInterface)) {
                        isListable = true;
                    }
                }
                if (!isListable) {
                    throw new Exception("ERROR: Rebuilds are not supported" +
                            " by " + clazz.getName() +
                            " because it does not implement the" +
                            " org.fcrepo.server.storage.lowlevel.IListable" +
                            " interface.");
                }

            } finally {
            }
        } else {
            logger.warn("Null options for " + getClass().getName());
        }
    }

    public void run() {
        try {
            if (m_options != null) {
                System.err.println();
                System.err.println("Rebuilding...");
                try {
                    // looks good, so init the rebuilder
                    m_rebuilder.start(m_options);

                    // add each object in llstore
                    ILowlevelStorage llstore =
                            (ILowlevelStorage) getServer().getModule(
                                    llstoreInterface);
                    if (llstore == null) {
                        logger.error("No module/bean definition for " +
                                llstoreInterface);
                    } else {
                        logger.info("Loaded bean/module " + llstoreInterface +
                                " with impl " + llstore.getClass().getName());
                    }
                    Iterator<String> pids = ((IListable) llstore).listObjects();
                    int total = 0;
                    int errors = 0;
                    DODeserializer deser = new FOXML1_1DODeserializer();

                    while (pids.hasNext()) {
                        total++;
                        String pid = pids.next();
                        System.out.println("Adding object #" + total + ": " +
                                pid);
                        if (!addObject(m_rebuilder, llstore, deser, pid)) {
                            errors++;
                        }
                    }
                    if (errors == 0) {
                        System.out.println("SUCCESS: " + total +
                                " objects rebuilt.");
                    } else {
                        System.out.println("WARNING: " + errors + " of " +
                                total +
                                " objects failed to rebuild due to errors.");
                    }
                } finally {
                    m_rebuilder.finish();
                    if (server != null) {
                        server.shutdown(null);
                        server = null;
                    }
                    System.err.print("Finished.");
                    System.err.println();
                }
                return;
            }
        } catch (Exception e) {
            System.err.println("Rebuild failed:");
            System.err.println(e.toString());
            e.printStackTrace(System.err);
        }
    }

    private boolean addObject(Rebuilder rebuilder, ILowlevelStorage llstore,
            DODeserializer deser, String pid) {
        InputStream in = null;
        try {
            in = llstore.retrieveObject(pid);
            DigitalObject obj = new BasicDigitalObject();
            deser.deserialize(in, obj, "UTF-8",
                    DOTranslationUtility.SERIALIZE_STORAGE_INTERNAL);
            rebuilder.addObject(obj);
            return true;
        } catch (Exception e) {
            System.out.println("WARNING: Skipped " + pid +
                    " due to exception: ");
            e.printStackTrace();
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Gets the instance of the server appropriate for rebuilding. If no such
     * instance has been initialized yet, initialize one.
     *
     * @return the server instance.
     * @throws InitializationException
     *         if initialization fails.
     */
    public static Server getServer() throws InitializationException {
        if (server == null) {
            server =
                    RebuildServer.getRebuildInstance(new File(
                            Constants.FEDORA_HOME));
        }
        return server;
    }

    private static Map<String, String> getOptions(Map<String, String> descs)
            throws IOException {
        Map<String, String> options = new HashMap<String, String>();
        if (descs != null) {
            Iterator<String> iter = descs.keySet().iterator();
            while (iter.hasNext()) {
                String name = iter.next();
                String desc = descs.get(name);
                options.put(name, getOptionValue(name, desc));
            }
        }
        int c = 1;

        if (System.getProperty("rebuilder") == null) {
            if (options.size() > 0) {
                c =
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
            } else {
                c =
                        getChoice("No options to set. Start rebuilding?",
                                new String[] {"Yes", "No, exit."});
                if (c == 0) {
                    return options;
                }
            }
        } else {
            return options;
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
        Server server = getServer();
        String[] rebuilders = server.getBeanNamesForType(Rebuilder.class);
        String[] labels = new String[rebuilders.length + 1];
        int i = 0;
        for (i = 0; i < rebuilders.length; i++) {
            Rebuilder r = server.getBean(rebuilders[i], Rebuilder.class);

            labels[i] = r.getAction();
        }
        labels[i] = "Exit";
        int choiceNum = i;
        System.out.println("Getting rebuilder... " +
                System.getProperty("rebuilder"));
        if (System.getProperty("rebuilder") == null) {
            choiceNum = getChoice("What do you want to do?", labels);
        } else {
            for (int j = 0; j < rebuilders.length; j++) {
                if (rebuilders[j].equals(System.getProperty("rebuilder"))) {
                    choiceNum = j;
                }
            }
        }
        if (choiceNum == i) {
            return null;
        } else {
            return server.getBean(rebuilders[choiceNum], Rebuilder.class);
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

    private static ServerConfiguration getServerConfig(File serverDir,
            String profile) throws IOException {
        ServerConfigurationParser parser =
                new ServerConfigurationParser(new FileInputStream(new File(
                        serverDir, "config/fedora.fcfg")));
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

    private static int
            setValuesForProfile(Configuration config, String profile) {
        int c = 0;
        Iterator<Parameter> iter =
                config.getParameters(Parameter.class).iterator();
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

    private static int setValuesForProfile(
            List<? extends Configuration> configs, String profile) {
        Iterator<? extends Configuration> iter = configs.iterator();
        int c = 0;
        while (iter.hasNext()) {
            c += setValuesForProfile(iter.next(), profile);
        }
        return c;
    }

    private static Map<String, String> getUserInput(Rebuilder rebuilder,
            File serverDir, ServerConfiguration serverConfig) throws Exception {
        if (rebuilder != null) {
            System.err.println();
            System.err.println(rebuilder.getAction());
            System.err.println();
            //refactor these, as they should be injected
            rebuilder.setServerConfiguration(serverConfig);
            rebuilder.setServerDir(serverDir);
            rebuilder.init();
            Map<String, String> options = getOptions(rebuilder.getOptions());
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
        }
        if (exit) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        String profile = null;
        if (args.length == 1) {
            profile = args[0];
        }
        if (args.length > 1) {
            for (int i = 0; i < args.length - 1; i+=2) {
                if ("-p".equals(args[i])) profile = args[i+1];
                if ("-r".equals(args[i])) System.setProperty("rebuilder", args[i+1]);
            }
            if (profile == null && System.getProperty("rebuilder") == null) {
                fail("Too many arguments", true, true);
            }
        }
        try {
            File fedoraHomeDir = new File(Constants.FEDORA_HOME);

            // Configure logging from file
            System.setProperty("fedora.home", Constants.FEDORA_HOME);
            System.setProperty("logfile.extension", "-rebuild.log");
            LogConfig.initFromFile(new File(fedoraHomeDir,
                    "server/config/logback.xml"));

            // Replace java.util.logging's default handlers with one that
            // redirects everything to SLF4J
            java.util.logging.Logger rootLogger =
                    java.util.logging.LogManager.getLogManager().getLogger("");
            java.util.logging.Handler[] handlers = rootLogger.getHandlers();
            for (int i = 0; i < handlers.length; i++) {
                rootLogger.removeHandler(handlers[i]);
            }
            SLF4JBridgeHandler.install();

            File serverDir = new File(fedoraHomeDir, "server");
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
            new Rebuild(rebuilder, options, getServer()).run();
            return;
        } catch (Throwable th) {
            String msg = th.getMessage();
            if (msg == null) {
                msg = th.getClass().getName();
            }
            fail(msg, false, false);
            th.printStackTrace();
            try {
                getServer().shutdown(null);
            } catch (Throwable t) {
                System.err.println("Server shutdown error: " + t.toString());
            }
        }
    }

}
