package com.activities.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a global cache of tags. If there are no activities which reference
 * a given tag, then the tag is eligible to be garbage collected and removed from
 * this cache.
 */
/*pkg*/ class TagCache {

private final Map<String, WeakValue> mCache = new HashMap<String, WeakValue>(3);
private final ReferenceQueue<TagImpl> mRefQueue = new ReferenceQueue<TagImpl>();

/** remove entries once their value is scheduled for removal by the garbage collector */
private void processQueue() {
    WeakValue valueRef;
    while ( (valueRef = (WeakValue) mRefQueue.poll()) != null ) {
        mCache.remove(valueRef.getKey());
    }
}

/**
 * Creates a new tag from the provided key if one did not already exist. If
 * a tag with the provided key already exists, it is returned.
 *
 * @param key The key from which to create the tag
 *
 * @return A Tag for the provided key, newly created if necessary.
 */
public synchronized TagImpl create(String key) {
    processQueue();

    TagImpl result = getReferenceValue(mCache.get(key));
    if ( result == null ) {
        // need to create a new key and add it.
        result = new TagImpl(key);
        mCache.put(key, new WeakValue(key, result, mRefQueue));
    }

    return result;
}

/**
 * Gets the tag from with the provided key.
 *
 * @param key The key to look up.
 *
 * @return the tag with the provided key or null if no such tag exists.
 */
public synchronized TagImpl get(String key) {
    processQueue();
    return getReferenceValue(mCache.get(key));
}

private TagImpl getReferenceValue(WeakValue valueRef) {
    return valueRef == null ? null : valueRef.get();
}

/** for faster removal in {@link #processQueue()} we need to keep track of the mKey for a value */
private static class WeakValue extends WeakReference<TagImpl> {
    private final String mKey;

    private WeakValue(String key, TagImpl value, ReferenceQueue<TagImpl> queue) {
        super(value, queue);
        this.mKey = key;
    }

    private String getKey() {
        return mKey;
    }
}

}
