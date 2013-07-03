package org.fcrepo.server.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class needed for synchronisation of access to modification of
 * digital objects represented by a PID (String) To avoid the throwing
 * of an ObjectLockedException when there were multiple attempts to
 * modify a DO we use this class to lock on the pids.  The Map lockMap
 * has a PID as keys and ReentrantLocks as objects. When a lock is
 * released with no queued threads it can be deleted.
 */
public class StringLock {

    private static final Logger logger = LoggerFactory.getLogger(StringLock.class);

    // This is the object on which we synchronize.
    private Map< String, ReentrantLock > lockMap;

    public StringLock() {
	    lockMap = new HashMap< String, ReentrantLock >();
	    logger.debug( "StringLock constructed" );
    }

    /**
     * Gets the Lock for the String pid, or waits til the Lock is available.
     * @throws IllegalArgumentException if pid is null
     */
    public void lock( String pid ) {
		if( pid == null ) {
		    throw new IllegalArgumentException("pid cannot be null");
		}
	
		ReentrantLock lockAdm = null;
		synchronized(lockMap) {
		    lockAdm = lockMap.get( pid );
		    if( lockAdm == null ) {
			    lockAdm = new ReentrantLock();
			    lockMap.put( pid, lockAdm );
		    }
		}
		lockAdm.lock();
    }

    
    /**
     * Unlock the Lock for String pid.
     * @throws IllegalMonitorStateException if either the pid is not
     * currently locked, or if the current thread is not owner of the
     * Lock for pid.
     */
    public void unlock( String pid ) {

	    synchronized(lockMap) {
	    	ReentrantLock lockAdm = lockMap.get( pid );
		    if( lockAdm == null ) {
			    throw new IllegalMonitorStateException( String.format( "Unlock called but no lock for the pid: '%s' found in the lockMap", pid ) );
		    }
	
		    if( !lockAdm.hasQueuedThreads() && lockAdm.getHoldCount() == 1) {
			    lockMap.remove( pid );
	        }
		    lockAdm.unlock();
	    }
    }

}
