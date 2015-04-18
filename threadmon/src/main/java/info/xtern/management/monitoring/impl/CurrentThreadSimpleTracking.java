package info.xtern.management.monitoring.impl;

import info.xtern.management.monitoring.HangEventHandler;
import info.xtern.management.monitoring.SimpleTaskTracker;
import info.xtern.management.monitoring.UnHangEventHandler;

/**
 * Decorator for {@link DelayQueueBasedTracker}, implements
 * {@link SimpleTaskTracker} interface
 * 
 * @author pereslegin pavel
 *
 */
public class CurrentThreadSimpleTracking extends DelayQueueBasedTracker
        implements SimpleTaskTracker {

    private final long baseDelayMillis;

    public CurrentThreadSimpleTracking(long baseDelayMillis,
            HangEventHandler<TaskDelayed> hangHandler,
            UnHangEventHandler<TaskDelayed> unhangHandler) {
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
