package com.activities.api;

/**
 * A generic wrapper for exceptions thrown while executing activities.
 */
public class ActivityException extends ServiceException {

	// Constants
	
	/**
	 * The serialization version ID.
	 */
	private static final long serialVersionUID = 1L;
	
	// Constructors
	
	public ActivityException(String serviceType, String message, Throwable cause) {
		super(serviceType, message, cause);
	}
	
	public ActivityException(String serviceType, String message) {
		super(serviceType, message);
	}
	
	public ActivityException(String message, Throwable cause) {
		super("ActivityService", message, cause);
	}
	
	public ActivityException(String message) {
		super("ActivityService", message);
	}
}
