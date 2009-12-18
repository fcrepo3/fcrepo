/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.utilities;

import org.apache.log4j.Logger;

/**
 * Implementation of ThreadMonitor.
 * 
 * @author Chris Wilper
 */
public class ThreadMonitorImpl
        implements ThreadMonitor {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(ThreadMonitorImpl.class.getName());

    private boolean m_stopRequested;

    private boolean m_onlyMemory;

    private int m_pollInterval;

    public ThreadMonitorImpl(int pollInterval, boolean onlyMemory) {
        m_onlyMemory = onlyMemory;
        if (pollInterval >= 0) {
            m_pollInterval = pollInterval;
            Thread t = new Thread(this, "ThreadMonitor");
            t.start();
        }
    }

    public void run() {
        while (!m_stopRequested) {
            try {
                Thread.sleep(m_pollInterval);
            } catch (InterruptedException ie) {
            }
            LOG.info("Available Memory: " + Runtime.getRuntime().freeMemory());
            if (!m_onlyMemory) {
                LOG.info(getThreadTree());
            }
        }
    }

    public void requestStop() {
        m_stopRequested = true;
    }

    public static String getThreadTree() {
        ThreadGroup current, root, parent;
        current = Thread.currentThread().getThreadGroup();
        root = current;
        parent = root.getParent();
        while (parent != null) {
            root = parent;
            parent = parent.getParent();
        }
        StringBuffer out = new StringBuffer();
        appendGroup(root, "", out);
        return out.toString();
    }

    private static void appendGroup(ThreadGroup g,
                                    String indent,
                                    StringBuffer out) {
        if (g != null) {
            int tc = g.activeCount();
            int gc = g.activeGroupCount();
            Thread[] threads = new Thread[tc];
            ThreadGroup[] groups = new ThreadGroup[gc];
            g.enumerate(threads, false);
            g.enumerate(groups, false);
            out.append(indent + "Group: " + g.getName() + " MaxPriority: "
                    + g.getMaxPriority() + (g.isDaemon() ? " DAEMON" : "")
                    + "\n");
            for (int i = 0; i < tc; i++) {
                appendThread(threads[i], indent + "    ", out);
            }
            for (int i = 0; i < gc; i++) {
                appendGroup(groups[i], indent + "    ", out);
            }
        }
    }

    private static void appendThread(Thread t, String indent, StringBuffer out) {
        if (t == null) {
            return;
        }
        out.append(indent + "Thread: " + t.getName() + "  Priority: "
                + t.getPriority() + (t.isDaemon() ? " DAEMON" : "")
                + (t.isAlive() ? "" : " NOT ALIVE") + "\n");
    }

}
