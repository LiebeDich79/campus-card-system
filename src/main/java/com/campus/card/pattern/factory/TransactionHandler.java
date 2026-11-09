package com.campus.card.pattern.factory;

import com.campus.card.dto.TransactionContext;
import com.campus.card.entity.Transaction;

/**
 * 【工厂模式 · Product】交易处理器接口。
 * 充值 / 消费 / 退款是三个具体产品，新增交易类型只需新增实现类并交给 Spring 注册。
 */
public interface TransactionHandler {
    /** 该处理器能处理的交易类型，作为工厂注册表的 key。 */
    String supportedType();

    /** 由工厂统一调用，返回构造完成的交易对象。 */
    Transaction handle(TransactionContext context);
}
