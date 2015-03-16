package info.xtern.management.monitoring;

public interface EventHandler<T> {
    
    public void onEvent(T t);

}
