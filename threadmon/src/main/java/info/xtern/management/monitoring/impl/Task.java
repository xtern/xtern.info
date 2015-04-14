package info.xtern.management.monitoring.impl;

import info.xtern.common.Identified;

/**
 * Wrapper implements hashCode/equals to allow identify object by long
 * identifier.
 * 
 * @author pereslegin pavel
 * 
 * @see java.lang.Long
 */
public class Task implements Identified<Long> {
    
    private final Long ident;

    public Task(Long taskId) {
        this.ident = taskId;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return ident.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Task))
            return false;
        return ident == ((Task) obj).ident;
    }

	@Override
	public Long getId() {
		return this.ident;
	}
}
