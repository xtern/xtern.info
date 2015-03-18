package info.xtern.management.monitoring;

import static org.junit.Assert.assertEquals;
import info.xtern.common.EventHandler;
import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.impl.LocalThreadTracker;
import info.xtern.management.monitoring.impl.TaskDelayed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * This test can failed, in case when {@link #COMPUTATION_INTERVAL} will be
 * insufficient for current hardware spec (speed, arch, load), if this happened,
 * you can increment it :)
 * 
 * @author pereslegin-pa
 *
 */
public class TaskTrackerTest {
    
    private class HangHandler implements EventHandler<TaskDelayed> {

        final AtomicInteger counter;
        
        HangHandler(AtomicInteger counter) {
            this.counter = counter;
        }
        
        @Override
        public void onEvent(TaskDelayed t) {
            counter.incrementAndGet();
        }
        
    }
    
    static class WorkThread extends Thread {
        
        private final TrackingLifeCycle tracker;
        
        private final long sleepTime;
        
        private final CountDownLatch latch;
        
        private final int tryCount;
        
        WorkThread(TrackingLifeCycle tracker, long sleepTime, CountDownLatch latch, int tryCount) {
            this.tracker = tracker;
            this.sleepTime = sleepTime;
            this.latch = latch;
            this.tryCount = tryCount;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < tryCount; i++)
            try {
                latch.await();
                tracker.startTracking();
                // task hanging imitation
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } finally {
                tracker.stopTracking();
            }
        }
    }
    
    private static boolean isSomeoneAlive(Thread[]... threadArrays) {
        for (int i = 0; i < threadArrays.length; i++)
            for (int j = 0; j < threadArrays[i].length; j++)
                if (threadArrays[i][j].isAlive())
                    return true;
        return false;
    }
    
    
    
    private static final int MULTIPLIER = 5;
    
    private static final int THREADS_COUNT = 10;
    
    private static final long SHORT_LIVE_TASK_INTERVAL = 100;
    
    private static final long MAX_LIVE_TASK_INTERVAL = 200;
    
    private static final long HANG_MULTIPLIER = 6;
    
    private static final long COMPUTATION_INTERVAL = 4 * MULTIPLIER;
    
    private static final long HANG_LIVE_TASK_INTERVAL = MAX_LIVE_TASK_INTERVAL * HANG_MULTIPLIER + (COMPUTATION_INTERVAL * HANG_MULTIPLIER);

    
    @Test
    public void testHangTaskTracking() throws InterruptedException {
        
        AtomicInteger totalUnhangCounter = new AtomicInteger();
        AtomicInteger totalhangCounter = new AtomicInteger();
        SimpleTaskTracker tracker = new LocalThreadTracker(new HangHandler(totalhangCounter), new HangHandler(totalUnhangCounter), MAX_LIVE_TASK_INTERVAL);
        
        LifeCycle controller = tracker.getController();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        final WorkThread[] normal = new WorkThread[THREADS_COUNT];
        for (int i = 0; i < normal.length; i++) {
            normal[i] = new WorkThread(tracker, SHORT_LIVE_TASK_INTERVAL, latch, MULTIPLIER);
        }
        
        final WorkThread[] hang = new WorkThread[THREADS_COUNT];
        for (int i = 0; i < hang.length; i++) {
            hang[i] = new WorkThread(tracker, HANG_LIVE_TASK_INTERVAL, latch, MULTIPLIER);
        }
        for (int i = 0; i < THREADS_COUNT; i++) {
            normal[i].start();
            hang[i].start();
        }
        
        try {
            // 1. starting monitoring thread
            controller.start();
            
            // 2. starting all threads
            latch.countDown();
            
            // 3. awaiting threads termination
            while (isSomeoneAlive(normal, hang))
                TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("No live test threads -> interrupting tracking");
            controller.stop();
        }
        assertEquals("Total unhang count must be " + THREADS_COUNT * MULTIPLIER, THREADS_COUNT * MULTIPLIER, totalUnhangCounter.get());
        
        assertEquals("Total hang count must be " + (THREADS_COUNT * MULTIPLIER * HANG_MULTIPLIER), (THREADS_COUNT * MULTIPLIER * HANG_MULTIPLIER) , totalhangCounter.get());
    }

}
