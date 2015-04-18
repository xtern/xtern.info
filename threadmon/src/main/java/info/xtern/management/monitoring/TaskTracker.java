/**
 * 
 */
package info.xtern.management.monitoring;

import info.xtern.common.LifeCycle;

/**
 * @author pereslegin-pa
 *
 */
public interface TaskTracker<T> {

    public void submit(T t, long delay);

    public boolean remove(T t);

    public LifeCycle getController();
}
