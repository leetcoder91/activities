package com.activities.utils;

/**
 * An exception that indicates that there is a dependency cycle in the provided list of tasks.
 */
public class CyclicDataException extends Exception {

	private static final long serialVersionUID = 1L;

	public CyclicDataException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CyclicDataException(String message, Throwable cause) {
	    super(message, cause);
	}

	/**
	 * @param message
	 */
	public CyclicDataException(String message) {
	    super(message);
	}

	/**
	 * @param cause
	 */
	public CyclicDataException(Throwable cause) {
	    super(cause);
	}
    
}
