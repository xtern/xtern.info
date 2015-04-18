package info.xtern.management.monitoring.impl;

import info.xtern.common.LifeCycle;
import info.xtern.common.PrototypeFactory;
import info.xtern.management.monitoring.HangEventHandler;
import info.xtern.management.monitoring.SimpleTaskTracker;
import info.xtern.management.monitoring.TrackerControllerSync;
import info.xtern.management.monitoring.UnHangEventHandler;

public class CurrentThreadFixedDelayConcurrentTracking implements
        SimpleTaskTracker, TrackerControllerSync {

    private final FixedDelayConcurrentLinkedQueueBasedSimpleTracker<TaskDelayed> trackingBase;

    class Factory implements PrototypeFactory<TaskDelayed> {

        @Override
        public TaskDelayed newInstance(TaskDelayed prototype) {
            return new TaskDelayed(prototype);
        }

    }

    private final long delay;

    public CurrentThreadFixedDelayConcurrentTracking(long delay,
            HangEventHandler<TaskDelayed> hangHandler,
            UnHangEventHandler<TaskDelayed> unhangHandler) {
        this.trackingBase = new FixedDelayConcurrentLinkedQueueBasedSimpleTracker<TaskDelayed>(
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
