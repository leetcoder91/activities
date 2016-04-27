package com.activities.api;

/**
 * Exception thrown by ServiceFactory when a requested service is not available
 * or if a service method fails in some way. 
 */
public class ServiceException extends Exception {
	private static final long serialVersionUID = 6389952399696008650L;
	
	private final String mServiceType;
	
	/**
	 * Constructor for an exception that wraps a caught exception
	 * 
	 * @param serviceType The name of the service interface that this error is reported by
	 * @param message Description of the problem
	 * @param cause Exception that caused the problem
	 */
	public ServiceException(String serviceType, String message, Throwable cause) {
	    super("Service " + serviceType + ": " + message, cause);
	    mServiceType = serviceType;
	}
	
	/**
	 * Constructor for an exception that was not caused by another one
	 * 
	 * @param serviceType The name of the service interface that this error is reported by
	 * @param message Description of the problem
	 */
	public ServiceException(String serviceType, String message) {
	    this(serviceType, message, null);
	}
	
	/**
	 * Get the service class that could not be constructed
	 * 
	 * @return Class object for the service interface requested
	 */
	public String getServiceType() {
	    return mServiceType;
	}
}
