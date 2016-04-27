package com.activities.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.activities.api.Activity;
import com.activities.api.ActivityAction;
import com.activities.api.ActivityAction.ExecutionResultType;
import com.activities.api.ActivityException;
import com.activities.api.ActivityService;
import com.activities.api.Tag;
import com.activities.utils.CorePoolThreadFactory;
import com.activities.utils.CyclicDataException;
import com.activities.utils.DAGVertex;
import com.activities.utils.PriorityThreadPool;

/**
 * The activities service provides the capability to manage activities, execute the required
 * action, and return the result back to the caller.
 */
public class ActivityServiceImpl implements ActivityService {
	
	// Constants
	
	/**
	 * The Logger instance.
	 */
	private static final Log CAT = LogFactory.getLog(ActivityServiceImpl.class);
	
	/**
	 * The debug logging enabled/disabled flag for activities service.
	 */
	protected static final boolean LOGGING_ENABLED =
		Boolean.getBoolean("com.quest.glue.activities.debugOperations.enabled");
	
	/**
	 * The maximum number of threads to use while executing activities in parallel.
	 */
	protected static final int MAX_POOL_SIZE =
		Long.getLong("com.quest.glue.maxActivityPoolSize.count", 20).intValue();
	
	/**
	 * The priority thread pool for executing activities in parallel.
	 */
	private static final PriorityThreadPool ACTIVITY_POOL =
		new PriorityThreadPool(
				1,  				// CORE_POOL_SIZE
				MAX_POOL_SIZE, 		// MAX_POOL_SIZE
				120, 				// KEEP_ALIVE
				TimeUnit.SECONDS, 	// KEEP_ALIVE_UNITS
				new CorePoolThreadFactory("ActivityExecutor")
		);
	
	// Attributes
	
	// Associations
	
	/**
	 * Map of vertex indexed by activity.
	 */
	private final TreeMap<Activity, DAGVertex<Activity>> mVertextByActivityMap =
		new TreeMap<Activity, DAGVertex<Activity>>(
			new Comparator<Activity>() {
				@Override
				public int compare(Activity activity1, Activity activity2) {
					return Integer.valueOf(activity1.hashCode()).compareTo(activity2.hashCode());
				}
			}
		);
	
	/**
	 * Map of list of activities indexed by Tag.
	 */
	private HashMap<Tag, List<Activity>> mActivityListByTag = new HashMap<Tag, List<Activity>>(3);
	
	/**
	 * Map of activity info objects indexed by activity.
	 */
	private HashMap<Activity, ActivityInfo> mActivityInfoByActivityMap =
		new HashMap<Activity, ActivityInfo>(3);
	
	// Constructors
	
	public ActivityServiceImpl() {
	}
	
	// Operations
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#create(java.lang.String, com.quest.glue.api.services.activities.ActivityAction)
	 */
	@Override
	public synchronized Activity create(ActivityAction action) throws ActivityException {
		if (action != null) {
			Activity activity = new ActivityImpl(action, this);
	
			tag(activity, activity.getActionTags());
	
			if (CAT.isDebugEnabled()) {
				CAT.debug("Successfully created activity \"" + activity.getActionName() +
					"\" [class: \"" + activity.getActionClassName() + "\"].");
			}
	
			return activity;
		}
	
		throw new ActivityException("Cannot create an activity without an action.");
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#add(com.quest.glue.api.services.activities.Activity)
	 */
	@Override
	public synchronized void add(Activity... activities) {
		if (activities != null) {
			for (Activity activityToAdd : activities) {
				getVertex(activityToAdd);
	
				if (CAT.isDebugEnabled()) {
					CAT.debug("Successfully added activity \"" + activityToAdd.getActionName() +
						"\" [class: \"" + activityToAdd.getActionClassName() + "\" to the execution queue.");
				}
			}
		}
	}
	
	/**
	 * Establish dependency between two activities such that the preceding activity comes before
	 * the succeeding activity.
	 *
	 * @param precedingActivity The activity to precede in execution.
	 * @param succeedingActivity The activity to succeed in execution.
	 */
	private void addDependency(Activity precedingActivity, Activity succeedingActivity) {
	    DAGVertex<Activity> precedingVertex = getVertex(precedingActivity);
	    DAGVertex<Activity> succeedingVertex = getVertex(succeedingActivity);
	
	    precedingVertex.before(succeedingVertex);
	}
	
	/**
	 * @param activity The activity.
	 * @return True, if there exists a vertex corresponding to the activity.
	 */
	private boolean isVertexAvailable(Activity activity) {
		return mVertextByActivityMap.containsKey(activity);
	}
	
	/**
	 * Get the vertex corresponding to the activity. Add the vertex if it does not exist and return
	 * it.
	 *
	 * @param activity The activity.
	 * @return The vertex corresponding to the activity.
	 */
	private DAGVertex<Activity> getVertex(Activity activity) {
	    if (!isVertexAvailable(activity)) {
	        mVertextByActivityMap.put(activity, new DAGVertex<Activity>(activity));
	    }
	
	    return mVertextByActivityMap.get(activity);
	}

	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#before(com.quest.glue.api.services.activities.Activity, com.quest.glue.api.services.activities.Activity[])
	 */
	@Override
	public synchronized void before(Activity activity, Activity... succeedingActivities) {
		if (activity != null) {
			for (Activity activityToAdd : succeedingActivities) {
				if (activityToAdd != null) {
					addDependency(activity, activityToAdd);

					if (CAT.isDebugEnabled()) {
						CAT.debug("Successfully added activity \"" + activity.getActionName() +
							"\" [class: \"" + activity.getActionClassName() + "\"] before activity \"" +
							activityToAdd.getActionName() + "\" [class: \"" + activityToAdd.getActionClassName() +
							"\"] in the execution queue.");
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#after(com.quest.glue.api.services.activities.Activity, com.quest.glue.api.services.activities.Activity[])
	 */
	@Override
	public synchronized void after(Activity activity, Activity... precedingActivities) {
		if (activity != null) {
			for (Activity activityToAdd : precedingActivities) {
				if (activityToAdd != null) {
					addDependency(activityToAdd, activity);
	
					if (CAT.isDebugEnabled()) {
						CAT.debug("Successfully added activity \"" + activity.getActionName() +
							"\" [class: \"" + activity.getActionClassName() + "\"] after activity \"" +
							activityToAdd.getActionName() + "\" [class: \"" + activityToAdd.getActionClassName() +
							"\"] in the execution queue.");
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#getAction(com.quest.glue.api.services.activities.Activity)
	 */
	@Override
	public synchronized ActivityAction getAction(Activity activity) {
		ActivityAction action = null;
	
		if (activity != null && isVertexAvailable(activity)) {
			action = activity.getAction();
		}
	
		return action;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#remove(com.quest.glue.api.services.activities.Activity)
	 */
	@Override
	public synchronized boolean remove(Activity activity) {
		if (activity != null && isVertexAvailable(activity)) {
			Tag[] tags = activity.getActionTags();
	
			untag(activity, tags);
	
			DAGVertex<Activity> vertex = mVertextByActivityMap.remove(activity);
	
			vertex.remove();
	
			if (CAT.isDebugEnabled()) {
				CAT.debug("Successfully deleted activity \"" + activity.getActionName() +
					"\" [class: \"" + activity.getActionClassName() + "\"].");
			}
		}
	
		return false;
	}
	
	/**
	 * Recursively disables activity and any of its dependents.
	 *
	 * @param activity The activity to disable.
	 * @param dependentSetByActivityMap The map of set of dependent activities indexed by activity.
	 */
	private void disableDependentActivities(Activity activity,
		HashMap<Activity, HashSet<Activity>> dependentSetByActivityMap) {
	
		if (!activity.isEnabled()) {
			for (Activity dependent : dependentSetByActivityMap.get(activity)) {
				if (dependent.isEnabled()) {
					boolean disable = dependent.disable();
	
					if (!disable) {
						if (CAT.isDebugEnabled()) {
							CAT.debug("Failed to disable dependent activity \"" + dependent.getActionName() +
								"\" [class: \"" + dependent.getActionClassName() + "\"]. Parent activity \"" +
								activity.getActionName() + "\" [class: \"" + activity.getActionClassName() + "\"]");
						}
					}
					else {
						if (CAT.isDebugEnabled()) {
							CAT.debug("Successfully disabled dependent activity \"" + dependent.getActionName() +
								"\" [class: \"" + dependent.getActionClassName() + "\"]. Parent activity \"" +
								activity.getActionName() + "\" [class: \"" + activity.getActionClassName() + "\"]");
						}
	
						disableDependentActivities(dependent, dependentSetByActivityMap);
					}
				}
			}
		}
	}
	
	/**
	 * Executes the activity.
	 *
	 * @param activityInfo The activity info object containing execution information for the activity.
	 *
	 * @return {@link ExecutionResultType}
	 *
	 * @throws ActivityException
	 */
	private ExecutionResultType executeActivity(ActivityInfo activityInfo) throws InterruptedException, ActivityException {
		Activity activity = activityInfo.getActivity();
		int currentIndex = activityInfo.getCurrentIndex();
		int executionListSize = activityInfo.getExecutionListSize();
	
		HashMap<Activity, HashSet<Activity>> dependentSetByActivityMap =
			activityInfo.getDependentSetByActivityMap();
	
	    if (CAT.isDebugEnabled()) {
	        CAT.debug("Executing " + Integer.toString(currentIndex) + "/" + executionListSize +
        		": activity \"" + activity.getActionName() + "\" [class: \"" + activity.getActionClassName() + "\"].");
	    }
	
	    ExecutionResultType executionResult = activity.execute();
	
	    if (executionResult == ExecutionResultType.FAILURE
			|| executionResult == ExecutionResultType.DISABLE
	        || executionResult == ExecutionResultType.DISABLE_ONCE ) {
	    	if (executionResult == ExecutionResultType.FAILURE) {
		    	CAT.warn("Failed to execute activity \"" + activity.getActionName() + "\" [class: \"" +
	    			activity.getActionClassName() + "\"] successfully. Disabling all dependent activities.");
	    	}
	
	    	if (!activity.isEnabled()) {
	    		disableDependentActivities(activity, dependentSetByActivityMap);
	    	}
	    	else {
	    		CAT.warn("Failed activity \"" + activity.getActionName() + "\" [class: \"" +
    				activity.getActionClassName() + "\"] could not disabled. Trying to disable the activity and all its dependents.");
	
	    		boolean disable = activity.disable();
	
	    		if (disable) {
	        		disableDependentActivities(activity, dependentSetByActivityMap);
	    		}
	    	}
	    }
	
	    return executionResult;
	}
	
	/**
	 * @param dependsOnSetByActivityMap Map of set of depends on activities indexed by activity.
	 *
	 * @return The count of the number of direct or indirect activities the activity depends on.
	 */
	private int findMaxDepth(LinkedHashMap<Activity, HashSet<Activity>> dependsOnSetByActivityMap,
		HashSet<Activity> dependsOnSet, LinkedHashMap<Activity, Integer> maxDepthByActivityMap) {
		if (dependsOnSet.isEmpty()) {
			return 0;
		}
		else {
			int maxDepth = 0;
	
			for (Activity activity : dependsOnSet) {
				if (maxDepthByActivityMap.containsKey(activity)) {
					maxDepth = Math.max(maxDepth, maxDepthByActivityMap.get(activity));
				}
				else {
					maxDepth = Math.max(maxDepth, findMaxDepth(dependsOnSetByActivityMap,
						dependsOnSetByActivityMap.get(activity), maxDepthByActivityMap));
				}
			}
	
			return maxDepth + 1;
		}
	}
	
	/**
	 * @param dependsOnSetByActivityMap Map of set of depends on activities indexed by activity.
	 *
	 * @return The map of set of activities indexed by priority. The map is sorted in descending order
	 * with respect to priorities. The priority is calculated by counting the number of direct or
	 * indirect activities the activity depends on.
	 */
	private TreeMap<Integer, HashSet<Activity>> getPriorities(
		LinkedHashMap<Activity, HashSet<Activity>> dependsOnSetByActivityMap) {
		LinkedHashMap<Activity, Integer> maxDepthByActivityMap =
			new LinkedHashMap<Activity, Integer>(dependsOnSetByActivityMap.size());
	
		for (Iterator<Entry<Activity, HashSet<Activity>>> itr =
			dependsOnSetByActivityMap.entrySet().iterator(); itr.hasNext();) {
			Entry<Activity, HashSet<Activity>> dependsOnSetByActivityEntry = itr.next();
			Activity activity = dependsOnSetByActivityEntry.getKey();
			HashSet<Activity> dependsOnSet = dependsOnSetByActivityEntry.getValue();
	
			int maxDependsOnCount = findMaxDepth(dependsOnSetByActivityMap, dependsOnSet,
				maxDepthByActivityMap);
	
			maxDepthByActivityMap.put(activity, maxDependsOnCount);
		}
	
		int maxDependsOn = Collections.max(maxDepthByActivityMap.values());
	
		TreeMap<Integer, HashSet<Activity>> activitySetByPriorityMap =
			new TreeMap<Integer, HashSet<Activity>>(
				new Comparator<Integer>() {
					@Override
					public int compare(Integer priority1, Integer priority2) {
						return priority2.intValue() - priority1.intValue();
					}
				}
			);
	
		for (Iterator<Entry<Activity, Integer>> itr =
			maxDepthByActivityMap.entrySet().iterator(); itr.hasNext();) {
			Entry<Activity, Integer> maxDepthByActivityEntry = itr.next();
			Activity activity = maxDepthByActivityEntry.getKey();
			Integer maxDepth = maxDepthByActivityEntry.getValue();
			int priority = maxDependsOn - maxDepth;
			HashSet<Activity> activitySet = activitySetByPriorityMap.get(priority);
	
			if (activitySet == null) {
				activitySet = new HashSet<Activity>(1);
				activitySetByPriorityMap.put(priority, activitySet);
			}
	
			activitySet.add(activity);
		}
	
		return activitySetByPriorityMap;
	}
	
	/**
	 * @param activityList The list of activities.
	 * @return The list of vertices corresponding to the provided list of activities. If the activity list
	 * is null, all vertices will be returned.
	 */
	private List<DAGVertex<Activity>> getVertexList(List<Activity> activityList) {
		final List<DAGVertex<Activity>> vertexList = new ArrayList<DAGVertex<Activity>>(1);
	
		if (activityList != null && !activityList.isEmpty()) {
			for (Activity activity : activityList) {
				DAGVertex<Activity> vertex = mVertextByActivityMap.get(activity);
	
				vertexList.add(vertex);
			}
		}
		else {
			for (Map.Entry<Activity, DAGVertex<Activity>> vertex : mVertextByActivityMap.entrySet()) {
				vertexList.add(vertex.getValue());
		    }
		}
	
		return vertexList;
	}
	
	/**
	 * Count down the latch corresponding to a priority level acquired by an activity.
	 *
	 * @param activity The activity whose latch needs to be counted down.
	 */
	private void countDownLevelLatch(Activity activity) {
		ActivityInfo activityInfo = getActivityInfo(activity);
		CountDownLatch latch = activityInfo.getLatch();
	
		latch.countDown();
	}
	
	/**
	 * @param activity The activity.
	 * @return The ActivityInfo object corresponding to the activity.
	 */
	private ActivityInfo getActivityInfo(Activity activity) {
		return mActivityInfoByActivityMap.get(activity);
	}
	
	private void executeActivities(List<Activity> activityList, boolean parallelExecution)
		throws InterruptedException, ActivityException {
	    List<DAGVertex<Activity>> vertexList = getVertexList(activityList);
	
		HashMap<Activity, HashSet<Activity>> dependentSetByActivityMap =
			DAGVertex.getMapOfDependents(vertexList);
	
	    List<DAGVertex<Activity>> sortedVertexList = null;
	
		if (LOGGING_ENABLED) {
			String graphVizOutput = DAGVertex.generateGraphVizOutput(vertexList, 50, 50);
	
			CAT.debug("GraphViz output showcasing the dependencies between activities.\n\n" + graphVizOutput + "\n\n");
		}
	
		try {
			sortedVertexList = DAGVertex.sort(vertexList);
		}
		catch (CyclicDataException e) {
			throw new ActivityException("Cyclic dependency was encountered in the activity execution" +
				" queue. Abandoning execution.", e);
		}
	
		int executionListSize = sortedVertexList.size();
	
		if (executionListSize > 0) {
			if (parallelExecution) {
				LinkedHashMap<Activity, HashSet<Activity>> dependsOnSetByActivityMap =
					DAGVertex.getMapOfDependsOn(vertexList);
	
				TreeMap<Integer, HashSet<Activity>> activitySetByPriorityMap =
					getPriorities(dependsOnSetByActivityMap);
	
				LinkedHashMap<Future<Boolean>, Activity> activityByFutureMap =
					new LinkedHashMap<Future<Boolean>, Activity>(executionListSize);
	
				LinkedHashMap<Activity, CountDownLatch> latchByActivityMap =
					new LinkedHashMap<Activity, CountDownLatch>(executionListSize);
	
			    for (int i = 0; i < executionListSize; i++) {
			        Activity activity = sortedVertexList.get(i).getValue();
			        CountDownLatch latch = new CountDownLatch(1);
	
			        latchByActivityMap.put(activity, latch);
			    }
	
			    int i = 0;
	
				for (Iterator<Entry<Integer, HashSet<Activity>>> itr =
					activitySetByPriorityMap.entrySet().iterator(); itr.hasNext();) {
					Entry<Integer, HashSet<Activity>> activitySetByPriorityEntry = itr.next();
					Integer priority = activitySetByPriorityEntry.getKey();
					HashSet<Activity> activitySet = activitySetByPriorityEntry.getValue();
	
				    for (Activity activity : activitySet) {
				        HashSet<Activity> dependsOnSet = dependsOnSetByActivityMap.get(activity);
	
				        ActivityInfo activityInfo = new ActivityInfo(activity, ++i,
			        		executionListSize, latchByActivityMap.get(activity),
			        		dependentSetByActivityMap, dependsOnSet);
	
				        mActivityInfoByActivityMap.put(activity, activityInfo);
				    }
	
				    for (Activity activity : activitySet) {
				        ActivityInfo activityInfo = mActivityInfoByActivityMap.get(activity);
				        ActivityExecutor executor = new ActivityExecutor(activityInfo);
				        Future<Boolean> future = ACTIVITY_POOL.submit(executor, priority);
	
				        activityByFutureMap.put(future, activity);
				    }
	
	                for (Activity activity : activitySet) {
	                    ActivityInfo activityInfo = getActivityInfo(activity);
	                    CountDownLatch activityLatch = activityInfo.getLatch();
	
	                    try {
	                        activityLatch.await();
	                    }
	                    finally {
	                        activityLatch.countDown();
	                    }
	                }
	
	                try {
	                    for (Iterator<Map.Entry<Future<Boolean>, Activity>> it =
	                		activityByFutureMap.entrySet().iterator(); it.hasNext();) {
	                        Map.Entry<Future<Boolean>, Activity> entry = it.next();
	
	                        try {
	                            entry.getKey().get();
	                        }
	                        finally {
	                            countDownLevelLatch(entry.getValue());
	                            it.remove();
	                        }
	                    }
	                }
	                catch (ExecutionException e) {
	                    CAT.debug("Activity execution failed; rethrowing cause", e);
	
	                    Throwable cause = e.getCause();
	
	                    if (cause instanceof InterruptedException) {
	                        throw (InterruptedException)cause;
	                    }
	
	                    throw new ActivityException("Execution failed while executing activity ",
	                		cause);
	                }
	                finally {
	                    // cancel remaining futures
	                    for (Map.Entry<Future<Boolean>, Activity> entry :
	                    	activityByFutureMap.entrySet()) {
	                        entry.getKey().cancel(true);
	                    }
	                }
			    }
			}
			else {
				StringBuffer orderBuffer = new StringBuffer();
	
				for (DAGVertex<Activity> vertex : sortedVertexList) {
					Activity activity = vertex.getValue();
	
					orderBuffer.append(activity.getActionName() + "\n");
				}
	
				if (CAT.isDebugEnabled()) {
					CAT.debug("Executing activities in the following order:\n" + orderBuffer.toString() + ".");
				}
	
			    for (int i = 0; i < executionListSize; i++) {
			        Activity activity = sortedVertexList.get(i).getValue();
	
			        ActivityInfo activityInfo = new ActivityInfo(activity, i + 1,
		        		executionListSize, null, dependentSetByActivityMap, null);
	
			        executeActivity(activityInfo);
			    }
			}
		}
		else {
			if (CAT.isDebugEnabled()) {
				CAT.debug("There are no activities to execute.");
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#executeFiltered(java.util.List, boolean)
	 */
	@Override
	public synchronized void executeFiltered(List<Activity> activityList, boolean parallelExecution)
		throws InterruptedException, ActivityException {
		try {
			executeActivities(activityList, parallelExecution);
		}
		finally {
			mActivityInfoByActivityMap.clear();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#runActivities()
	 */
	@Override
	public synchronized void executeAll(boolean parallelExecution) throws InterruptedException, ActivityException {
		try {
			executeActivities(null, parallelExecution);
		}
		finally {
			mActivityInfoByActivityMap.clear();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#reset()
	 */
	@Override
	public synchronized void reset() {
		Set<Activity> activitySet = mVertextByActivityMap.keySet();
	
		if (activitySet != null) {
			Object[] activityObjs = activitySet.toArray();
			int size = activityObjs.length;
	
			if (CAT.isDebugEnabled()) {
				CAT.debug("Resetting activity execution queue of size \"" + Integer.toString(size) + "\".");
			}
	
		    for (int i = 0; i < size; i++) {
		    	remove((Activity)activityObjs[i]);
		    }
	
		    mVertextByActivityMap.clear();
		    mActivityListByTag.clear();
		}
	
		mActivityInfoByActivityMap.clear();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#createTag(java.lang.String)
	 */
	@Override
	public synchronized Tag createTag(String name) {
		Tag tag = new TagImpl(name);
	
		if (!mActivityListByTag.containsKey(tag)) {
			mActivityListByTag.put(tag, null);
		}
	
		return tag;
	}
	
	/**
	 * Recursively adds the activity and activities that it depends on to the list.
	 *
	 * @param activityList The list of activities to build.
	 * @param activityToAddList The list of activities to add.
	 * @param dependsOnSetByActivityMap Map of set of depends on activities indexed by activity.
	 * @return The list of activities.
	 */
	private void addActivities(List<Activity> activityList, List<Activity> activityToAddList,
			HashMap<Activity, HashSet<Activity>> dependsOnSetByActivityMap) {
		if (activityToAddList == null || activityToAddList.isEmpty()) {
			return;
		}
		else {
			for (Activity activityToAdd : activityToAddList) {
				if (!activityList.contains(activityToAdd)) {
					activityList.add(activityToAdd);
	
					if (dependsOnSetByActivityMap != null) {
						HashSet<Activity> dependsOnSet = dependsOnSetByActivityMap.get(activityToAdd);
	
						if (dependsOnSet != null && !dependsOnSet.isEmpty()) {
							addActivities(activityList, new ArrayList<Activity>(dependsOnSet),
								dependsOnSetByActivityMap);
						}
					}
				}
			}
		}
	}
	
	/**
	 * The list of activities.
	 *
	 * @param taggedOnly If true, retrieve only tagged activities. If false, retrieve all
	 * activities and activities they depend on regardless of whether they are tagged.
	 * @param tags One or more tags to use as key while searching for corresponding activities.
	 *
	 * @return The list of activities.
	 */
	private List<Activity> getTaggedActivities(boolean taggedOnly, Tag... tags) {
		List<Activity> activityList = new ArrayList<Activity>();
	
		if (tags != null && tags.length > 0) {
		    List<DAGVertex<Activity>> vertexList = getVertexList(null);
	
			LinkedHashMap<Activity, HashSet<Activity>> dependsOnSetByActivityMap =
				(taggedOnly) ? null : DAGVertex.getMapOfDependsOn(vertexList);
	
			for (Tag tag : tags) {
				List<Activity> activityToAddList = mActivityListByTag.get(tag);
	
				addActivities(activityList, activityToAddList, dependsOnSetByActivityMap);
			}
		}
	
		return activityList;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#getActivities(boolean, com.quest.glue.api.services.activities.Tag[])
	 */
	@Override
	public synchronized List<Activity> getActivities(boolean taggedOnly, Tag... tags) {
		return getTaggedActivities(taggedOnly, tags);
	}
	
	/**
	 * Tag the activity.
	 *
	 * @param activity The activity to tag. Cannot be null.
	 */
	/*pkg*/ void tag(Activity activity, Tag... tags) {
		if (tags != null) {
			for (Tag tag : tags) {
				List<Activity> activityList = mActivityListByTag.get(tag);
	
				if (activityList == null) {
					activityList = new ArrayList<Activity>();
					mActivityListByTag.put(tag, activityList);
				}
	
				activityList.add(activity);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.quest.glue.api.services.activities.ActivityService#untag(com.quest.glue.api.services.activities.Activity, com.quest.glue.api.services.activities.Tag[])
	 */
	@Override
	public synchronized void untag(Activity activity, Tag... tags) {
		if (activity != null && tags != null) {
			for (Tag tag : tags) {
				List<Activity> activityList = mActivityListByTag.get(tag);
	
				if (activityList != null && !activityList.isEmpty()) {
					activityList.remove(activity);
	
					if (activityList.isEmpty()) {
						mActivityListByTag.remove(tag);
					}
				}
			}
		}
	}
	
	// Inner classes
	
	/**
	 * Contains activity execution related information.
	 */
	private class ActivityInfo {
		// Attributes
	
		/**
		 * The index of the activity in the execution queue that is being executed.
		 */
		final private int mCurrentIndex;
	
		/**
		 * The size of the execution queue.
		 */
		final private int mExecutionListSize;
	
		/**
		 * The activity being executed.
		 */
		final private Activity mActivity;
	
		/**
		 * The count down latch that belongs to this activity.
		 */
		final private CountDownLatch mLatch;
	
		// Associations
	
		/**
		 * Map of set of dependent activities indexed by activity.
		 */
		final private HashMap<Activity, HashSet<Activity>> mDependentSetByActivityMap;
	
		/**
		 * Set of activities that this activity depends on.
		 */
		final private HashSet<Activity> mDependsOnSet;
	
		// Constructors
	
		/**
		 * Constructs the activity info.
		 *
		 * @param activity The activity to execute.
		 * @param currentIndex The index of the activity in the execution queue that is to be executed.
		 * @param executionListSize The size of the execution queue.
		 * @param dependentSetByActivityMap Map of set of dependent activities indexed by activity.
		 * @param latch The count down latch that belongs to this activity.
		 * @param dependsOnSet Set of activities this activity depends on.
		 */
		private ActivityInfo(Activity activity,	int currentIndex, int executionListSize,
			CountDownLatch latch, HashMap<Activity, HashSet<Activity>> dependentSetByActivityMap,
			HashSet<Activity> dependsOnSet) {
			mActivity = activity;
			mDependentSetByActivityMap = dependentSetByActivityMap;
			mCurrentIndex = currentIndex;
			mExecutionListSize = executionListSize;
			mLatch = latch;
			mDependsOnSet = dependsOnSet;
		}
	
		// Operations
	
		/**
		 * @return The index of the activity in the execution queue that is being executed.
		 */
		private int getCurrentIndex() {
			return mCurrentIndex;
		}
	
		/**
		 * @return The size of the execution queue.
		 */
		private int getExecutionListSize() {
			return mExecutionListSize;
		}
	
		/**
		 * @return The activity being executed.
		 */
		private Activity getActivity() {
			return mActivity;
		}
	
		/**
		 * @return The map of set of dependent activities indexed by activity.
		 */
		private HashMap<Activity, HashSet<Activity>> getDependentSetByActivityMap() {
			return mDependentSetByActivityMap;
		}
	
		/**
		 * @return The count down latch that belongs to this activity.
		 */
		private CountDownLatch getLatch() {
			return mLatch;
		}
	
		/**
		 * @return The list of count down latches that belong to activities that this activity depends
		 * on.
		 */
		private List<CountDownLatch> getDependsOnLatchList() {
			List<CountDownLatch> dependsOnLatchList =
				new ArrayList<CountDownLatch>(mDependsOnSet.size());
	
			for (Activity activity : mDependsOnSet) {
				ActivityInfo activityInfo = getActivityInfo(activity);
	
				dependsOnLatchList.add(activityInfo.getLatch());
			}
	
			return dependsOnLatchList;
		}
	}
	
	/**
	 * The activity executor.
	 */
	private class ActivityExecutor implements Callable<Boolean> {
		// Attributes
	
		/**
		 * The activity being executed.
		 */
		final private ActivityInfo mActivityInfo;
	
		// Constructors
	
		/**
		 * Constructs the activity executor.
		 *
		 * @param activityInfo The activity info.
		 */
		private ActivityExecutor(ActivityInfo activityInfo) {
			mActivityInfo = activityInfo;
		}
	
		// Operations
	
		/*
		 * (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Boolean call() throws InterruptedException, Exception {
	        CountDownLatch activityLatch = mActivityInfo.getLatch();
	        try {
	            for (CountDownLatch dependsOnLatch : mActivityInfo.getDependsOnLatchList()) {
	                dependsOnLatch.await();
	            }
	
	            ExecutionResultType executionResult = executeActivity(mActivityInfo);
	
	            return (executionResult == ExecutionResultType.SUCCESS
	                    || executionResult == ExecutionResultType.DISABLE
	                    || executionResult == ExecutionResultType.DISABLE_ONCE);
	        } finally {
	            activityLatch.countDown();
	        }
		}
	}
}
