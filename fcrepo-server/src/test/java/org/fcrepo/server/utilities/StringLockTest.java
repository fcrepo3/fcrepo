package org.fcrepo.server.utilities;


import org.junit.*;
import static org.junit.Assert.*;


/**
 * This is a darn difficult class to test.  The strategy for the test
 * will be to do a blackbox-test based on the documentation/interface,
 * preferably using sequential tests. It may prove necessary to use
 * some threading to ensure correctness of locking between threads.
 * 
 * It should be noted, that these tests are nowhere complete, since no tests
 * for race-conditions are performed.
 */
public class StringLockTest 
{
    @Test( expected=IllegalMonitorStateException.class )
    public void testUnlockWithoutLock()
    {
	StringLock sl = new StringLock();
	sl.unlock("id_1");
    }


    @Test( expected=IllegalArgumentException.class )
    public void testLockWithNULL()
    {
	StringLock sl = new StringLock();
	sl.lock(null);
    }


    /**
     *  The following tests are performed sequentially if the
     *  name of the function is ending with "Seq".
     */ 

    @Test
    public void testSimpleLockUnlockSeq()
    {
	StringLock sl = new StringLock();
	sl.lock("id_1");
	sl.unlock("id_1");
    }


    @Test
    public void testReentrancyOfLockSeq()
    {
	StringLock sl = new StringLock();
	sl.lock("id_1");
	sl.lock("id_1");
	sl.unlock("id_1");
	sl.unlock("id_1");
    }


    @Test
    public void testRepeatOfLockSeq()
    {
	StringLock sl = new StringLock();
	sl.lock("id_1");
	sl.unlock("id_1");
	sl.lock("id_1");
	sl.unlock("id_1");
    }


    @Test
    public void testTwoLocksSeq()
    {
	StringLock sl = new StringLock();
	sl.lock("id_1");
	sl.lock("id_2");
	sl.unlock("id_1");
	sl.unlock("id_2");
    }


    @Test
    public void testTwoLocksReverseUnlockSeq()
    {
	StringLock sl = new StringLock();
	sl.lock("id_1");
	sl.lock("id_2");
	sl.unlock("id_2");
	sl.unlock("id_1");
    }

    /**
     * The following tests are concurrency tests.  The names end with
     * "Concurrent".  Normally it is undesired to have threads in
     * unittests, but this is not a normal class.
     * BTW: Errors in the code were found based on these unittests!
     */ 

    /**
     * In this test the following should happen:
     * CurrentThread: lock( "id_1" )
     * OtherThread:   lock( "id_2" )
     * OtherThread:   unlock( "id_2" )
     * CurrentThread: unlock( "id_1" )
     */ 
    @Test
    public void testTwoLocksConcurrent() throws InterruptedException
    {
	final StringLock sl1 = new StringLock(); // needs unique name

	// Creating inner class with thread for test
	class MyThread extends Thread
	{
	    public void run() 
	    {
		sl1.lock( "id_2" );
		sl1.unlock( "id_2" );
	    }
	}

	sl1.lock( "id_1" );
	MyThread t = new MyThread();
	t.start();
	t.join(); // waiting for other thread to finish
	sl1.unlock( "id_1" );
    }


    /**
     * In this test the following should happen:
     * OtherThread:   lock( "id_1" )
     * CurrentThread: unlock( "id_1" ) - Exception
     *
     * The unlock should throw an IllegalMonitorStateException since
     * CurrentThread does not hold the lock for "id_1".
     */ 
    @Test( expected=IllegalMonitorStateException.class )
    public void testLockInOneThreadUnlockInAnotherThreadConcurrent() throws InterruptedException
    {
	final StringLock sl2 = new StringLock(); // needs unique name

	// Creating inner class with thread for test
	class MyThread extends Thread
	{
	    public void run() 
	    {
		sl2.lock( "id_1" );
	    }
	}

	MyThread t = new MyThread();
	t.start(); // starting the other thread to get the lock
	t.join(); // wait for the other thread to finish
	sl2.unlock( "id_1" ); // unlock in current thread; this should throw an exception. 
    }

    /**
     * In this test the following should happen:
     *
     * Thread 1 ask for and aquire lock on "id_1"
     * Thread 2 ask for lock on "id_1"
     * Thread 1 releases lock on "id_1"
     * Thread 2 aquire lock on "id_1"
     * Thread 2 releases lock on "id_1"
     *
     * This test is implemented using two threads in order to ensure
     * readability. Thread 1 could be the current thread, but I belive
     * that would clutter the code.
     */
    @Test
    public void testT1LockT2LockT1UnlockT2UnlockSameLockConcurrent() throws InterruptedException
    {
	final StringLock sl3 = new StringLock(); // needs unique name

	// Creating inner class with thread for test.
	// This class has methods for locking and unlocking on 
	// the identifier "id_1":
	class MyThread extends Thread
	{
	    public void lock() { sl3.lock( "id_1" ); }
	    public void unlock() { sl3.unlock( "id_1" ); }

	    public void run() 
	    {
	    }
	}

	MyThread t1 = new MyThread();
	MyThread t2 = new MyThread();
	t1.start(); // starting thread 1
	t2.start(); // starting thread 2

	t1.lock();
	t2.lock();
	t1.unlock();
	t2.unlock();
    }

}