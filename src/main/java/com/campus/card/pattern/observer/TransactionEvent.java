package com.campus.card.pattern.observer;

import com.campus.card.entity.Transaction;

import java.util.Date;

/** 【观察者模式 · Event】事件对象，携带交易聚合根与发生时间。 */
public class TransactionEvent {
    private final Transaction transaction;
    private final Date occurredAt;

    public TransactionEvent(Transaction transaction) {
        this.transaction = transaction;
        this.occurredAt = new Date();
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }
}
