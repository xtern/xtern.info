/**
 * 
 */
package info.xtern.management.monitoring.impl;

import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.TrackerController;

/**
 * @author stash
 *
 */
public class ThreadMonitor implements LifeCycle, Runnable {

    private final Thread thread;
    
    private volatile boolean shutdown = false;
    
    private final TrackerController taskTrackerController;
    
    public ThreadMonitor(TrackerController taskTrackerController) {
        
        // very bad style
        this.thread = new Thread(this);
        this.taskTrackerController = taskTrackerController;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            while (!shutdown && !Thread.currentThread().isInterrupted()) {
                taskTrackerController.trackTasks();
            } 
        } catch (InterruptedException e) {
            if (!shutdown)
                e.printStackTrace();
                //SBRFLogger.getLogger(ThreadMonitor.class).warn(Thread.currentThread().getName() + " was interrupted and will be terminated");
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
        // TODO Auto-generated method stub
        shutdown = true;
        thread.interrupt();
    }

}
