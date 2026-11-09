package com.campus.card.pattern.observer;

import com.campus.card.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/** 【观察者模式 · ConcreteObserver ②】内存统计计数，供看板快速展示（可扩展为 Redis）。 */
@Component
public class StatisticsObserver implements TransactionObserver {
    private final AtomicLong totalTx = new AtomicLong(0);
    private volatile BigDecimal totalAmount = BigDecimal.ZERO;

    @Override
    public void onEvent(TransactionEvent event) {
        Transaction tx = event.getTransaction();
        totalTx.incrementAndGet();
        synchronized (this) {
            totalAmount = totalAmount.add(tx.getActualAmount());
        }
    }

    public long getTotalTx() {
        return totalTx.get();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
