package info.xtern.management.monitoring;

import static org.junit.Assert.*;
import info.xtern.common.EventHandler;
import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.impl.LocalThreadTracker;
import info.xtern.management.monitoring.impl.TaskDelayed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * This test can not pass due to insufficient {@link #COMPUTATION_INTERVAL} for
 * current hardware spec (speed, arch, load), if this happened, you can
 * increment it :)
 * 
 * @author pereslegin pavel
 */
public class TaskTrackerTest {
    
    
    
    private class HangHandler implements EventHandler<TaskDelayed> {

        final AtomicInteger counter;
        
        final int[] countersArray;
        
        HangHandler(AtomicInteger counter, int[] countersArray) {
            this.counter = counter;
            this.countersArray = countersArray;
        }
        
        @Override
        public void onEvent(TaskDelayed t) {
            counter.incrementAndGet();
            countersArray[(int) t.getTaskId()]++;
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
    
    private static boolean isThreadIdentifiersFitsRange(int range, Thread[]... threadArrays) {
        for (int i = 0; i < threadArrays.length; i++)
            for (int j = 0; j < threadArrays[i].length; j++)
                if (threadArrays[i][j].getId() >= range)
                    return false;
        return true;
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
        int[] countersArray = new int[1000];
        
        SimpleTaskTracker tracker = new LocalThreadTracker(new HangHandler(totalhangCounter, countersArray), new HangHandler(totalUnhangCounter, countersArray), MAX_LIVE_TASK_INTERVAL);
        
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
        
        // TODO
        if (!isThreadIdentifiersFitsRange(countersArray.length, normal, hang)) {
            fail("VM already created many threads ( > " + countersArray.length
                    + "), this test will not work properly in this environment and must be reorganized");
        }
        
        for (int i = 0; i < THREADS_COUNT; i++) {
            normal[i].start();
            hang[i].start();
        }
        
        try {
            // starting monitoring thread
            controller.start();
            
            // starting all threads
            latch.countDown();
            
            // awaiting threads termination
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

        for (int i = 0; i < THREADS_COUNT; i++) {
            // 
            int id = (int)normal[i].getId();
            assertEquals( "Task " + id + " should not hang", 0, countersArray[id]);
        }
        int hangPlusUnhangCount = (int) (MULTIPLIER * HANG_MULTIPLIER + MULTIPLIER) ;
        for (int i = 0; i < THREADS_COUNT; i++) {
            int id = (int)hang[i].getId();
            assertEquals( "Task " + id + " should hang " + (MULTIPLIER * HANG_MULTIPLIER) + " and unhang for " + MULTIPLIER, hangPlusUnhangCount, countersArray[id]);
        }
    }

}