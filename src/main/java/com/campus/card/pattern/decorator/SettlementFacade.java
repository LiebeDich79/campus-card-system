package com.campus.card.pattern.decorator;

import com.campus.card.entity.Transaction;

/** 【装饰模式 · Component】结算服务统一接口，装饰者与被装饰者实现同一接口。 */
public interface SettlementFacade {
    void settle(Transaction tx);
}
