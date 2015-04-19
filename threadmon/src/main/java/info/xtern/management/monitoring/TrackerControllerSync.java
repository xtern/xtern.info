/**
 * 
 */
package info.xtern.management.monitoring;

/**
 * Allows monitoring thread track events from task tracker without any knowledge
 * about implementation
 * 
 * @author Pereslegin Pavel
 */
public interface TrackerControllerSync {
    /**
     * 
     * @throws InterruptedException
     *             if controller thread was interrupted
     */
    public void trackTasksSync() throws InterruptedException;
}
