package com.campus.card.pattern.decorator;

import com.campus.card.dao.TransactionDao;
import com.campus.card.entity.Transaction;

/** 【装饰模式 · ConcreteComponent】基础结算：只做交易落账时间标记。 */
public class DefaultSettlement implements SettlementFacade {
    private final TransactionDao transactionDao;

    public DefaultSettlement(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Override
    public void settle(Transaction tx) {
        transactionDao.updateSettled(tx.getTxNo());
    }
}
