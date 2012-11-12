package org.fcrepo.server.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class needed for synchronisation of access to modification of
 * digital objects represented by a PID (String) To avoid the throwing
 * of an ObjectLockedException when there were multiple attempts to
 * modify a DO we use this class to lock on the pids.  The Map lockMap
 * has a PID as value and an object consisting of a lock, a counter
 * and an owner. The counter is used to maintain the map so we know
 * when a lock is no longer in use and can be deleted
 */
public class StringLock {

    private static final Logger logger = LoggerFactory.getLogger(StringLock.class);

    // This is the object on which we synchronize.
    private Map< String, LockAdmin > lockMap;

    public StringLock() {
	lockMap = new HashMap< String, LockAdmin >();
	logger.info( "StringLock constructed" );
    }

    /**
     * Gets the Lock for the String pid, or waits til the Lock is available.
     * @throws IllegalArgumentException if pid is null
     */
    public void lock( String pid ) {
	if( pid == null ) {
	    throw new IllegalArgumentException("pid cannot be null");
	}

	ReentrantLock pidLock= null;
	LockAdmin lockAdm = null;
	synchronized(lockMap) {
	    lockAdm = lockMap.get( pid );
	    if( lockAdm == null ) {
		lockAdm = new LockAdmin();
		lockMap.put( pid, lockAdm );
	    }
	    lockAdm.increaseCounter();
	    pidLock = lockAdm.getLock();
	}
	pidLock.lock();
	lockAdm.setOwner( Thread.currentThread().getId() );
    }

    
    /**
     * Unlock the Lock for String pid.
     * @throws IllegalMonitorStateException if either the pid is not
     * currently locked, or if the current thread is not owner of the
     * Lock for pid.
     */
    public void unlock( String pid )
    {
	ReentrantLock pidLock = null;

	synchronized(lockMap) {
	    LockAdmin lockAdm = lockMap.get( pid );
	    if( lockAdm == null ) {
		throw new IllegalMonitorStateException( String.format( "Unlock called and no LockAdmin corresponding to the pid: '%s' found in the lockMap", pid ) );
	    }

	    if( lockAdm.getOwner() != Thread.currentThread().getId() ) {
		throw new IllegalMonitorStateException( String.format( "Unlock called by thread: '%s' but lock for pid: '%s' is owned by thread: '%s'", Thread.currentThread().getId(), pid, lockAdm.getOwner() ) );
	    }
                
	    lockAdm.getLock().unlock();
	    lockAdm.decreaseCounter();
	    
	    if( lockAdm.counterIsZero() ) {
		lockMap.remove( pid );
	    }
	}
    }


    /**
     * Inner class used by StringLock It maintains a lock, a counter
     * and an owner. This class should not be exposed outside of
     * StringLock.
     */
    private class LockAdmin {
        private ReentrantLock lock;
        private int counter;
        private long owner;

        LockAdmin() {
            this.lock = new ReentrantLock();
            this.counter = 0;
            this.owner = 0L;
        }

        /**
         *  Retrieves the lock.
         *  @return ReentrantLock The lock.
         */
        ReentrantLock getLock() {
            return lock;
        }

        /**
         *  Check if the counter is 0.
         *  @return boolean true if counter is 0. False otherwise.
         */
        boolean counterIsZero() {
            if( counter == 0 ) {
		return true;
	    }
            return false;
        }

        /**
         * decreases the counter part with 1
         * @throws IllegalStateException if the counter is attempted to 
         * be decreased below zero
         */
        void decreaseCounter() throws IllegalStateException {
            if( counter == 0 ) {
		String msg = "counter is decreased below zero!!" ;
		logger.error( msg );
		throw new IllegalStateException( msg );
	    }
            counter--;
        }

        /**
         * increases the counter part with 1
         */
        void increaseCounter() {
            counter++;
        }

        /**
         * sets the owner of the lock
         */
        void setOwner( long id ) {
            owner = id;
        }

        /**
         * gets the owner of the lock
         */
        long getOwner() {
            return owner;
        }

        /**
         *  A string representation containing the following:
         *  A string representation of the lock, the value of the counter and the 
         *  value of the threadId of the thread currently holding the lock
         *  @return a String representation of the object
         */
        @Override
        public String toString() {
            return String.format( "StringLock< %s, %s, %s >", lock.toString(), counter, owner );
        }

        /**
         *  Returns a unique hashcode for the specific combination 
         *  of elements in this Lock
         */
        @Override
        public int hashCode() {
            return lock.hashCode() ^ counter ^ (int)owner;
        }
    }
}
