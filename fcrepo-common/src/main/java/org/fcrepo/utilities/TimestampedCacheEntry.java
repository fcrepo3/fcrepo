package org.fcrepo.utilities;

public class TimestampedCacheEntry<T> {
	private long timeStamp;
	private final T object;

	public TimestampedCacheEntry(final T object) {
		this(System.currentTimeMillis(), object);
	}

    public TimestampedCacheEntry(final long timeStamp, final T object) {
        super();
        this.timeStamp = System.currentTimeMillis();
        this.object = object;
    }

    public TimestampedCacheEntry<T> refresh() {
        this.timeStamp = System.currentTimeMillis();
		return this;
	}
	
	public long timestamp() {
	    return this.timeStamp;
	}
	
	public long age() {
	    return System.currentTimeMillis() - this.timeStamp;
	}
	
	public T value() {
	    return this.object;
	}

}