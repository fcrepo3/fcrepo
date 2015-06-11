package org.fcrepo.utilities;

public class TimestampedCacheEntry<T> {
	private long timeStamp;
	private final T object;

	public TimestampedCacheEntry(final T object) {
		this(System.currentTimeMillis(), object);
	}

    public TimestampedCacheEntry(final long timeStamp, final T object) {
        super();
        this.timeStamp = timeStamp;
        this.object = object;
    }

    public TimestampedCacheEntry<T> refresh() {
        this.timeStamp = System.currentTimeMillis();
		return this;
	}
	
	public long timestamp() {
	    return this.timeStamp;
	}
	
	/**
	 * Calculate the age since the object was created (or claimed creation)
	 * and the Current system time
     * @return long time since documented creation
	 */
	public long age() {
	    return System.currentTimeMillis() - this.timeStamp;
	}
	
	/**
	 * Calculate the age since the object was created (or claimed creation)
	 * and the passed time as a long. Useful for loop processing of entries.
	 * @param time
	 * @return long time since documented creation
	 */
    public long ageAt(long time) {
        return time - this.timeStamp;
    }

    public T value() {
	    return this.object;
	}

}