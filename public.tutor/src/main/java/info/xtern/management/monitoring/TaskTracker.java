/**
 * 
 */
package info.xtern.management.monitoring;

import info.xtern.common.LifeCycle;

/**
 * @author stash
 *
 */
public interface TaskTracker<T> {
    
    public void submit(T t, long delay);
    
    public boolean remove(T t);
    
    public LifeCycle getController();
}
