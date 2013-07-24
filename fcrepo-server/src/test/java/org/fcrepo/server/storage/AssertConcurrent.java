/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */

package org.fcrepo.server.storage;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 * A method for simultaneously executing Runnables and ensuring they exit normally
 * Implementation from:
 * http://www.planetgeek.ch/2009/08/25/how-to-find-a-concurrency-bug-with-java/
 * @author ba2213
 *
 */
public abstract class AssertConcurrent {
    public static void assertConcurrent(final String message, final int maxTimeoutSeconds, Runnable... runnables) throws InterruptedException {
    	final int numThreads = runnables.length;
    	final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
    	final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
    	try {
    		final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
    		final CountDownLatch afterInitBlocker = new CountDownLatch(1);
    		final CountDownLatch allDone = new CountDownLatch(numThreads);
    		for (final Runnable submittedTestRunnable : runnables) {
    			threadPool.submit(new Runnable() {
    				public void run() {
    					allExecutorThreadsReady.countDown();
    					try {
    						afterInitBlocker.await();
    						submittedTestRunnable.run();
    					} catch (final Throwable e) {
    						exceptions.add(e);
    					} finally {
    						allDone.countDown();
    					}
    				}
    			});
    		}
    		// wait until all threads are ready
    		assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.length * 10, TimeUnit.MILLISECONDS));
    		// start all test runners
    		afterInitBlocker.countDown();
    		assertTrue(message +" timeout! More than " + maxTimeoutSeconds + " seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
    	} finally {
    		threadPool.shutdownNow();
    	}
    	assertTrue(message + " failed with exception(s) " + exceptions, exceptions.isEmpty());
    }
}
