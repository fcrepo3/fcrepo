/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.client;

import java.awt.Dimension;
import java.util.Map;

import javax.swing.SwingUtilities;

/**
 * This is the 3rd version of SwingWorker (also known as SwingWorker 3), an
 * abstract class that you subclass to perform GUI-related work in a dedicated
 * thread. For instructions on and examples of using this class, see:
 * http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html Note that
 * the API changed slightly in the 3rd version: You must now invoke start() on
 * the SwingWorker after creating it.
 */
public abstract class SwingWorker<T> {

    private T value; // see getValue(), setValue()

    public boolean done; // set to true only upon finish

    public Exception thrownException;

    /**
     * Class to maintain reference to current worker thread under separate
     * synchronization control.
     */
    private static class ThreadVar {

        private Thread thread;

        ThreadVar(Thread t) {
            thread = t;
        }

        synchronized Thread get() {
            return thread;
        }

        synchronized void clear() {
            thread = null;
        }
    }

    private final ThreadVar threadVar;

    /**
     * Get the value produced by the worker thread, or null if it hasn't been
     * constructed yet.
     */
    protected synchronized T getValue() {
        return value;
    }

    /**
     * Set the value produced by worker thread
     */
    public synchronized void setValue(T x) {
        value = x;
    }

    /**
     * Compute the value to be returned by the <code>get</code> method.
     */
    public abstract T construct();

    /**
     * Called on the event dispatching thread (not on the worker thread) after
     * the <code>construct</code> method has returned.
     */
    public void finished() {
        done = true;
    }

    /**
     * A new method that interrupts the worker thread. Call this method to force
     * the worker to stop what it's doing.
     */
    public void interrupt() {
        Thread t = threadVar.get();
        if (t != null) {
            t.interrupt();
        }
        threadVar.clear();
    }

    /**
     * Return the value created by the <code>construct</code> method. Returns
     * null if either the constructing thread or the current thread was
     * interrupted before a value was produced.
     * 
     * @return the value created by the <code>construct</code> method
     */
    public T get() {
        while (true) {
            Thread t = threadVar.get();
            if (t == null) {
                return getValue();
            }
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // propagate
                return null;
            }
        }
    }

    public Map<?,?> parms;

    /**
     * Start a thread that will call the <code>construct</code> method and
     * then exit.
     */
    public SwingWorker() {
        final Runnable doFinished = new Runnable() {

            public void run() {
                finished();
            }
        };

        Runnable doConstruct = new Runnable() {

            public void run() {
                try {
                    setValue(construct());
                } finally {
                    threadVar.clear();
                }
                done = true;
                SwingUtilities.invokeLater(doFinished);
            }
        };

        Thread t = new Thread(doConstruct);
        threadVar = new ThreadVar(t);
    }
    
    public SwingWorker(Map<?,?> parms) {
        this();
        this.parms = parms;
    }

    /**
     * Start the worker thread.
     */
    public void start() {
        Thread t = threadVar.get();
        if (t != null) {
            t.start();
        }
    }

    public static <T> T waitForResult(SwingWorker<T> worker, String msg) {
        worker.start();
        // The following code will run in the (safe)
        // Swing event dispatcher thread.
        int ms = 0;
        Dimension d = Administrator.PROGRESS.getSize();
        Administrator.PROGRESS.setString(msg + ". . .");
        while (!worker.done) {
            try {
                Administrator.PROGRESS.setValue(ms);
                Administrator.PROGRESS
                        .paintImmediately(0,
                                          0,
                                          (int) d.getWidth() - 1,
                                          (int) d.getHeight() - 1);
                Thread.sleep(100);
                ms = ms + 100;
                if (ms >= 2000) ms = 200;
            } catch (InterruptedException ie) {
            }
        }
        Administrator.PROGRESS.setValue(2000);
        Administrator.PROGRESS.paintImmediately(0,
                                                0,
                                                (int) d.getWidth() - 1,
                                                (int) d.getHeight() - 1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
        Administrator.PROGRESS.setValue(0);
        Administrator.PROGRESS.setString("");
        // Otherwise, get the value from the
        // worker (returning it if applicable)
        return worker.get();
    }
}
