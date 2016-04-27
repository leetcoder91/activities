package com.activities.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.activities.api.Activity;
import com.activities.api.ActivityAction;
import com.activities.api.ActivityAction.ExecutionResultType;
import com.activities.api.ActivityException;
import com.activities.api.Tag;

/**
 * An activity is a single, general purpose, specific task that is performed.
 */
public class ActivityImpl implements Activity {
	// Constants

	/**
	 * The Logger instance.
	 */
	private static final Log CAT = LogFactory.getLog(ActivityImpl.class);

	/**
	 * The maximum number of retries allowed for an activity to recover from failure.
	 */
	private static final Long MAX_RETRIES = Long.getLong("com.quest.glue.maxActivityRetry.count", 5);

	// Attributes

	/**
	 * The action to perform.
	 */
	private final ActivityAction mAction;

	/**
	 * The enable flag that indicates whether the activity was disabled by the service.
	 */
	private boolean mEnabled;

	/**
	 * The executing flag that determines whether the activity is executing.
	 */
	private boolean mExecuting;

	private final ActivityServiceImpl mService;

	// Constructors

	/**
	 * Constructs the activity.
	 *
	 * @param action The activity action.
	 * @param logSource The log source of the owner of this activity.
	 */
	public ActivityImpl(ActivityAction action, ActivityServiceImpl service) {
		mAction = action;
		mEnabled = true;

	    mService = service;

		if (CAT.isDebugEnabled()) {
			CAT.debug("Activity created \"" + getActionName() + "\"");
		}
	}

	// Operations

	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#getActionName()
	 */
	@Override
	public String getActionName() {
		return mAction.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#getActionClassName()
	 */
	@Override
	public String getActionClassName() {
		return mAction.getClass().getName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#getActionTags()
	 */
	@Override
	public Tag[] getActionTags() {
		return mAction.getTags();
	}

	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#getAction()
	 */
	@Override
	public ActivityAction getAction() {
		return mAction;
	}

	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#isExecuting()
	 */
	@Override
	public synchronized boolean isExecuting() {
		return mExecuting;
	}

	/**
	 * Sets the activity as executing.
	 * This method should only be called internally and its caller must be synchronized.
	 *
	 * @param executing The executing flag to set.
	 */
	private void setExecuting(boolean executing) {
		if (CAT.isDebugEnabled()) {
			CAT.debug("Execution status changed from " + mExecuting + " to " + executing +
				" for activity \"" + getActionName() + "\".");
		}

		mExecuting = executing;
	}

	/**
	 * Execute the activity action and return one of ExecutionResultType types as the result. If the
	 * execution fails, retries the execution given that ActivityAction.canRetry() return true.
	 * Continues to retry until ActivityAction.canRetry() returns false.
	 *
	 * @return {@link ExecutionResultType}
	 *
	 * @throws ActivityException
	 */
	private ExecutionResultType exec() throws InterruptedException, ActivityException {
		ExecutionResultType executionResult = ExecutionResultType.FAILURE;
		final boolean executing = isExecuting();
		final boolean enabled = isEnabled();
	
		if (!executing) {
	        if(enabled) {
	            try {
	                setExecuting(true);
	                executionResult = mAction.perform();
	
	                if (executionResult == ExecutionResultType.SUCCESS) {
	                    if (CAT.isDebugEnabled()) {
	                        CAT.debug("Successfully executed activity \"" + getActionName() + "\"");
	                    }
	                }
	                else if (executionResult == ExecutionResultType.DISABLE) {
	                    if (CAT.isDebugEnabled()) {
	                        CAT.debug("Successfully executed activity \"" + getActionName() +
                        		"\" however the activity and all its dependents will be disabled as requested.");
	                    }
	                }
	                else {
	                    if (CAT.isDebugEnabled()) {
	                        CAT.debug("Execution did not succeed for activity \"" + getActionName() +
                        		"\". Activity's action returned false.");
	                    }
	                }
	            }
	            catch (InterruptedException e) {
	                CAT.debug("Interrupted while executing activity " + getActionName() +
	                    "; rethrowing interrupted exception", e);
	
	                throw e;
	            }
	            catch (Exception e) {
	                CAT.warn("Execution of activity \"" + getActionName() +
                		"\" failed because it threw an exception.", e);
	            }
	            finally {
	                setExecuting(false);
	            }
	    	}
	        else {
	            // activity already disabled
	            executionResult = ExecutionResultType.DISABLE;

	            if (CAT.isDebugEnabled()) {
	                CAT.debug("Activity \"" + getActionName() +
                		"\" was already disabled so all its dependents will be disabled if they are not already.");
	            }
	        }
	    }
		else {
	        // we should never be trying to execute and activity that is already executing
	        CAT.error("Failed to execute activity " + getActionName() +
        		" because its state is invalid since it is already executing");
		}
	
		return executionResult;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#execute()
	 */
	@Override
	public synchronized ExecutionResultType execute() throws InterruptedException, ActivityException {
		ExecutionResultType executionResult = exec();
	
		if (executionResult == ExecutionResultType.DISABLE) {
			disable();
		}
		else {
			int retryCount = 0;
	
			while (executionResult == ExecutionResultType.FAILURE && mAction.canRetry()
				&& retryCount <= MAX_RETRIES) {
				retryCount++;
	
				if (CAT.isDebugEnabled()) {
					CAT.debug("Attempting to execute activity \"" + getActionName() +
						"\". Try #" + Integer.toString(retryCount) + ".");
				}
	
				executionResult = exec();
			}
	
			if (executionResult == ExecutionResultType.FAILURE) {
				if (retryCount == MAX_RETRIES) {
					if (CAT.isDebugEnabled()) {
						CAT.debug("Attempted to execute activity \"" + getActionName() +
							"\" maximum number of time(s) \"" + Long.toString(MAX_RETRIES) +
							"\", however it continues to fail. Disabling it.");
					}
				}
				else if (retryCount > 0 && retryCount < MAX_RETRIES) {
					if (CAT.isDebugEnabled()) {
						CAT.debug("Attempted to execute activity \"" + getActionName() + "\"" +
							Integer.toString(retryCount) + " time(s), however it continues to fail. Disabling it.");
					}
				}
				else {
					if (CAT.isDebugEnabled()) {
						CAT.debug("Retry was not attempted to execute activity \"" + getActionName() +
							"\" because either canRetry() is not implemented or it returned false.");
					}
				}
	
				disable();
			}
		}
	
		return executionResult;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#disable()
	 */
	@Override
	public synchronized boolean disable() {
		if (!isExecuting()) {
			mEnabled = false;
	
			if (CAT.isDebugEnabled()) {
				CAT.debug("Activity \"" + getActionName() + "\" was disabled.");
			}
		}
		else {
			if (CAT.isDebugEnabled()) {
				CAT.debug("Could not disable activity \"" + getActionName() +
					"\" because it is already executing.");
			}
		}
	
		return mEnabled;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#isEnabled()
	 */
	@Override
	public synchronized boolean isEnabled() {
		return mEnabled && mAction.isEnabled();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Activity: " + getActionName();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#before(com.activities.api.Activity[])
	 */
	@Override
	public void before(Activity... succeedingActivities) {
	    mService.before(this, succeedingActivities);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#after(com.activities.api.Activity[])
	 */
	@Override
	public void after(Activity... precedingActivities) {
	    mService.after(this, precedingActivities);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#removeAllDependencies()
	 */
	@Override
	public void removeAllDependencies() {
	    mService.remove(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#tag(com.activities.api.Tag[])
	 */
	@Override
	public void tag(Tag... tags) {
	    mService.tag(this, tags);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#untag(com.activities.api.Tag[])
	 */
	@Override
	public void untag(Tag... tags) {
	    mService.untag(this, tags);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.activities.api.Activity#removeAllTags()
	 */
	@Override
	public void removeAllTags() {
	}
}
