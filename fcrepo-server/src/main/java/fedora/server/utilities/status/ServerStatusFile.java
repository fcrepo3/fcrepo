/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities.status;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The file consists of one or more serialized <code>ServerStatusMessage</code>s.
 * The format of the file is simple:
 * 
 * <pre>
 * [STATUS]
 * State
 * Date
 * [/STATUS]
 * [STATUS]
 * State
 * Date
 * DetailLine1
 * DetailLine2
 * [/STATUS]
 * </pre>
 */
public class ServerStatusFile {

    public static final String FILENAME = "status";

    public static final String BEGIN_LINE = "[STATUS]";

    public static final String END_LINE = "[/STATUS]";

    private final File _file;

    public ServerStatusFile(File serverHome)
            throws Exception {

        if (!serverHome.isDirectory()) {
            throw new Exception("Server home directory not found: "
                    + serverHome.getPath());
        }

        _file = new File(serverHome, FILENAME);
    }

    public String getPath() {
        return _file.getPath();
    }

    public synchronized void clear() throws Exception {

        if (_file.exists()) {
            boolean deleted = _file.delete();
            if (!deleted) {
                throw new Exception("Failed to delete server status file: "
                        + _file.getPath());
            }
        }
    }

    public synchronized boolean exists() {

        return _file.exists();
    }

    public synchronized void appendError(ServerState state, Throwable detail)
            throws Exception {

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter, true);
        detail.printStackTrace(writer);
        writer.close();

        append(state, stringWriter.toString());
    }

    public synchronized void append(ServerState state, String detail)
            throws Exception {

        ServerStatusMessage message =
                new ServerStatusMessage(state, null, detail);

        FileOutputStream out = null;
        FileChannel channel = null;
        FileLock lock = null;
        try {

            out = new FileOutputStream(_file, true); // append mode
            channel = out.getChannel();
            lock = channel.lock(); // block till we get an OS-level exclusive lock

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
            serialize(message, writer);
            writer.close();

        } catch (IOException ioe) {
            throw new Exception("Error opening server status file for writing: "
                                        + _file.getPath(),
                                ioe);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (Exception e) {
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Get all messages in the status file, or only those after the given
     * message if it is non-null. If the status file doesn't exist or can't be
     * parsed, throw an exception.
     */
    public synchronized ServerStatusMessage[] getMessages(ServerStatusMessage afterMessage)
            throws Exception {

        boolean sawAfterMessage;
        String afterMessageString = null;
        if (afterMessage == null) {
            sawAfterMessage = true;
        } else {
            sawAfterMessage = false;
            afterMessageString = afterMessage.toString();
        }

        FileInputStream in = null;
        try {

            in = new FileInputStream(_file);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in));

            List messages = new ArrayList();
            ServerStatusMessage message = getNextMessage(reader);
            while (message != null) {
                if (!sawAfterMessage) {
                    if (message.toString().equals(afterMessageString)) {
                        sawAfterMessage = true;
                    }
                } else {
                    messages.add(message);
                }
                message = getNextMessage(reader);
            }

            return (ServerStatusMessage[]) messages
                    .toArray(new ServerStatusMessage[0]);

        } catch (IOException ioe) {
            throw new Exception("Error opening server status file for reading: "
                                        + _file.getPath(),
                                ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private void serialize(ServerStatusMessage message, PrintWriter writer)
            throws Exception {

        writer.println(BEGIN_LINE);
        writer.println(message.getState().toString());
        writer.println(ServerStatusMessage.dateToString(message.getDate()));
        if (message.getDetail() != null) {
            writer.println(message.getDetail());
        }
        writer.println(END_LINE);
        writer.println();
    }

    // return the next message, or null if there are no more messages in the file
    private ServerStatusMessage getNextMessage(BufferedReader reader)
            throws Exception {

        boolean messageStarted = false;

        String line = reader.readLine();
        while (line != null && !messageStarted) {
            if (line.equals(BEGIN_LINE)) {
                messageStarted = true;
            } else {
                line = reader.readLine();
            }
        }

        if (messageStarted) {

            // get and parse first two required lines
            ServerState state = ServerState.fromString(getNextLine(reader));
            Date time = ServerStatusMessage.stringToDate(getNextLine(reader));
            String detail = null;

            // read optional detail lines till END_LINE
            line = getNextLine(reader);
            if (!line.equals(END_LINE)) {
                StringBuffer buf = new StringBuffer();
                while (!line.equals(END_LINE)) {
                    buf.append(line + "\n");
                    line = getNextLine(reader);
                }
                detail = buf.toString();
            }

            return new ServerStatusMessage(state, time, detail);
        } else {

            return null;
        }

    }

    // get the next line or throw an exception if eof was reached
    private String getNextLine(BufferedReader reader) throws Exception {
        String line = reader.readLine();
        if (line != null) {
            return line;
        } else {
            throw new Exception("Error parsing server status file (unexpectedly ended): "
                    + _file.getPath());
        }
    }

}
