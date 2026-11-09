package com.campus.card.pattern.observer;

import com.campus.card.entity.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 【观察者模式 · 发布时机控制】把通知注册到事务同步器，
 * 仅在事务提交成功（afterCommit）后才发布事件，避免事务回滚产生"脏通知"。
 */
@Component
public class TransactionEventPublisher {
    private final TransactionSubject subject;

    public TransactionEventPublisher(TransactionSubject subject) {
        this.subject = subject;
    }

    // 一开始是在事务里直接 publish，回滚后通知表里还留着推送记录；改到 afterCommit 才干净
    public void publishAfterCommit(final Transaction tx) {
        final TransactionEvent event = new TransactionEvent(tx);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    subject.publish(event);
                }
            });
        } else {
            subject.publish(event);
        }
    }
}
