/**
 * 
 */
package info.xtern.management.monitoring.impl;

import info.xtern.common.EventHandler;
import info.xtern.common.LifeCycle;
import info.xtern.management.monitoring.TaskTracker;
import info.xtern.management.monitoring.TrackerController;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;

/**
 * Оптимистичное отслеживание зависания выполнения задачи, базирующееся на
 * конкурентной очереди с приоритетами.<br>
 * <b>Warning</b> отсутствует HB между {@link#trackTasks} /{@link#submit}/
 * {@link#remove}, т.е. как минимум вероятна ситуация, когда в лог попадет
 * информация о том, что зависшая задача была удалена раньше, чем стек вызовов
 * состояния потока зависшей задачи, кроме того<br>
 * 
 * @author sbt-pereslegin-pa
 * 
 * @see java.util.concurrent.DelayQueue
 */
public class DelayQueueBasedTracker implements TaskTracker<Thread>, TrackerController {
        // 
        private static final long SPIN_MAX = 1000L;
        
        /**
         * Ассоциативная коллекция для хранения зависших задач
         */
        private final ConcurrentMap<Long, TaskDelayed> hangMap = new ConcurrentHashMap<Long, TaskDelayed>();
        
        /**
         * Очередь для обработки наступающих тайм-аутов (может переполниться, при
         * задании некорректных интервалов)
         */
        private final DelayQueue<TaskDelayed> taskQueue = new DelayQueue<TaskDelayed>();
        
        /**
         * Обработчик события, когда произошел таймаут ожидания выполнения задачи
         */
        private final EventHandler<TaskDelayed> onHangHandler;
        
        /**
         * Обработчик события, когда задача, которая выглядела завислей отвисла
         */
        private final EventHandler<TaskDelayed> onUnHangHandler;
        
        /**
         * Конструктриурует монтиринг выполнения задач
         * 
         * @param onHangHandler
         *            Обработчик события, когда произошел таймаут ожидания
         *            выполнения задачи
         * @param onUnHangHandler
         *            Обработчик события, когда задача, которая выглядела завислей
         *            отвисла
         */
        public DelayQueueBasedTracker(EventHandler<TaskDelayed> onHangHandler, EventHandler<TaskDelayed> onUnHangHandler) {
            this.onHangHandler = onHangHandler;
            this.onUnHangHandler = onUnHangHandler;
        }
        
        @Override
        public boolean remove(Thread thread) {
            TaskDelayed taskDelayed;
            Task taskId;
            boolean removed;
            if (!(removed = taskQueue.remove(taskId = new Task(thread.getId())))) {
                removed = removeWithSpin(SPIN_MAX, taskId);
            } 
            // проверка - какую задачу мы удалили, если задача считалась ранее
            // завсишей необходимо выполнить оповещение о том, что она завершилась
            if ((taskDelayed = hangMap.remove(thread.getId())) != null) {
                onUnHangHandler.onEvent(taskDelayed);
            }
            return removed;
        }

        @Override
        public void submit(Thread t, long millsWait) {
            taskQueue.add(new TaskDelayed(t, millsWait));
        }

        @Override
        public void trackTasks() throws InterruptedException {
            TaskDelayed task;
            while ( !Thread.currentThread().isInterrupted() && (task = taskQueue.take()) != null) {
                onHangHandler.onEvent(task);
                hangMap.putIfAbsent(task.getTaskId(), task);
                taskQueue.add(new TaskDelayed(task));
            }
        }

        private boolean removeWithSpin(long spinMax, Task task) {
            long spinTill = System.currentTimeMillis() + spinMax;
            boolean consistent = false;
            while (!(consistent = taskQueue.remove(task)) && spinTill > System.currentTimeMillis())
                ; // spin
            if (!consistent)
                return false;
            
            return true;
                //SBRFLogger.getLogger(PriorityBasedTracker.class).warn("WARNING! Inconsistent state of queue (probably)");
        }

        @Override
        public LifeCycle getController() {
            return new ThreadMonitor(this);
        }

}
