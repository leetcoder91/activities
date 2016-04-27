package com.activities.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The PriorityThreadPool allows scheduling of tasks with a defined priority level.
 */
public class PriorityThreadPool extends ResizableThreadPoolExecutor {

private static final int DEFAULT_PRIORITY = 0;
private final AtomicLong PRIORITY_SEQUENCE = new AtomicLong();

public PriorityThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
	super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new PriorityBlockingQueue<Runnable>(), threadFactory);
}

/**
 * Submits a task with the default priority
 * @param task the task to submit
 * @return a Future representing pending completion of the task
 */
@Override
public Future<?> submit(Runnable task) {
	return submit(task, DEFAULT_PRIORITY);
}

/**
 * Submits a task with the declared priority
 * @param task the task to submit
 * @param priority for executing this task
 * @return a Future representing pending completion of the task
 */
public Future<?> submit(Runnable task, int priority) {
	if (task == null) throw new NullPointerException();

	RunnableFuture<Object> ftask =
			new PrioritizedFutureTask<Object>(task, null, priority, PRIORITY_SEQUENCE.incrementAndGet());
	execute(ftask);
	return ftask;
}

/**
 * Submits a Runnable task for execution and returns a Future
 * representing that task with a default priority
 *
 * The Future's <tt>get</tt> method will
 * return the given result upon successful completion.
 */
@Override
public <T> Future<T> submit(Runnable task, T result) {
	return submit(task, result, DEFAULT_PRIORITY);
}

/**
 * Submits a Runnable task for execution and returns a Future
 * representing that task with declared priority
 *
 * The Future's <tt>get</tt> method will
 * return the given result upon successful completion.
 *
 * @param task the task to submit
 * @param result the result to return
 * @param priority for executing this task
 *
 * @return a Future representing pending completion of the task
 */
public <T> Future<T> submit(Runnable task, T result, int priority) {
	if (task == null) throw new NullPointerException();

	RunnableFuture<T> ftask =
			new PrioritizedFutureTask<T>(task, result, priority, PRIORITY_SEQUENCE.incrementAndGet());
	execute(ftask);
	return ftask;
}

/**
 * Submits a task with the default priority
 * @param task the task to submit
 * @return a Future representing pending completion of the task
 */

@Override
public <T> Future<T> submit(Callable<T> task) {
	return submit(task, DEFAULT_PRIORITY);
}

/**
 * Submits a task with the declared priority
 * @param task the task to submit
 * @param priority for executing this task
 * @return a Future representing pending completion of the task
 */
public <T> Future<T> submit(Callable<T> task, int priority) {
	if (task == null) throw new NullPointerException();

	final RunnableFuture<T> ftask =
			new PrioritizedFutureTask<T>(task, priority, PRIORITY_SEQUENCE.incrementAndGet());
	execute(ftask);
	return ftask;
}

/**
 * A FutureTask that implements Comparable so that we can sort it within the Queue
 * @param <V>
 */
private static class PrioritizedFutureTask<V> extends FutureTask<V> implements Comparable<PrioritizedFutureTask<V>> {

	private final int mPriority;
	private final long mSequence;

	public PrioritizedFutureTask(Callable<V> vCallable, int priority, long sequence) {
		super(vCallable);
		mPriority = priority;
		mSequence = sequence;
	}

	public PrioritizedFutureTask(Runnable runnable, V result, int priority, long sequence) {
		super(runnable, result);
		mPriority = priority;
		mSequence = sequence;
	}

	@Override
	public int compareTo(PrioritizedFutureTask<V> compare) {
		if (compare == this) { // compare zero ONLY if same object
			return 0;
		}
		
		// make sure to handle negative numbers as well.
		if ( mPriority == compare.mPriority ) {
		    if ( mSequence > compare.mSequence ) {
		        // I came later, so I should sort lower in the queue
		        return 1;
		    }
		    return -1;
		}
		else if ( mPriority > compare.mPriority ) {
		    // I'm more important, so I should higher in the queue
		    return 1;
		}
		
		return -1;
		
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		PrioritizedFutureTask that = (PrioritizedFutureTask) o;

		return mPriority == that.mPriority && mSequence == that.mSequence;
	}

	@Override
	public int hashCode() {
		int result = mPriority;
		result = 31 * result + (int) (mSequence ^ (mSequence >>> 32));
		return result;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PrioritizedFutureTask [mPriority=" + mPriority + ", mSequence=" + mSequence + "]";
    }
}

}
