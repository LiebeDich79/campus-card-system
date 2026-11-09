package com.campus.card.pattern.observer;

/** 【观察者模式 · Observer】交易完成事件的订阅者接口。 */
public interface TransactionObserver {
    void onEvent(TransactionEvent event);
}
