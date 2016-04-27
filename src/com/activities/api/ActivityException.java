package com.activities.api;


/**
 * A generic wrapper for exceptions thrown while executing activities.
 */
public class ActivityException extends Exception {
	// Constants
	
	/**
	 * The serialization version ID.
	 */
	private static final long serialVersionUID = 6389952399696008650L;
	
	// Constructors

	/**
	 * Constructor for an exception that wraps a caught exception
	 * 
	 * @param activity The name of the activity that this error is reported by
	 * @param message Description of the problem
	 * @param cause Exception that caused the problem
	 */
	public ActivityException(String activity, String message, Throwable cause) {
	    super("Activity " + activity + ": " + message, cause);
	}
	
	/**
	 * Constructor for an exception that was not caused by another one
	 * 
	 * @param activity The name of the activity that this error is reported by
	 * @param message Description of the problem
	 */
	public ActivityException(String activity, String message) {
	    this(activity, message, null);
	}
	
	public ActivityException(String message) {
		this("Activity", message);
	}

	public ActivityException(String message, Throwable e) {
		this("Activity", message, e);
	}
}
