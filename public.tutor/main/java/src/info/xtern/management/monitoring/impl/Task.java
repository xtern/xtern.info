/**
 * 
 */
package info.xtern.management.monitoring.impl;

/**
 * Базовая обертка объекта, реализующая hashcode/equals по long id, для удаления
 * объекта по id из очереди
 * 
 * @author sbt-pereslegin-pa
 */
public class Task {
    
    protected final long taskId;
    
    public Task(long taskId) {
        this.taskId = taskId;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (taskId ^ (taskId >>> 32));
        return result;
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
        return taskId == ((Task) obj).taskId;
    }
}
