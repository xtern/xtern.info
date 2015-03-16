package info.xtern.management.monitoring.impl;

import info.xtern.common.EventHandler;
import info.xtern.common.LifeCycle;

public class CurrentThreadTracker extends DelayQueueBasedTracker implements
        LifeCycle {

    private EventHandler<TaskDelayed> hangHandler;
    private EventHandler<TaskDelayed> unhangHandler;
    
    private final long baseDelayMillis;
    
    public CurrentThreadTracker(long baseDelayMillis) {
        super(null, null);
        this.baseDelayMillis = baseDelayMillis;
        // TODO Auto-generated constructor stub
    }

    
    
    @Override
    public void start() {
        // TODO Auto-generated method stub
        super.submit(Thread.currentThread(), baseDelayMillis);
    }

    @Override
    public void stop() {
        super.remove(Thread.currentThread());
    }

}
