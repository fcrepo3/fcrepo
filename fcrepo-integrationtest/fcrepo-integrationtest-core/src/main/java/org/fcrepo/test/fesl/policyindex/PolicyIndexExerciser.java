package org.fcrepo.test.fesl.policyindex;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import org.fcrepo.test.fesl.util.AuthorizationDeniedException;
import org.fcrepo.test.fesl.util.HttpUtils;


// FIXME: should spin off readers and updaters into separate classes with the same interface/abstract base class
public class PolicyIndexExerciser
        extends Thread {

    // counts how many exercisers are currently running
    private static int updaterRunningCount = 0;
    private static int readerRunningCount = 0;

    // record if failed, and reason
    private Throwable failure;
    private boolean failed = false;
    private static int updaterPassedCount = 0;
    private static int readerPassedCount = 0;

    private HttpUtils utils = null;
    private HttpUtils adminUtils = null;

    private byte[][] objects = null;
    private String[] pids = null;

    private static String urlA = "/fedora/objects/test:1000002?format=xml";
    private static String urlB = "/fedora/objects/test:1000007?format=xml";

    private String url = "";

    private boolean isReader = false;

    private boolean stopped = false;


    // constructor for an updater
    public PolicyIndexExerciser(String testurl, String testuser, String testpassword, String adminUrl, String adminUser, String adminPassword, String[] testPids) throws Exception {
        utils = new HttpUtils(testurl, testuser, testpassword);
        adminUtils = new HttpUtils(adminUrl, adminUser, adminPassword);
        pids =testPids;

        // construct array of foxml policy objects to test on
        objects = new byte[pids.length][];
        for (int i = 0; i < pids.length; i++) {
            // alternate policy A and policy B
            String policy = (i % 2) == 0 ? "A" : "B";
            objects[i] = PolicyIndexUtils.getPolicyObject(policy, "A", "A", pids[i]);
        }
    }

    // constructor for a reader
    public PolicyIndexExerciser(String testurl, String testuser, String testpassword) throws Exception {
        utils = new HttpUtils(testurl, testuser, testpassword);
        isReader = true;
    }


    @Override
    public void run() {

        if (isReader) {
            runReader();
        } else {
            runUpdater();
        }

    }


    private void runReader() {
        readerStarted();
        try {
            while (!stopped()) {
                // read
                read("A");
                read("B");

            }
            readerPass();

        } catch (Throwable th) {
            failed = true;
            failure = th;
        } finally {
            readerFinished();
        }
    }

    private boolean stopped() {
        synchronized(this) {
            return stopped;
        }
    }
    public void stopit() {
        synchronized(this) {
            stopped = true;
        }
    }

    private void runUpdater() {
        updaterStarted();
        try {
            for (int i = 0; i < pids.length; i++) {
                // read
                read("A");
                read("B");

                // add policy
                add(objects[i]);

                // read
                read("A");
                read("B");

                // delete policy
                delete(pids[i]);

                // read
                read("A");
                read("B");

            }
            updaterPass();

        } catch (Throwable th) {
            failed = true;
            failure = th;
        } finally {
            updaterFinished();
        }

    }

    // access object covered by policy
    // may succeed or fail depending what policies are actually in force, ignore authz failure
    private void read(String policy) throws ClientProtocolException, IOException {

        long startTime = System.nanoTime();
        if (policy.equals("A")) {
            url = urlA;
        } else if (policy.equals("B")) {
            url = urlB;
        }
        try {
            utils.get(url);
            doSleep(startTime, System.nanoTime());
        } catch (AuthorizationDeniedException e) {
            // don't care if access was allowed
        }
    }

    private void add(byte[] object) throws ClientProtocolException, IOException, AuthorizationDeniedException {
        long startTime = System.nanoTime();
        url = "/fedora/objects/new";
        adminUtils.post(url, null, object);
        doSleep(startTime, System.nanoTime());

    }
    private void delete(String pid) throws ClientProtocolException, IOException, AuthorizationDeniedException {
        long startTime = System.nanoTime();
        url = "/fedora/objects/" + pid;
        adminUtils.delete(url, null);
        doSleep(startTime, System.nanoTime());
    }

    private static void doSleep(long startTime, long endTime) {
        try {
            sleep((endTime - startTime)/2000000); // wait for half the operation's execution time
        } catch (InterruptedException e) {
            // should not happen
            throw new RuntimeException("Sleep failed - " + e.getMessage(), e);
        }
    }

    public static synchronized int updaterRunningCount() {
        return updaterRunningCount;
    }


    private static synchronized void updaterFinished() {
        updaterRunningCount--;
    }
    private static synchronized void updaterStarted() {
        updaterRunningCount++;
    }


    private static synchronized void updaterPass() {
        updaterPassedCount++;
    }
    public static synchronized int updaterPassedCount() {
        return updaterPassedCount;
    }

    public static synchronized int readerRunningCount() {
        return readerRunningCount;
    }


    private static synchronized void readerFinished() {
        readerRunningCount--;
    }
    private static synchronized void readerStarted() {
        readerRunningCount++;
    }

    private static synchronized void readerPass() {
        readerPassedCount++;
    }
    public static synchronized int readerPassedCount() {
        return readerPassedCount;
    }


    public Throwable failure() {
        return failure;
    }
    public boolean failed() {
        return failed;
    }
    public String lastUrl() {
        return url;
    }
}
