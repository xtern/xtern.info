/**
 * 
 */
package info.xtern.management.monitoring.impl;

import info.xtern.common.PrototypeFactory;
import info.xtern.common.Identified;
import info.xtern.management.monitoring.HangEventHandler;
import info.xtern.management.monitoring.UnHangEventHandler;

import java.lang.Thread.State;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author pereslegin pavel
 *
 */
public class FixedDelayConcurrentLinkedQueueBasedSimpleTracker<E extends Delayed & Identified<Long>>  {

    private volatile Thread localThread;
    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();
    
    private final HangEventHandler<E> hangHandler;
    
    private final UnHangEventHandler<E> unhangHandler;
    
    private final ConcurrentMap<Long, E> hangMap = new ConcurrentHashMap<Long, E>();
    
    private final PrototypeFactory<E> fact;
    
    FixedDelayConcurrentLinkedQueueBasedSimpleTracker(HangEventHandler<E> hangHandler,
    		UnHangEventHandler<E> unhangHandler, PrototypeFactory<E> fact) {
        this.hangHandler = hangHandler;
        this.unhangHandler = unhangHandler;
        this.fact = fact;
    }
    
    public void submit(E t) {
        Thread th = localThread;
        if (th == null)
            throw new IllegalStateException("Task tracker MUST BE started before workers");
        queue.add(t);
        if (th.getState() == State.WAITING) {
            LockSupport.unpark(th);
        }
    }
    
    public void remove(E t) {
        // spin forever
        while (!queue.remove(t)) ;
//        {
//            System.out.println("Critical error - unable to remove: " + t + " (queue.size = " + queue.size())");
//        };
        
        if ((t = hangMap.remove(t.getId())) != null)
            unhangHandler.onEvent(t, hangMap.size());
    }
    
    public void track() throws InterruptedException {
        Thread local = localThread = Thread.currentThread();
        E t;
        boolean event = false;
        for (; !local.isInterrupted();) {

            while ((t = queue.peek()) == null) {
                LockSupport.park(local);
            }
            long timeout = t.getDelay(TimeUnit.NANOSECONDS);
            if (timeout > 0 && queue.peek() == t) {
                //
                LockSupport.parkNanos(timeout);
            }

            if (t.getDelay(TimeUnit.NANOSECONDS) <= 0) { // spurious wake up test
                if (queue.peek() == t) {
                    if (queue.poll() != t) { // someone other was peeked - inconsistent state
                        throw new IllegalStateException("State failed");
                    }
                    event = true;
                }
            }
            if (event) {
                event = false;
                if (unhangHandler != null) {
                    hangMap.putIfAbsent(t.getId(), t);
                }
                hangHandler.onEvent(t);
                // resubmit task
                submit(fact.newInstance(t));

            }
        }
        if (Thread.interrupted())
            throw new InterruptedException("handling interrupted");
    }
}
