package info.xtern.management.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import info.xtern.common.EventHandler;
import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.impl.LocalThreadTracker;
import info.xtern.management.monitoring.impl.TaskDelayed;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
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
    
    private static final int MULTIPLIER = 5;
    
    private static final int THREADS_COUNT = 10;
    
    private static final long SHORT_LIVE_TASK_INTERVAL = 100;
    
    private static final long MAX_LIVE_TASK_INTERVAL = 200;
    
    private static final long HANG_MULTIPLIER = 6;
    
    private static final long COMPUTATION_INTERVAL = 4;
    
    private static final long HANG_LIVE_TASK_INTERVAL = MAX_LIVE_TASK_INTERVAL * HANG_MULTIPLIER + (COMPUTATION_INTERVAL * HANG_MULTIPLIER);
    
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
        
        private final CountDownLatch endLatch;
        
        WorkThread(TrackingLifeCycle tracker, long sleepTime, CountDownLatch latch, CountDownLatch endLatch, int tryCount) {
            this.tracker = tracker;
            this.sleepTime = sleepTime;
            this.latch = latch;
            this.tryCount = tryCount;
            this.endLatch = endLatch;
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
                // copying to exception handler (will be validated after completion)
                throw new RuntimeException(e);
            } finally {
                tracker.stopTracking();
            }
            endLatch.countDown();
        }
    }
    

    class TestThreadsLastExceptionHandler implements UncaughtExceptionHandler {

        Thread t;
        Throwable e;
        
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            this.t = t;
            this.e = e;
            e.printStackTrace(System.err);
        }
        
    }
    
    private static boolean isThreadIdentifiersFitsRange(int range, Thread[]... threadArrays) {
        for (int i = 0; i < threadArrays.length; i++)
            for (int j = 0; j < threadArrays[i].length; j++)
                if (threadArrays[i][j].getId() >= range)
                    return false;
        return true;
    }

    
    @Test
    public void testHangTrackingWithoutMainLock() throws Throwable {
        AtomicInteger totalUnhangCounter = new AtomicInteger();
        AtomicInteger totalhangCounter = new AtomicInteger();
        
        int[] countersArray = new int[1000];
        SimpleTaskTracker tracker = new LocalThreadTracker(new HangHandler(
                totalhangCounter, countersArray), new HangHandler(
                totalUnhangCounter, countersArray), MAX_LIVE_TASK_INTERVAL);
        testHangTaskTracking(tracker, countersArray, totalhangCounter, totalUnhangCounter);
    }
    
//    @Test
//    public void testHangTrackingWithMainLock() throws Throwable {
//        AtomicInteger totalUnhangCounter = new AtomicInteger();
//        AtomicInteger totalhangCounter = new AtomicInteger();
//        
//        int[] countersArray = new int[1000];
//        SimpleTaskTracker tracker = new LocalThreadTracker(new HangHandler(
//                totalhangCounter, countersArray), new HangHandler(
//                totalUnhangCounter, countersArray), MAX_LIVE_TASK_INTERVAL);
//        testHangTaskTracking(tracker, countersArray, totalhangCounter, totalUnhangCounter);
//    }

    public void testHangTaskTracking(SimpleTaskTracker tracker, int[] countersArray, AtomicInteger totalhangCounter, AtomicInteger totalUnhangCounter) throws Throwable {
        
        TestThreadsLastExceptionHandler exceptionHandler = new TestThreadsLastExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
        
        System.out.printf("Max time for executing task: %d ms%n%n", MAX_LIVE_TASK_INTERVAL);
        System.out.printf(" Normal task delay interval: %d ms%n", SHORT_LIVE_TASK_INTERVAL);
        System.out.printf("   Hang task delay interval: %d ms%n%n", HANG_LIVE_TASK_INTERVAL);
        System.out.printf("Expected unhang count (removed earlier hanged tasks): %d(repeat count) x %d(threads count) = %d%n%n", MULTIPLIER, THREADS_COUNT, THREADS_COUNT * MULTIPLIER);
        System.out.printf("Expected hang count (tracker detected task's hanging): %d(hang interval / max interval) x %d(repeat count) x %d(threads count) = %d%n%n", HANG_MULTIPLIER, MULTIPLIER, THREADS_COUNT, HANG_MULTIPLIER * THREADS_COUNT * MULTIPLIER);

        
        LifeCycle controller = tracker.getController();
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREADS_COUNT * 2);
        
        final WorkThread[] normal = new WorkThread[THREADS_COUNT];
        for (int i = 0; i < normal.length; i++) {
            normal[i] = new WorkThread(tracker, SHORT_LIVE_TASK_INTERVAL, startLatch, endLatch, MULTIPLIER);
        }
        
        final WorkThread[] hang = new WorkThread[THREADS_COUNT];
        for (int i = 0; i < hang.length; i++) {
            hang[i] = new WorkThread(tracker, HANG_LIVE_TASK_INTERVAL, startLatch, endLatch, MULTIPLIER);
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
            startLatch.countDown();
            
            // awaiting threads termination
            endLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            System.out.println("No live test threads -> stopping tracking");
            controller.stop();
        }
        if (exceptionHandler.e != null) {
            throw exceptionHandler.e;
        }
        int unhangCount = THREADS_COUNT * MULTIPLIER;
        assertEquals("Total unhang count must be " + unhangCount, unhangCount, totalUnhangCounter.get());
        
        long hangCount = THREADS_COUNT * MULTIPLIER * HANG_MULTIPLIER;
        assertEquals("Total hang count must be " + hangCount, hangCount, totalhangCounter.get());

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
