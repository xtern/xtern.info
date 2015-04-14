package info.xtern.common;

public interface PrototypeFactory<T> {
	/**
	 * Constructs new object (with some custom logic) using "base" as prototype
	 * 
	 * @param proto
	 *            prototype to construct new instance
	 * @return new instance of T
	 */
	public T newInstance(T proto);
}
