package com.activities.api;


/**
 * The ActivityAction class that enables the activity to perform a single, general purpose,
 * specific task. This interface is to be implemented by the cartridge developer.
 */
public interface ActivityAction {
	/**
	 * The action's execution result type.
	 *
	 * <ul>
	 *   <li>{@link ExecutionResultType#SUCCESS}</li>
	 *   <li>{@link ExecutionResultType#FAILURE}</li>
	 *   <li>{@link ExecutionResultType#DISABLE}</li>
	 *   <li>{@link ExecutionResultType#DISABLE_ONCE}</li>
	 * </ul>
	 */
	public static enum ExecutionResultType { 
		/**
		 * The action executed successfully.
		 */
		SUCCESS,
	
		/**
		 * The action executed unsuccessfully. canRetry() will be called to determine whether the
		 * activity's execution should be retried. If execution fails for maximum number of retries,
		 * the activity is disabled along with its dependents for the duration of the current execution
	     * cycle. It will be executed in the next cycle however.
		 */
		FAILURE,
	
		/**
		 * The action completed, however it must be disabled (along with its dependents).
		 * Execution will not be retried during the current execution cycle <strong>or any
	     * subsequent execution cycles</strong>. The related {@link com.quest.glue.api.services.activities.Activity#isEnabled()}
	     * will return false from this point on.
		 */
		DISABLE,
	
	    /**
	     * The action will be disabled for the current execution cycle, but will be executed
	     * in the next cycle. This is only available when using {@link com.quest.glue.api.services.activities.ActivityService2}.
	     * When using {@link com.quest.glue.api.services.activities.ActivityService}, this is
	     * equivalent to returning {@link #DISABLE}
	     */
	    DISABLE_ONCE
	}
	
	// Operations
	
	/**
	 * The activity will perform the required action using the perform() method. The activity must
	 * be enabled before perform() can be executed and must return one of ExecutionResultType types as
	 * the result. In case of a failure, canRetry() will be called to determine whether the activity's
	 * execution should be retried.
	 *
	 * @return {@link ExecutionResultType}
	 *
	 * @throws Exception
	 */
	public ExecutionResultType perform() throws InterruptedException, Exception;
	
	/**
	 * In case of a failure while executing the perform() method, determines whether the execution
	 * should be retried or not.
	 *
	 * @return True, if execution of the perform() method should be retried. Otherwise, false.
	 */
	public boolean canRetry();
	
	/**
	 * @return The action name.
	 */
	public String getName();
	
	/**
	 * Get an array of Tag(s) associated with the action.
	 *
	 * @return The non-empty array of Tag(s) associated with the action. If no tags are required to be
	 * associated with the action, return null.
	 */
	public Tag[] getTags();
	
	/**
	 * Before executing the perform() method, this method is called to determine whether the
	 * execution should proceed or not.
	 *
	 * @return True, if the action is allowed to be executed. Otherwise, false.
	 */
	public boolean isEnabled();
}
