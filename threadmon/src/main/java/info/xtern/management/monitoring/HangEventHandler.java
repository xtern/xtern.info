package info.xtern.management.monitoring;

/**
 * Handles task hanging event
 * 
 * @author pereslegin pavel
 *
 * @param <T>
 */
public interface HangEventHandler<T> {
    
	/**
	 * 
	 * @param task
	 */
    public void onEvent(T task);

}
