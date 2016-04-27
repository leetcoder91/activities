package com.activities.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Contains utility methods for working with threads and thread groups.
 */
public class ThreadUtil {

private static final Log CAT = LogFactory.getLog(ThreadUtil.class);

/**
 * Finds the root thread group (the one who's parent is null) and returns it. This thread
 * group is usually reserved for JVM system tasks (GC, finalizer, etc).
 * 
 * @see #findMainThreadGroup()
 */
public static ThreadGroup findRootThreadGroup() {
    
    ThreadGroup parent = Thread.currentThread().getThreadGroup();
    ThreadGroup current = parent;
    while (parent != null) {
        current = parent;
        parent = parent.getParent();
    }
    
    return current;    
}

/**
 * Finds the main thread group. The main thread group is the single child thread group
 * of the system thread group and contains all user-related (non-JVM) threads and thread
 * groups. 
 */
public static ThreadGroup findMainThreadGroup() {
    
    ThreadGroup parent = Thread.currentThread().getThreadGroup();
    ThreadGroup main = parent;
    ThreadGroup current = parent;
    
    while (current != null) {
        main = parent;
        parent = current;
        current = current.getParent();
    }
    
    return main;    
}

/**
 * Creates a thread in the main thread group. No other properties (daemon, priority, etc)
 * are changed from the defaults. The main thread group contains all user created threads 
 * and thread groups and is considered the top-level user group.
 * 
 * @param r The {@link Runnable} interface associated with the thread.
 * @param name The name for the thread
 * 
 * @return A new thread in the main thread group.
 */
public static Thread createMainThread(Runnable r, String name) {
    return new Thread(findMainThreadGroup(), r, name);
}

/**
 * Finds and returns the thread group with the provided name that is a direct
 * child of the provided thread group.
 * 
 * @param parent The parent of the group to be searched for. Cannot be <code>null</code>
 * @param childName The name of the child thread group to search for. Cannot be <code>null</code>
 * 
 * @return The named child thread group or <code>null</code> if it could not be located.
 */
public static ThreadGroup findChildGroup(ThreadGroup parent, String childName) {
    
    if ( parent == null ) {
        throw new NullPointerException("Parent thread group cannot be null");
    }
    if ( childName == null ) {
        throw new NullPointerException("Thread group name to seach for cannot be null");
    }
    
    int numGroupsInGroup = parent.activeGroupCount();
    final ThreadGroup[] subGroups = new ThreadGroup[numGroupsInGroup];
    numGroupsInGroup = parent.enumerate(subGroups, false);
    
    for (int i = 0; i < numGroupsInGroup; i++ ) {
        if ( childName.equals(subGroups[i].getName()) ) {
            return subGroups[i];
        }
    }
    
    // not found
    return null;
}

/**
 * Attempts (politely) to destroy the provided thread group. If the group has no child
 * threads, it is destroyed immediately. Otherwise all child threads are interrupted and the
 * destroy is tried again after a small delay. If there are still threads active in the group
 * after the delay, an error is logged and the group is not destroyed. 
 * 
 * @param g The thread group to destroy
 */
public static void destroyThreadGroup(final ThreadGroup g) {
    if ( g.isDestroyed() ) {
        // no-op
        return;
    }
    
    final ThreadGroupKiller killer = new ThreadGroupKiller(g);
    
    if ( g.parentOf( Thread.currentThread().getThreadGroup()) ) {
        // We're in the thread group that needs to be destroyed, move
        // the work to an independent thread.
        final Thread groupKiller = new Thread(findMainThreadGroup(), killer, g.getName() + "Thread Group Shutdown");
        groupKiller.setContextClassLoader(null);
        groupKiller.setDaemon(true);
        groupKiller.start();
        Thread.yield();
    }
    else {
        // we can just run the shutdown code directly.
        killer.run();
    }
}

/**
 * Used to move the work of shutting down a thread group to an independent
 * thread on the main group.
 */
private static final class ThreadGroupKiller implements Runnable {
    private final ThreadGroup mGroupToKill;
    
    private ThreadGroupKiller(ThreadGroup g) {
        mGroupToKill = g;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if ( mGroupToKill.isDestroyed() ) {
            // no-op
            return;
        }
        
        int count = mGroupToKill.activeCount();
        if ( count >= 0 ) {
            // there are still some threads running. Send them a polite interrupt.
            mGroupToKill.interrupt();
        }
        
        // pause for a bit to make sure the threads go away.
        try {
            Thread.sleep(2000);
        }
        catch (InterruptedException e) {
            CAT.warn("Interrupted while waiting for thread in " + 
        		mGroupToKill.getName() + " to shut down", e);
            
            // if we get an interrupt ourselves, the polite thing to do is exit
            // at once, but we only have a little more work to do and killing the
            // thread group could have some weird side effects.
        }
        
        // Check again to see if there are any active threads
        count = mGroupToKill.activeCount();
        if ( count == 0 ) {
            mGroupToKill.destroy();
        }
        else {
            CAT.warn("Thread group " + mGroupToKill.getName() +
        		" could not be destroyed as there are approximately " + count +
        		" threads still active in it.");
        }
    }
}

}
