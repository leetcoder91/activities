package com.activities.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A ThreadPoolExecutor that prefers to resize the pool by adding new threads
 * before queuing tasks. No more that {@link ThreadPoolExecutor#getCorePoolSize()}
 * threads will ever be active at one time.  
 */
public class ResizableThreadPoolExecutor extends ThreadPoolExecutor {
	/**
	 * Lock held while messing around with the core pool size in the superclass.
	 */
	private final ReentrantLock CORE_LOCK = new ReentrantLock();
	
	private int mActualCorePoolSize;
	
	/**
	 * Creates a new <tt>ThreadPoolExecutor</tt> with the given
	 * initial parameters and default thread factory and handler.  It
	 * may be more convenient to use one of the {@link Executors}
	 * factory methods instead of this general purpose constructor.
	 *
	 * @param corePoolSize the number of threads to keep in the
	 * pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the
	 * pool.
	 * @param keepAliveTime when the number of threads is greater than
	 * the core, this is the maximum time that excess idle threads
	 * will wait for new tasks before terminating.
	 * @param unit the time unit for the keepAliveTime
	 * argument.
	 * @param workQueue the queue to use for holding tasks before they
	 * are executed. This queue will hold only the <tt>Runnable</tt>
	 * tasks submitted by the <tt>execute</tt> method.
	 * @throws IllegalArgumentException if corePoolSize, or
	 * keepAliveTime less than zero, or if maximumPoolSize less than or
	 * equal to zero, or if corePoolSize greater than maximumPoolSize.
	 * @throws NullPointerException if <tt>workQueue</tt> is null
	 */
	public ResizableThreadPoolExecutor(int corePoolSize,
	                          int maximumPoolSize,
	                          long keepAliveTime,
	                          TimeUnit unit,
	                          BlockingQueue<Runnable> workQueue) {
	    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	    mActualCorePoolSize = corePoolSize;
	}
	
	/**
	 * Creates a new <tt>ThreadPoolExecutor</tt> with the given initial
	 * parameters.
	 *
	 * @param corePoolSize the number of threads to keep in the
	 * pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the
	 * pool.
	 * @param keepAliveTime when the number of threads is greater than
	 * the core, this is the maximum time that excess idle threads
	 * will wait for new tasks before terminating.
	 * @param unit the time unit for the keepAliveTime
	 * argument.
	 * @param workQueue the queue to use for holding tasks before they
	 * are executed. This queue will hold only the <tt>Runnable</tt>
	 * tasks submitted by the <tt>execute</tt> method.
	 * @param threadFactory the factory to use when the executor
	 * creates a new thread.
	 * @throws IllegalArgumentException if corePoolSize, or
	 * keepAliveTime less than zero, or if maximumPoolSize less than or
	 * equal to zero, or if corePoolSize greater than maximumPoolSize.
	 * @throws NullPointerException if <tt>workQueue</tt>
	 * or <tt>threadFactory</tt> are null.
	 */
	public ResizableThreadPoolExecutor(int corePoolSize,
	                          int maximumPoolSize,
	                          long keepAliveTime,
	                          TimeUnit unit,
	                          BlockingQueue<Runnable> workQueue,
	                          ThreadFactory threadFactory) {
	    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	    mActualCorePoolSize = corePoolSize;
	}
	
	/**
	 * Creates a new <tt>ThreadPoolExecutor</tt> with the given initial
	 * parameters.
	 *
	 * @param corePoolSize the number of threads to keep in the
	 * pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the
	 * pool.
	 * @param keepAliveTime when the number of threads is greater than
	 * the core, this is the maximum time that excess idle threads
	 * will wait for new tasks before terminating.
	 * @param unit the time unit for the keepAliveTime
	 * argument.
	 * @param workQueue the queue to use for holding tasks before they
	 * are executed. This queue will hold only the <tt>Runnable</tt>
	 * tasks submitted by the <tt>execute</tt> method.
	 * @param handler the handler to use when execution is blocked
	 * because the thread bounds and queue capacities are reached.
	 * @throws IllegalArgumentException if corePoolSize, or
	 * keepAliveTime less than zero, or if maximumPoolSize less than or
	 * equal to zero, or if corePoolSize greater than maximumPoolSize.
	 * @throws NullPointerException if <tt>workQueue</tt>
	 * or  <tt>handler</tt> are null.
	 */
	public ResizableThreadPoolExecutor(int corePoolSize,
	                          int maximumPoolSize,
	                          long keepAliveTime,
	                          TimeUnit unit,
	                          BlockingQueue<Runnable> workQueue,
	                          RejectedExecutionHandler handler) {
	    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	    mActualCorePoolSize = corePoolSize;
	}
	
	/**
	 * Creates a new <tt>ThreadPoolExecutor</tt> with the given initial
	 * parameters.
	 *
	 * @param corePoolSize the number of threads to keep in the
	 * pool, even if they are idle.
	 * @param maximumPoolSize the maximum number of threads to allow in the
	 * pool.
	 * @param keepAliveTime when the number of threads is greater than
	 * the core, this is the maximum time that excess idle threads
	 * will wait for new tasks before terminating.
	 * @param unit the time unit for the keepAliveTime
	 * argument.
	 * @param workQueue the queue to use for holding tasks before they
	 * are executed. This queue will hold only the <tt>Runnable</tt>
	 * tasks submitted by the <tt>execute</tt> method.
	 * @param threadFactory the factory to use when the executor
	 * creates a new thread.
	 * @param handler the handler to use when execution is blocked
	 * because the thread bounds and queue capacities are reached.
	 * @throws IllegalArgumentException if corePoolSize, or
	 * keepAliveTime less than zero, or if maximumPoolSize less than or
	 * equal to zero, or if corePoolSize greater than maximumPoolSize.
	 * @throws NullPointerException if <tt>workQueue</tt>
	 * or <tt>threadFactory</tt> or <tt>handler</tt> are null.
	 */
	public ResizableThreadPoolExecutor(int corePoolSize,
	                          int maximumPoolSize,
	                          long keepAliveTime,
	                          TimeUnit unit,
	                          BlockingQueue<Runnable> workQueue,
	                          ThreadFactory threadFactory,
	                          RejectedExecutionHandler handler) {
	    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	    mActualCorePoolSize = corePoolSize;
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	public void setCorePoolSize(int corePoolSize) {
	    final ReentrantLock coreLock = CORE_LOCK;
	    coreLock.lock();
	    try {
	        mActualCorePoolSize = corePoolSize;
	        super.setCorePoolSize(corePoolSize);
	    }
	    finally {
	        coreLock.unlock();
	    }
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
	 */
	public void execute(Runnable command) {
	    if (command == null) {
	        throw new NullPointerException();
	    }
	    
	    if ( isShutdown() ) {
	        getRejectedExecutionHandler().rejectedExecution(command, this);
	        return;
	    }
	    
	    // Case 1: less that corePoolSize threads running. Super class does the
	    //         right thing and creates a new thread for this task.
	    if ( getPoolSize() < getCorePoolSize() ) {  
	        super.execute(command);
	        return;
	    }
	    
	    // Case 2: less that maxPoolSize threads running. Super class does the
	    //         **wrong** thing and queues the request. We want to create a 
	    //         new thread. However, the methods we want to call in the super
	    //         class are private so we force the behavior we want using the
	    //         public methods. Basically we coerce the super class into
	    //         believing that the requirements for "Case 1" have been met. 
	    if ( getPoolSize() < getMaximumPoolSize() ) {
	        final ReentrantLock coreLock = CORE_LOCK;
	        coreLock.lock();
	        try {
	            super.setCorePoolSize(getPoolSize() + 1);
	            super.execute(command);
	            super.setCorePoolSize(mActualCorePoolSize);
	        }
	        finally {
	            coreLock.unlock();
	        }
	        return;
	    }
	    
	    // Case 3: all threads busy, try and queue the task
	    if ( ! getQueue().offer(command) ) {            
	        getRejectedExecutionHandler().rejectedExecution(command, this);
	        return;
	    }
	        
	}
}
