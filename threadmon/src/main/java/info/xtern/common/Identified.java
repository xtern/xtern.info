package info.xtern.common;

/**
 * Represent identified (by some identifier) object<br>
 * This object supposed to be found in hash table, that's way, implementation
 * <b>MUST</b> override identity {@link java.lang.Object#hashCode}
 * 
 * @author pereslegin pavel
 *
 * @param <T>
 */
public interface Identified<T> {
    /**
     * 
     * @return identifier of object
     */
    public T getId();
}
