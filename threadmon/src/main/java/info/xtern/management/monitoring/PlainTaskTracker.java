/**
 * 
 */
package info.xtern.management.monitoring;

import info.xtern.common.LifeCycle;

/**
 * @author pereslegin-pa
 *
 */
public interface PlainTaskTracker extends LifeCycle {
    
    public LifeCycle getController();

}
