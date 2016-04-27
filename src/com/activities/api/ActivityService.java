package com.activities.api;


import java.util.List;

/**
 * The activities service provides the capability to manage activities, execute the required
 * action, and return the result back to the caller.
 */
public interface ActivityService {
	// Operations
	
	/**
	 * Creates an activity.
	 *
	 * @param action The ActivityAction to perform. Cannot be null.
	 * @return The created activity or null, if the activity could not be created successfully.
	 * @throws ActivityException Thrown if the action being used to create the activity is null.
	 */
	public Activity create(ActivityAction action) throws ActivityException;
	
	/**
	 * Add the activities to the execution queue.
	 *
	 * @param activities The activities to add to the execution queue.
	 */
	public void add(Activity... activities);
	
	/**
	 * Add the activity before one or more succeeding activities in the execution queue. If any of the
	 * succeeding activities are not yet added to the execution queue, they will automatically will be
	 * added.
	 *
	 * @param activity The activity to add before one or more activities.
	 * @param succeedingActivities The activities to run before the activity being added.
	 */
	public void before(Activity activity, Activity... succeedingActivities);
	
	/**
	 * Add the activity after one or more preceding activities in the execution queue. If any of the
	 * preceding activities are not yet added to the execution queue, they will automatically will be
	 * added.
	 *
	 * @param activity The activity to add after one or more activities.
	 * @param precedingActivities The activities to run after the activity being added.
	 */
	public void after(Activity activity, Activity... precedingActivities);
	
	/**
	 * Get the AcvitityAction of the activity.
	 *
	 * @return The ActivityAction of the activity.
	 */
	public ActivityAction getAction(Activity activity);
	
	/**
	 * Removes the activity from the execution queue.
	 * Note: Since the execution order has not been determined yet, any dependent activities must be
	 * removed manually by the caller.
	 *
	 * @param activity The activity to remove.
	 * @return True, if the activity was successfully removed. Otherwise, false.
	 */
	public boolean remove(Activity activity);
	
	/**
	 * Iterate over the execution queue executing activities in the defined order.
	 * @param parallelExecution If true, actions will be executed in parallel while maintaining their
	 * dependencies. If false, actions will be executed sequentially.
	 * @throws InterruptedException Thrown if execution was interrupted
	 * @throws ActivityException Thrown if a failed activity and/or its dependents could not be
	 * disabled, the thread executing the activity is interrupted, the dependencies could not be
	 * resolved (there may be a cyclic dependency between activities), or attempting to retrieve
	 * the result of an activity that was aborted by throwing an exception.
	 */
	public void executeAll(boolean parallelExecution) throws InterruptedException, ActivityException;
	
	/**
	 * Executes the provided activities by iterating over the execution queue and executing activities
	 * in the defined order.
	 * @param activityList The list of activities to execute.
	 * @param parallelExecution If true, actions will be executed in parallel while maintaining their
	 * dependencies. If false, actions will be executed sequentially.
	 * @throws InterruptedException Thrown if execution was interrupted
	 * @throws ActivityException Thrown if a failed activity and/or its dependents could not be
	 * disabled, the thread executing the activity is interrupted, the dependencies could not be
	 * resolved (there may be a cyclic dependency between activities), or attempting to retrieve
	 * the result of an activity that was aborted by throwing an exception.
	 */
	public void executeFiltered(List<Activity> activityList, boolean parallelExecution)
		throws InterruptedException, ActivityException;
	
	/**
	 * Resets the execution queue such that all activities are removed from the execution along with
	 * any tags that the activities were tagged with. After calling executeAll(), it is recommended
	 * that the execution queue be reset in order to avoid having duplicate activities in the execution
	 * queue that share the same action.
	 */
	public void reset();
	
	/**
	 * Creates a new tag using the provided tag name.
	 *
	 * @param name The tag name. Cannot be null or an empty string.
	 * @return The created tag.
	 */
	public Tag createTag(String name);
	
	/**
	 * Get the list of activities corresponding to the provided tag name(s). If taggedOnly is false,
	 * retrieve any depends on activities whether or not they are tagged with atleast one of the
	 * provided tags. Otherwise, only retrieve depends on activities that are tagged with atleast
	 * one of the provided tags.
	 *
	 * @param taggedOnly If taggedOnly is false, any activities that the retrieved activity depends on
	 * will also be returned whether or not they are tagged. Otherwise, any activities that the
	 * retrieved activity depends on will be returned if and only if it is tagged with atleast one of
	 * the provided tag names.
	 * @param tags One or more tags to use as key while searching for corresponding activities.
	 *
	 * @return The unordered list of activities corresponding to the provided tag name(s). If no
	 * activities corresponding to the provided tag name(s) are found, an empty List is returned.
	 */
	public List<Activity> getActivities(boolean taggedOnly, Tag... tags);
	
	/**
	 * Untag the provided activity. If there exist no activities tagged with this tag, the tag will be
	 * removed.
	 *
	 * @param activity The activity to untag.
	 * @param tags The tags to untag the activity with.
	 */
	public void untag(Activity activity, Tag... tags);
}
