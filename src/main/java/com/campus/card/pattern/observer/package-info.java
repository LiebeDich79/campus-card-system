/**
 * 观察者模式（Observer，行为型模式）。
 *
 * <p>一笔交易成功落库后，需要通知多个下游模块：对账、统计、消息推送、积分流水。
 * 这些下游模块彼此独立、可能继续扩展。本包以 {@code TransactionSubject} 维护观察者列表，
 * {@code TransactionEventPublisher} 在数据库事务提交后发布事件（避免"事务未提交就通知"导致的不一致），
 * 各 {@code *Observer}（统计 / 对账 / 积分 / 通知）各自响应，互不影响、可独立增删。</p>
 */
package com.campus.card.pattern.observer;
