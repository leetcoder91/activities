package com.activities.api;


/**
 * Provides tagging capability. Tags are global to the agent, persist for the lifetime
 * of the agent, and are immutable. Should a tag with the same name as an existing tag
 * be requested, the existing tag is returned instead.
 */
public interface Tag {
	/**
	 * @return The tag name.
	 */
	public String getName();
}