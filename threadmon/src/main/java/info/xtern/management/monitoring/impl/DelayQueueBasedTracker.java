/**
 * 
 */
package info.xtern.management.monitoring.impl;

import info.xtern.common.EventHandler;
import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.TaskTracker;
import info.xtern.management.monitoring.TrackerControllerSync;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * Optimistic tracking of "hanging" tasks, based on concurrent priority blocking
 * queue.<br>
 * <b>Warning</b> there is no explicit HB between {@link#trackTasksSync}/
 * {@link#submit}/{@link#remove}
 * 
 * @author sbt-pereslegin-pa
 * 
 * @see java.util.concurrent.DelayQueue
 */
public class DelayQueueBasedTracker implements TaskTracker<Thread>,
        TrackerControllerSync {
    //
    private static final long SPIN_MAX_NANOS = TimeUnit.SECONDS.convert(1,
            TimeUnit.NANOSECONDS);

    /**
     * In order to be able to get information about the nature of the problem,
     * timeout interval, etc. after removal
     */
    private final ConcurrentMap<Long, TaskDelayed> hangMap = new ConcurrentHashMap<Long, TaskDelayed>();

    /**
     * Queue for timeout handling
     */
    private final DelayQueue<TaskDelayed> taskQueue = new DelayQueue<TaskDelayed>();

    /**
     * Will be invoked if timeout occurs
     */
    private final EventHandler<TaskDelayed> onHangHandler;

    /**
     * Will be invoked when hanged task removed
     */
    private final EventHandler<TaskDelayed> onUnHangHandler;

    /**
     * Constructs monitoring
     * 
     * @param onHangHandler
     *            handler for timeout processing
     * @param onUnHangHandler
     *            handler for reporting, that hanged task completed
     */
    public DelayQueueBasedTracker(EventHandler<TaskDelayed> onHangHandler,
            EventHandler<TaskDelayed> onUnHangHandler) {
        if (onHangHandler == null && onUnHangHandler == null)
            throw new NullPointerException(
                    "\"Hang\" and/or \"unhang\" handlers must be set!");

        this.onHangHandler = onHangHandler;
        this.onUnHangHandler = onUnHangHandler;
    }

    @Override
    public boolean remove(Thread thread) {
        TaskDelayed taskDelayed;
        Task taskId;
        boolean removed;

        if (!(removed = taskQueue.remove(taskId = new Task(thread.getId())))) {
            removed = removeWithSpin(SPIN_MAX_NANOS, taskId);
        }

        // checking what task has been completed, it it was reported as hang we
        // must perform "unhang" action
        if ((taskDelayed = hangMap.remove(thread.getId())) != null) {
            onUnHangHandler.onEvent(taskDelayed);
        }
        return removed;
    }

    @Override
    public void submit(Thread t, long millsWait) {
        taskQueue.add(new TaskDelayed(t, millsWait));
    }

    @Override
    public void trackTasksSync() throws InterruptedException {
        TaskDelayed task;

        while (!Thread.currentThread().isInterrupted()
                && (task = taskQueue.take()) != null) {
            //
            if (onHangHandler != null)
                onHangHandler.onEvent(task);

            if (onUnHangHandler != null)
                hangMap.putIfAbsent(task.getTaskId(), task);

            //
            taskQueue.add(new TaskDelayed(task));
        }
    }

    @Override
    public LifeCycle getController() {
        return new ThreadMonitor(this);
    }

    private boolean removeWithSpin(long spinMax, Task task) {
        long spinTill = System.nanoTime() + spinMax;
        boolean consistent = false;
        while (!(consistent = taskQueue.remove(task))
                && spinTill > System.nanoTime())
            ; // spin
        return consistent;
    }
}
