package com.activities.api;

import java.util.Iterator;

/**
 * An activity group contains one or more actions that it executes
 * when the activity service executes the group. Activities inside the
 * group and include inter-dependencies and these will be taken into
 * account when the group is executed.
 * <p/>
 * Groups can be single- or multi-threaded, but will share the same limited
 * thread pool as the {@link com.quest.glue.api.services.activities.ActivityService2}.
 * <p/>
 */
public interface ActivityGroup extends Activity {
	/**
	 * Adds one or more activities to the group.
	 */
	public void add(Activity... activities);
	
	/**
	 * Removes one or more activities from the group.
	 */
	public void remove(Activity... activities);
	
	/**
	 * Removes all activities from the group.
	 */
	public void removeAll();
	
	/**
	 * By default, groups execute their contained activities on a single
	 * threaded, in sequence. This can be changed to use the same pool of
	 * threads that the service allocates to all activities.
	 *
	 * @param multithreaded true if the activities contained in this group should
	 *                 be executed in parallel and false if they should be
	 *                 executed in sequence.
	 */
	public void setIsMultiThreaded(boolean multithreaded);
	
	/**
	 * @return Iterator consisting of activities and groups this group holds.
	 */
	public Iterator<Activity> iterator();
}
