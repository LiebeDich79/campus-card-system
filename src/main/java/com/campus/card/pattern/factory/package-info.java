/**
 * 工厂模式（Factory Method，创建型模式）。
 *
 * <p>交易分为充值、消费、退款等多种类型，每类交易对象的创建逻辑不同且可能继续扩展。
 * 本包定义 {@code TransactionHandler} 产品接口与 {@code TransactionFactory} 工厂，
 * 各交易类型实现自己的 Handler（{@code RechargeHandler} / {@code ConsumeHandler} / {@code RefundHandler}）。
 * 工厂通过 Spring 注入自动收集所有 Handler 形成注册表，新增交易类型无需改动任何调用方。</p>
 */
package com.campus.card.pattern.factory;
