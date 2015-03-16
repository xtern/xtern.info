/**
 * 
 */
package info.xtern.management.monitoring;

/**
 * @author stash
 *
 */
public interface TaskTracker<T> {
    
    public void submit(T t, long delay);
    
    public boolean remove(T t);
    
    public LifeCycle getController();
}
