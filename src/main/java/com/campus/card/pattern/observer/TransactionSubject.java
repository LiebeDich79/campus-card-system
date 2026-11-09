package com.campus.card.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 【观察者模式 · Subject】维护订阅者列表并在状态变化时逐一通知。
 * 使用 CopyOnWriteArrayList 支持运行期动态增删订阅者且线程安全；
 * 单个订阅者失败被隔离，不影响主交易与其他订阅者。
 */
@Component
public class TransactionSubject {
    private static final Logger log = LoggerFactory.getLogger(TransactionSubject.class);
    private final List<TransactionObserver> observers = new CopyOnWriteArrayList<TransactionObserver>();

    public TransactionSubject(List<TransactionObserver> initial) {
        observers.addAll(initial);
    }

    public void attach(TransactionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(TransactionObserver observer) {
        observers.remove(observer);
    }

    public int count() {
        return observers.size();
    }

    public void publish(TransactionEvent event) {
        for (TransactionObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                // 通知类故障不应导致交易回滚，记录日志后继续通知其余订阅者
                log.warn("观察者 {} 处理事件失败: {}", observer.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
