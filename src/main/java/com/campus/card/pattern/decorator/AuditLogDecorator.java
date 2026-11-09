package com.campus.card.pattern.decorator;

import com.campus.card.dao.NotifyLogDao;
import com.campus.card.entity.Transaction;

/** 【装饰模式 · ConcreteDecorator ②】为结算叠加操作审计日志，且可继续叠加缓存装饰者。 */
public class AuditLogDecorator extends SettlementDecorator {
    private final NotifyLogDao notifyLogDao;

    public AuditLogDecorator(SettlementFacade wrapped, NotifyLogDao notifyLogDao) {
        super(wrapped);
        this.notifyLogDao = notifyLogDao;
    }

    @Override
    public void settle(Transaction tx) {
        long start = System.currentTimeMillis();
        wrapped.settle(tx);
        String detail = "SETTLE " + tx.getTxNo() + " " + tx.getTxType() + " " + tx.getActualAmount()
                + " 元，耗时 " + (System.currentTimeMillis() - start) + " ms";
        notifyLogDao.insert(tx.getTxNo(), "AUDIT", detail);
    }
}
