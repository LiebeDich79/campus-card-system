package com.campus.card.pattern.observer;

import com.campus.card.config.SystemConfig;
import com.campus.card.dao.AccountDao;
import com.campus.card.dao.PointsLogDao;
import com.campus.card.entity.Account;
import com.campus.card.entity.Transaction;
import com.campus.card.util.PointsUtils;
import org.springframework.stereotype.Component;

/**
 * 【观察者模式 · ConcreteObserver ④】积分流水记账。
 * 职责划分：PointsDecorator 在事务内变更账户积分，本观察者在事务提交后补记流水，
 * 二者通过 PointsUtils.delta 共用同一计算规则，避免重复实现导致账实不符。
 */
@Component
public class PointsObserver implements TransactionObserver {
    private final AccountDao accountDao;
    private final PointsLogDao pointsLogDao;

    public PointsObserver(AccountDao accountDao, PointsLogDao pointsLogDao) {
        this.accountDao = accountDao;
        this.pointsLogDao = pointsLogDao;
    }

    @Override
    public void onEvent(TransactionEvent event) {
        Transaction tx = event.getTransaction();
        int delta = PointsUtils.delta(tx, SystemConfig.getInstance());
        if (delta == 0) {
            return;
        }
        Account account = accountDao.findByCardId(tx.getCardId());
        int after = account == null ? 0 : account.getPoints();
        pointsLogDao.insert(tx.getCardId(), tx.getTxNo(), delta, after);
    }
}
