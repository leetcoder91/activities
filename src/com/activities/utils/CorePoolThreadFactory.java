package com.activities.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory to create the threads for a thread pool whose parent thread group is the 
 * main <strong>FglAM</strong> thread group. It ensures that all the threads share a 
 * common naming pattern and thread group. By default the factory creates daemon threads 
 * unless specified otherwise.
 * </p>
 * The names of created threads are ensured to be unique even if different thread factories use the same group name.
 */
public class CorePoolThreadFactory implements ThreadFactory {

	private final String mPoolName;
	private final ThreadGroup mThreadGroup;
	private final int mPriority;
	private final AtomicLong mThreadNumber = new AtomicLong(0);
	private final boolean mDaemon;
	private final long mFactoryId;
	
	private static ThreadGroup sFglAMThreadGroup = null;
	
	public static final String THREAD_GROUP_NAME = "FglAM";
	
	private static final AtomicLong THREAD_FACTORY_ID = new AtomicLong(0L);
	
	/**
	 * Creates the pool thread factory that creates daemon threads with the name of the thread 
	 * pool specified.
	 *
	 * @param poolName the name of the thread pool to create
	 */
	public CorePoolThreadFactory(String poolName) {
	    this(poolName, Thread.NORM_PRIORITY);
	}    
	
	/**
	 * Creates the pool thread factory that creates daemon threads with the specified thread pool 
	 * name, and the priority for the created threads.
	 *
	 * @param poolName the name of the thread pool
	 * @param priority the priority to set the created threads to
	 */
	public CorePoolThreadFactory(String poolName, int priority) {
	    this(poolName, priority, true);
	}
	
	/**
	 * Creates the pool thread factory that with the specified thread pool name, and the priority 
	 * for the created threads.
	 *
	 * @param poolName the name of the thread pool
	 * @param priority the priority to set the created threads to
	 * @param daemon true if threads created should be daemon threads, false otherwise
	 */
	public CorePoolThreadFactory(String poolName, int priority, boolean daemon) {
	    mPoolName = poolName;
	    mThreadGroup = new ThreadGroup(getFglAMThreadGroup(), poolName);
	    mPriority = priority;
	    mDaemon = daemon;
	    mFactoryId = THREAD_FACTORY_ID.getAndIncrement();
	}
	
	/**
	 * Cleans up the thread factory, removing the thread group, if possible.
	 */
	public void shutdown() {
	    ThreadUtil.destroyThreadGroup(mThreadGroup);
	}
	
	@Override
	public synchronized Thread newThread(Runnable r) {
	    if ( mThreadGroup.isDestroyed() ) {
	        throw new IllegalThreadStateException("Cannot create new threads in " + mPoolName + 
	                                              " because enclosing the thread group was destroyed");
	    }
	    
	    final String name = getNewThreadName();
	    final Thread result = new Thread(mThreadGroup, r, name);
	    result.setPriority(mPriority);
	    result.setDaemon(mDaemon);
	
	    return result;
	}
	
	private String getNewThreadName() {
	    StringBuilder builder = new StringBuilder(mPoolName);
	    builder.append('[').append(mFactoryId).append(']');
	    builder.append("-");
	    builder.append(mThreadNumber.getAndIncrement());
	
	    return builder.toString();
	}
	
	/**
	 * Creates the main/root FglAM thread group.
	 */
	private static ThreadGroup createCoreThreadGroup() {
	    final ThreadGroup mainThreadGroup = ThreadUtil.findMainThreadGroup();
	    
	    // check to see if there is already a FglAM thread group
	    final ThreadGroup existing = ThreadUtil.findChildGroup(mainThreadGroup, THREAD_GROUP_NAME);
	    if ( existing != null ) {
	        return existing;
	    }
	    
	    return new ThreadGroup(mainThreadGroup, THREAD_GROUP_NAME);
	}
	
	/**
	 * Returns the main/root FglAM thread group.
	 *
	 * @return the main/root FglAM thread group
	 */
	private static synchronized ThreadGroup getFglAMThreadGroup() {
	    if( sFglAMThreadGroup == null ) {
	        sFglAMThreadGroup = createCoreThreadGroup();
	    }
	
	    return sFglAMThreadGroup;
	}
	
	
	/**
	 * Destroys the FglAM thread group (and any sub groups) if possible. Once this method is called, 
	 * no further threads will be created.
	 */
	public static synchronized void destroyFglAMRootThreadGroup() {
	    ThreadUtil.destroyThreadGroup(sFglAMThreadGroup);
	    sFglAMThreadGroup = null;
	}
}
