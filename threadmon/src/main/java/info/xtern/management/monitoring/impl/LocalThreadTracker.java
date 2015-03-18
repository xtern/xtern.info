package info.xtern.management.monitoring.impl;

import info.xtern.common.EventHandler;
import info.xtern.management.monitoring.SimpleTaskTracker;

/**
 * Decorator for {@link DelayQueueBasedTracker}, implements
 * {@link SimpleTaskTracker} interface
 * 
 * @author pereslegin-pa
 *
 */
public class LocalThreadTracker extends DelayQueueBasedTracker implements
    SimpleTaskTracker {

    private final long baseDelayMillis;

    public LocalThreadTracker(EventHandler<TaskDelayed> hangHandler,
            EventHandler<TaskDelayed> unhangHandler, long baseDelayMillis) {
        super(hangHandler, unhangHandler);
        this.baseDelayMillis = baseDelayMillis;
    }

    @Override
    public void startTracking() {
        super.submit(Thread.currentThread(), baseDelayMillis);
    }

    @Override
    public void stopTracking() {
        super.remove(Thread.currentThread());
    }

}
