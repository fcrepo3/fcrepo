/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities.status;

import java.io.File;

import fedora.common.Constants;

public class ServerStatusTool {

    /**
     * Default number of seconds watch-startup should wait for the state to
     * change from NOT_STARTING to STARTING before giving up.
     */
    public static final int DEFAULT_STARTING_TIMEOUT = 45;

    /**
     * Default number of seconds watch-startup should wait for the state to
     * change from STARTING to STARTED or STARTUP_FAILED before giving up.
     */
    public static final int DEFAULT_STARTUP_TIMEOUT = 120;

    /**
     * Default number of seconds watch-shutdown should wait for the state to
     * change from STARTED to STOPPING before giving up.
     */
    public static final int DEFAULT_STOPPING_TIMEOUT = 45;

    /**
     * Default number of seconds watch-shutdown should wait for the state to
     * change from STOPPING to STOPPED or STOPPED_WITH_ERR before giving up.
     */
    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 600;

    private final ServerStatusFile _statusFile;

    public ServerStatusTool(File serverHome)
            throws Exception {

        _statusFile = new ServerStatusFile(serverHome);
    }

    /**
     * If the server appears not to be running, initialize the status file to
     * indicate that the server is not running and has not yet indicated that it
     * is starting (NOT_STARTING). Otherwise, throw an exception.
     */
    public void init() throws Exception {

        // If the status file exists, make sure it doesn't indicate
        // that the server is running!
        if (_statusFile.exists()) {
            ServerStatusMessage[] messages = getAllMessages();
            ServerStatusMessage lastMessage = messages[messages.length - 1];
            if (lastMessage.getState() == ServerState.STARTED) {
                throw new Exception("The server is already running or was shut down unexpectedly.\n"
                        + "If the server has shut down unexpectedly, you must manually delete\n"
                        + "the following file: "
                        + _statusFile.getPath()
                        + "\n"
                        + "then try again.");
            }
        }

        _statusFile.clear();
        _statusFile.append(ServerState.NOT_STARTING,
                           "Waiting for startup to begin");
    }

    /**
     * Watch the status file and print details to standard output until the
     * STARTED or STARTUP_FAILED state is encountered. If there are any problems
     * reading the status file, a timeout is reached, or STARTUP_FAILED is
     * encountered, this will throw an exception.
     */
    public void watchStartup(int startingTimeout, int startupTimeout)
            throws Exception {

        // use this for timeout checks later
        long startTime = System.currentTimeMillis();

        ServerStatusMessage[] messages = getAllMessages();

        ServerStatusMessage lastMessage = messages[messages.length - 1];

        boolean starting = false;
        boolean started = false;
        while (!started) {

            showStartup(messages);

            // update started and starting flags, and
            // throw a startup exception if startup failed
            // is encountered
            for (ServerStatusMessage element : messages) {
                ServerState state = element.getState();
                if (state == ServerState.STARTING) {
                    starting = true;
                } else if (state == ServerState.STARTED) {
                    started = true;
                } else if (state == ServerState.STARTUP_FAILED) {
                    throw new Exception("Fedora startup failed (see above)");
                }
            }

            if (!started) {

                // wait half a second
                try {
                    Thread.sleep(500);
                } catch (Throwable th) {
                }

                // throw an exception if either timeout has been
                // exceeded
                long now = System.currentTimeMillis();
                if (!starting) {
                    if ((now - startTime) / 1000 > startingTimeout) {
                        throw new Exception("Server startup did not begin within "
                                + startingTimeout + " seconds");
                    }
                }
                if ((now - startTime) / 1000 > startupTimeout) {
                    throw new Exception("Server startup did not complete within "
                            + startupTimeout + " seconds");
                }

                // get next batch of messages
                messages = _statusFile.getMessages(lastMessage);
                if (messages.length > 0) {
                    lastMessage = messages[messages.length - 1];
                }
            }

        }

    }

    private void showStartup(ServerStatusMessage[] messages) {

        for (ServerStatusMessage element : messages) {
            ServerState state = element.getState();
            if (state != ServerState.STOPPING && state != ServerState.STOPPED) {
                String detail = element.getDetail();
                System.out.print(ServerStatusMessage.dateToString(element
                        .getDate())
                        + " - ");
                if (state == ServerState.NOT_STARTING
                        || state == ServerState.STARTING) {
                    if (detail != null) {
                        // print it like: date - detail
                        System.out.println(detail);
                    } else {
                        // print it like: date - stateName
                        System.out.println(element.getState().getName());
                    }
                } else {
                    // print it like: date - state[\ndetail]
                    System.out.println(element.getState().getName());
                    if (detail != null) {
                        System.out.println(detail);
                    }
                }
            }
        }
    }

    /**
     * Watch the status file and print details to standard output until the
     * STOPPED or STOPPED_WITH_ERR state is encountered. If there are any
     * problems reading the status file, a timeout is reached, or
     * STOPPED_WITH_ERR is encountered, this will throw an exception.
     */
    public void watchShutdown(int stoppingTimeout, int shutdownTimeout)
            throws Exception {

        if (!_statusFile.exists()) {
            _statusFile
                    .append(ServerState.STOPPING,
                            "WARNING: Server status file did not exist; re-created");
        }

        // use this for timeout checks later
        long startTime = System.currentTimeMillis();

        ServerStatusMessage[] messages = getAllMessages();

        ServerStatusMessage lastMessage = messages[messages.length - 1];

        boolean stopping = false;
        boolean stopped = false;
        while (!stopped) {

            showShutdown(messages);

            // update stopping and stopped flags, and
            // throw a shutdown exception if STOPPED_WITH_ERR
            // is encountered
            for (ServerStatusMessage element : messages) {
                ServerState state = element.getState();
                if (state == ServerState.STOPPING) {
                    stopping = true;
                } else if (state == ServerState.STOPPED) {
                    stopped = true;
                } else if (state == ServerState.STOPPED_WITH_ERR) {
                    throw new Exception("Fedora shutdown finished with error (see above)");
                }
            }

            if (!stopped) {

                // wait half a second
                try {
                    Thread.sleep(500);
                } catch (Throwable th) {
                }

                // throw an exception if either timeout has been
                // exceeded
                long now = System.currentTimeMillis();
                if (!stopping) {
                    if ((now - startTime) / 1000 > stoppingTimeout) {
                        throw new Exception("Server shutdown did not begin within "
                                + stoppingTimeout + " seconds");
                    }
                }
                if ((now - startTime) / 1000 > shutdownTimeout) {
                    throw new Exception("Server shutdown did not complete within "
                            + shutdownTimeout + " seconds");
                }

                // get next batch of messages
                messages = _statusFile.getMessages(lastMessage);
                if (messages.length > 0) {
                    lastMessage = messages[messages.length - 1];
                }
            }

        }

    }

    private void showShutdown(ServerStatusMessage[] messages) {

        for (ServerStatusMessage element : messages) {
            ServerState state = element.getState();
            if (state == ServerState.STOPPING || state == ServerState.STOPPED
                    || state == ServerState.STOPPED_WITH_ERR) {
                String detail = element.getDetail();
                System.out.print(ServerStatusMessage.dateToString(element
                        .getDate())
                        + " - ");
                if (state == ServerState.STOPPING) {
                    if (detail != null) {
                        // print it like: date - detail
                        System.out.println(detail);
                    } else {
                        // print it like: date - stateName
                        System.out.println(element.getState().getName());
                    }
                } else {
                    // print it like: date - state[\ndetail]
                    System.out.println(element.getState().getName());
                    if (state == ServerState.STOPPED_WITH_ERR && detail != null) {
                        System.out.println(detail);
                    }
                }
            }
        }
    }

    /**
     * Show a human-readable form of the latest message in the server status
     * file. If the status file doesn't yet exist, this will print a special
     * status message indicating the server is new. The response will have the
     * following form.
     * 
     * <pre>
     * STATE  : Some State
     * AS OF  : 2006-03-29 06:44:23AM EST
     * DETAIL : Detail line 1, if it exists
     * Detail line 2, if it exists
     * Detail line 3, etc..
     * </pre>
     */
    public void showStatus() throws Exception {

        ServerStatusMessage message;
        if (_statusFile.exists()) {
            ServerStatusMessage[] messages = getAllMessages();
            message = messages[messages.length - 1];
        } else {
            message = ServerStatusMessage.NEW_SERVER_MESSAGE;
        }
        System.out.println(message.toString());
    }

    // get all messages in the status file, validating that
    // it exists and contains at least one message. 
    // if the first message's state is not ServerState.NOT_STARTING,
    // print a warning
    private ServerStatusMessage[] getAllMessages() throws Exception {

        ServerStatusMessage[] messages = _statusFile.getMessages(null);
        if (messages.length == 0) {
            System.out
                    .println("WARNING: Server status file is empty; re-creating");
            init();
            messages = _statusFile.getMessages(null);
        }
        ServerState firstState = messages[0].getState();
        if (firstState != ServerState.NOT_STARTING) {
            System.out
                    .println("WARNING: Server status file is missing one or more messages");
        }
        return messages;
    }

    private static void showUsage(String err) {
        if (err != null) {
            System.out.println("ERROR: " + err);
        }
        System.out.println("Usage: ServerStatusTool init");
        System.out.println("   Or: ServerStatusTool show-status");
        System.out
                .println("   Or: ServerStatusTool watch-startup [starting-timeout] [startup-timeout]");
        System.out
                .println("   Or: ServerStatusTool watch-shutdown [stopping-timeout] [shutdown-timeout]");
    }

    private static int getInt(String s) throws Exception {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Exception("Not an integer: " + s);
        }
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            showUsage(null);
            System.exit(1);
        } else {
            String cmd = args[0];
            if (cmd.equals("init") || cmd.equals("watch-startup")
                    || cmd.equals("watch-shutdown")
                    || cmd.equals("show-status")) {
                try {
                    String fedoraHome = Constants.FEDORA_HOME;
                    if (fedoraHome == null) {
                        throw new Exception("FEDORA_HOME is undefined");
                    }
                    File serverHome = new File(new File(fedoraHome), "server");
                    ServerStatusTool tool = new ServerStatusTool(serverHome);
                    if (cmd.equals("init")) {
                        tool.init();
                    } else if (cmd.equals("watch-startup")) {
                        int startingTimeout = DEFAULT_STARTING_TIMEOUT;
                        int startupTimeout = DEFAULT_STARTUP_TIMEOUT;
                        if (args.length > 1) {
                            startingTimeout = getInt(args[1]);
                            if (args.length > 2) {
                                startupTimeout = getInt(args[2]);
                            }
                        }
                        tool.watchStartup(startingTimeout, startupTimeout);
                    } else if (cmd.equals("watch-shutdown")) {
                        int stoppingTimeout = DEFAULT_STOPPING_TIMEOUT;
                        int shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
                        if (args.length > 1) {
                            stoppingTimeout = getInt(args[1]);
                            if (args.length > 2) {
                                shutdownTimeout = getInt(args[2]);
                            }
                        }
                        tool.watchShutdown(stoppingTimeout, shutdownTimeout);
                    } else {
                        tool.showStatus();
                    }
                    System.exit(0);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg == null
                            || !e.getClass().getName()
                                    .equals("java.lang.Exception")) {
                        System.out.println("ERROR: " + e.getClass().getName());
                        e.printStackTrace();
                    } else {
                        System.out.println("ERROR: " + msg);
                    }
                    System.exit(1);
                }
            } else {
                showUsage("Bad argument: " + cmd);
                System.exit(1);
            }
        }
    }

}
