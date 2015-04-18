package info.xtern.management.monitoring;

/**
 * Handles "unhang" event
 * 
 * @author pereslegin pavel
 *
 * @param <T>
 */
public interface UnHangEventHandler<T> {

    /**
     * Handles unhanged task (that was reported as "hanged" previously
     * 
     * @param task
     *            unhanged task
     * @param leftHangCount
     *            count of still hanged tasks (0 - no hanged tasks)
     */
    public void onEvent(T task, int leftHangCount);

}
