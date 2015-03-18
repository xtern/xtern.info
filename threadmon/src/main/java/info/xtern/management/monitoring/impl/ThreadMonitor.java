/**
 * 
 */
package info.xtern.management.monitoring.impl;

import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.TrackerControllerSync;

/**
 * Just a simple runnable thread wrapper for interact with task tracker queue
 * 
 * @author pereslegin-pa
 *
 */
public class ThreadMonitor implements LifeCycle, Runnable {

    private final Thread thread;
    
    private final TrackerControllerSync taskTrackerController;
    
    private volatile boolean shutdown = false;
    
    public ThreadMonitor(TrackerControllerSync taskTrackerController) {
        // bad style
        this.thread = new Thread(this, "Thread monitor");
        this.thread.setDaemon(true);
        this.taskTrackerController = taskTrackerController;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            taskTrackerController.trackTasksSync(); 
        } catch (InterruptedException e) {
            if (!shutdown)
                throw new IllegalStateException("Thread " + Thread.currentThread().getName() + " was interrupted and will terminate");
        }
    }

    /* (non-Javadoc)
     * @see info.xtern.management.monitoring.LifeCycle#start()
     */
    @Override
    public void start() {
        thread.start();
    }

    /* (non-Javadoc)
     * @see info.xtern.management.monitoring.LifeCycle#stop()
     */
    @Override
    public void stop() {
        shutdown = true;
        thread.interrupt();
    }

}
