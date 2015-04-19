/**
 * 
 */
package info.xtern.management.monitoring.impl;

import info.xtern.common.Identified;
import info.xtern.common.PrototypeFactory;
import info.xtern.management.monitoring.HangEventHandler;
import info.xtern.management.monitoring.UnHangEventHandler;

import java.lang.Thread.State;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author pereslegin pavel
 *
 */
public class ConcurrentDequeBasedSimpleTracker<E extends Delayed & Identified<Long>>  {

    private volatile Thread localThread;
    private final ConcurrentLinkedDeque<E> deque = new ConcurrentLinkedDeque<>();
    
    private final HangEventHandler<E> hangHandler;
    
    private final UnHangEventHandler<E> unhangHandler;
    
    private final ConcurrentMap<Long, E> hangMap = new ConcurrentHashMap<Long, E>();
    
    private final PrototypeFactory<E> fact;
    
    ConcurrentDequeBasedSimpleTracker(HangEventHandler<E> hangHandler,
            UnHangEventHandler<E> unhangHandler, PrototypeFactory<E> fact) {
        this.hangHandler = hangHandler;
        this.unhangHandler = unhangHandler;
        this.fact = fact;
    }
    
    public void submit(E t) {
        Thread th = localThread;
        if (th == null)
            throw new IllegalStateException("Task tracker MUST BE started before workers");
        
        deque.offerLast(t);
        
        if (th.getState() == State.WAITING) {
            LockSupport.unpark(th);
        }
    }
    
    public void remove(E t) {
        // for a short period of time task can be extracted by tracking-thread
        // and not found until tracking-thread resubmit it
        while (!deque.remove(t));

        if ((t = hangMap.remove(t.getId())) != null)
            unhangHandler.onEvent(t, hangMap.size());
    }
    
    public void track() throws InterruptedException {
        localThread = Thread.currentThread();
        Thread local = Thread.currentThread();
        E t;
        boolean event = false;
        while (!local.isInterrupted()) {

            while ((t = deque.peek()) == null) {
                LockSupport.park(local);
            }
            long timeout = t.getDelay(TimeUnit.NANOSECONDS);
            if (timeout > 0 && deque.peek() == t) {
                //
                LockSupport.parkNanos(timeout);
            }

            if (t.getDelay(TimeUnit.NANOSECONDS) <= 0) { // spurious wake up test
                if (deque.peek() == t) {
                    // testing that element was not removed
                    if (deque.poll() != t) {
                        // if someone other was polled - reversing (this is
                        // possible because we have a single thread for tracking)
                        deque.addFirst(t);
                    }
                    else {
                        event = true;
                    }
                }
            }
            if (event) {
                event = false;
                // resubmit task
                submit(fact.newInstance(t));

                if (unhangHandler != null) {
                    hangMap.putIfAbsent(t.getId(), t);
                }
                hangHandler.onEvent(t);
            }
        }
        if (Thread.interrupted()) // handling thread interruption
            throw new InterruptedException("Thread was interrupted");
    }
}
