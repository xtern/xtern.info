package info.xtern.management.monitoring;

/**
 * Handles event, when previously seemed hung task completed
 * 
 * @author pereslegin pavel
 *
 * @param <T>
 */
public interface UnHangEventHandler<T> {

    /**
     * Handles event, when previously seemed hung task completed
     * 
     * @param task
     *            task that previously was seemed hung 
     * @param leftHangCount
     *            count of hung tasks (0 - no hung tasks)
     */
    public void onEvent(T task, int leftHangCount);

}
