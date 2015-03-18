package info.xtern.management.monitoring.impl;

import info.xtern.common.EventHandler;
import info.xtern.management.monitoring.PlainTaskTracker;

public class LocalThreadTracker extends DelayQueueBasedTracker implements
    PlainTaskTracker {

    private final long baseDelayMillis;

    public LocalThreadTracker(EventHandler<TaskDelayed> hangHandler,
            EventHandler<TaskDelayed> unhangHandler, long baseDelayMillis) {
        super(hangHandler, unhangHandler);
        this.baseDelayMillis = baseDelayMillis;
    }

    @Override
    public void start() {
        super.submit(Thread.currentThread(), baseDelayMillis);
    }

    @Override
    public void stop() {
        super.remove(Thread.currentThread());
    }

}
