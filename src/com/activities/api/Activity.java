package com.activities.api;

import com.activities.api.ActivityAction.ExecutionResultType;

/**
 * An activity represents a single, general purpose, task that is performed. Activities can
 * depend on each other and will be executed in the correct order by the
 * {@link com.quest.glue.api.services.activities.ActivityService2}. Activities can also
 * be assigned any number of {@link com.quest.glue.api.services.activities.Tag}s and can be
 * searched for using these tags.
 */
public interface Activity {
	/**
	 * @return The action to execute.
	 */
	public ActivityAction getAction();
	
	/**
	 * @return The action's name.
	 */
	public String getActionName();
	
	/**
	 * @return The action's class name.
	 */
	public String getActionClassName();
	
	/**
	 * Get an array of Tag(s) associated with the action.
	 *
	 * @return The non-empty array of Tag(s) associated with the action. If no tags are required to be
	 * associated with the action, return null.
	 */
	public Tag[] getActionTags();
	
	/**
	 * @return True, if the activity is being executed. Otherwise, false.
	 */
	public boolean isExecuting();
	
	/**
	 * Execute the activity action and return one of ExecutionResultType types as the result. If the
	 * execution fails, retries the execution given that ActivityAction.canRetry() return true.
	 * Continues to retry until ActivityAction.canRetry() returns false.
	 *
	 * @return {@link ExecutionResultType}
	 *
	 * @throws InterruptedException if execution was interrupted
	 * @throws ActivityException if there was a problem during execution
	 */
	public ExecutionResultType execute() throws InterruptedException, ActivityException;
	
	/**
	 * @return True, if the activity is enabled. False, if the activity is either disabled by the
	 * service or the user.
	 */
	public boolean isEnabled();
	
	/**
	 * Disables an activity.
	 *
	 * @return True, if the activity was successfully disabled. Otherwise, false.
	 */
	public boolean disable();
	
	/**
	 * Indicate that this activity must execute <strong>before</strong> all the provided
	 * activities. The list of activities can be thought of as depending on the current
	 * activity.
	 *
	 * @param succeedingActivities The activities that this activity must run before.
	 */
	public void before(Activity... succeedingActivities);
	
	/**
	 * Indicate that this activity must run <strong>after</strong> all of the provided
	 * activities. The current activity can be though of as depending on all the
	 * provided activities.
	 *
	 * @param precedingActivities TThe activities this activity must run after.
	 */
	public void after(Activity... precedingActivities);
	
	/**
	 * Removed from this activity any that depend on it, and any that it depends on.
	 */
	public void removeAllDependencies();
	
	/**
	 * Assign the tags provided to the activity. Tags can be applied to any number of
	 * activities.
	 *
	 * @param tags The tags to be added to this activity.
	 */
	public void tag(Tag... tags);
	
	/**
	 * Remove the list of provided tags from this activity.
	 *
	 * @param tags The list of tags to remove
	 */
	public void untag(Tag... tags);
	
	/**
	 * Remove all registered tags from this activity.
	 */
	public void removeAllTags();


}
