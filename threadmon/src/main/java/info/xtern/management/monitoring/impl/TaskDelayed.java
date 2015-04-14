package info.xtern.management.monitoring.impl;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


/**
 * Simple implementation for {@link java.util.concurrent.Delayed}
 * 
 * @author sbt-pereslegin-pa
 * 
 */
public class TaskDelayed extends Task implements Delayed {

    private final long delay;
    private final long startTime;
    private final long originalStartTime;
    private final Reference<Thread> threadRef;

    public TaskDelayed(Thread thread, long delay) {
        super(thread.getId());
        this.startTime = this.originalStartTime = System.currentTimeMillis();
        this.delay = delay;
        this.threadRef = new WeakReference<Thread>(thread);
    }

    protected TaskDelayed(TaskDelayed task) {
        super(task.getId());
        this.originalStartTime = task.getOriginalStartTime();
        this.delay = task.delay;
        this.threadRef = task.threadRef;

        this.startTime = System.currentTimeMillis();
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return 0;
        }
        
        if (o == null)
            throw new NullPointerException("Unable to compare with null");
        
        long diff = getDelay(TimeUnit.MILLISECONDS)
                - o.getDelay(TimeUnit.MILLISECONDS);
        
        return (diff == 0L ? 0 : (diff < 0L ? -1 : 1));
    }

    @Override
    public long getDelay(TimeUnit unit) {

        return unit.convert(delay - (System.currentTimeMillis() - startTime),
                TimeUnit.MILLISECONDS);
    }

    public Thread getThread() {
        return threadRef.get();
    }

    /**
     * 
     * @return превоначально заданное время ожидания
     */
    public long getOriginalDelay() {
        return delay;
    }

    /**
     * @return the originalStartTime
     */
    public long getOriginalStartTime() {
        return originalStartTime;
    }
}
