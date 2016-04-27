package com.activities.impl;

import com.activities.api.Tag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Provides tagging capability.
 */
public class TagImpl implements Tag {

	// Attributes
	
	private Set<ActivityImpl> mActivitiesWithTag = null;
	
	/**
	 * The tag name.
	 */
	public final String mName;
	
	// Constructors
	
	/**
	 * Constructs a tag.
	 *
	 * @param name The tag name. Cannot be null or an empty string.
	 */
	public TagImpl(String name) {
		mName = name;
	}
	
	// Operations
	
	public synchronized void add(ActivityImpl activity) {
	    if (mActivitiesWithTag == null) {
	        mActivitiesWithTag = new HashSet<ActivityImpl>(1);
	    }

	    mActivitiesWithTag.add(activity);
	}
	
	public synchronized void remove(ActivityImpl activity) {
	    if (mActivitiesWithTag != null) {
	        mActivitiesWithTag.remove(activity);
	    }
	}
	
	public synchronized Collection<ActivityImpl> getTaggedActivities() {
	    final Collection<ActivityImpl> result;

	    if (mActivitiesWithTag != null) {
	        result = Collections.unmodifiableSet(mActivitiesWithTag);
	    }
	    else {
	        result = null;
	    }
	
	    return result;
	}
	
	/**
	 * @return The tag name.
	 */
	@Override
	public String getName() {
		return mName;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    return "Tag:[" + mName + "@" + hashCode() + "]";
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
	    return (obj instanceof Tag && mName.equals(((Tag)obj).getName()));
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return 31 + ((mName == null) ? 0 : mName.hashCode());
	}
}