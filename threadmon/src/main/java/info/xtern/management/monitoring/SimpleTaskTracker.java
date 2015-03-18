/**
 * 
 */
package info.xtern.management.monitoring;

import info.xtern.common.LifeCycle;

/**
 * Simplified task tracking interface (for current thread tracking)
 * 
 * @author pereslegin-pa
 *
 */
public interface SimpleTaskTracker extends TrackingLifeCycle {
    
    public LifeCycle getController();

}
