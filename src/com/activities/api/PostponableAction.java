package com.activities.api;

/**
 * An {@link com.quest.glue.api.services.activities.ActivityAction} can be asked to
 * execute via its {@link com.quest.glue.api.services.activities.ActivityAction#perform()}
 * method while it is still waiting for some other condition (access to a limited network resource
 * for example). Activities can instead request to be moved to the back of the current execution queue
 * (after a small delay) by implementing this optional interface and returning {@code false}
 * from the {@link #isReady()} method.
 * <p/>
 * If is <strong>strongly</strong> recommended that locks and limited resources <strong>not</strong>
 * be acquired in the {@link #isReady()} method as there may still be some unspecified delay
 * between calling {@link #isReady()} and
 * {@link com.quest.glue.api.services.activities.ActivityAction#perform()}, should there be
 * other actions ahead of it in the active execution queue.
 */
public interface PostponableAction {
	/**
	 * Whether or not this action is ready to be executed. This is only a hint about the current
	 * state of the action. An action that is not ready will be paused for a short period of
	 * time and then rechecked. Other actions may be executed while this action is waiting.
	 *
	 * @return true if this action is ready for its {@link com.quest.glue.api.services.activities.ActivityAction#perform()}
	 *          method to be called and false if the action needs to wait before
	 *          executing.
	 */
	public boolean isReady();
}
