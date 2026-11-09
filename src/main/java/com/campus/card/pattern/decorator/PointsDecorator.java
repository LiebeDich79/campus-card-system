package com.campus.card.pattern.decorator;

import com.campus.card.config.SystemConfig;
import com.campus.card.dao.AccountDao;
import com.campus.card.entity.Transaction;
import com.campus.card.util.PointsUtils;

/** 【装饰模式 · ConcreteDecorator ①】为结算叠加积分账户变动能力。 */
public class PointsDecorator extends SettlementDecorator {
    private final AccountDao accountDao;

    public PointsDecorator(SettlementFacade wrapped, AccountDao accountDao) {
        super(wrapped);
        this.accountDao = accountDao;
    }

    @Override
    public void settle(Transaction tx) {
        wrapped.settle(tx);
        int delta = PointsUtils.delta(tx, SystemConfig.getInstance());
        if (delta != 0) {
            accountDao.addPoints(tx.getCardId(), delta);
        }
    }
}
