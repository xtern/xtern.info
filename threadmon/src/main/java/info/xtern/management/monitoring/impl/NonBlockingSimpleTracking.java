package info.xtern.management.monitoring.impl;

import info.xtern.common.LifeCycle;
import info.xtern.common.PrototypeFactory;
import info.xtern.management.monitoring.HangEventHandler;
import info.xtern.management.monitoring.SimpleTaskTracker;
import info.xtern.management.monitoring.TrackerControllerSync;
import info.xtern.management.monitoring.UnHangEventHandler;
/**
 * 
 * @author pereslegin pavel
 *
 */
public class NonBlockingSimpleTracking implements
        SimpleTaskTracker, TrackerControllerSync {

    private final ConcurrentDequeBasedSimpleTracker<TaskDelayed> trackingBase;

    private final long delay;
    
    class Factory implements PrototypeFactory<TaskDelayed> {

        @Override
        public TaskDelayed newInstance(TaskDelayed prototype) {
            return new TaskDelayed(prototype);
        }

    }

    public NonBlockingSimpleTracking(long delay,
            HangEventHandler<TaskDelayed> hangHandler,
            UnHangEventHandler<TaskDelayed> unhangHandler) {
        this.trackingBase = new ConcurrentDequeBasedSimpleTracker<TaskDelayed>(
                hangHandler, unhangHandler, new Factory());
        this.delay = delay;
    }

    @Override
    public void startTracking() {
        trackingBase.submit(new TaskDelayed(Thread.currentThread(), delay));
    }

    @Override
    public void stopTracking() {
        trackingBase.remove(new TaskDelayed(Thread.currentThread(), delay));
    }

    @Override
    public LifeCycle getController() {
        return new ThreadMonitor(this);
    }

    @Override
    public void trackTasksSync() throws InterruptedException {
        trackingBase.track();
    }

}
