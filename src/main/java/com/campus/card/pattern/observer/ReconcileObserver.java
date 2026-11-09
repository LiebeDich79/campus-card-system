package com.campus.card.pattern.observer;

import com.campus.card.dao.ReconcileDao;
import com.campus.card.entity.Transaction;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/** 【观察者模式 · ConcreteObserver ①】实时累加终端当日对账汇总。 */
@Component
public class ReconcileObserver implements TransactionObserver {
    private final ReconcileDao reconcileDao;

    public ReconcileObserver(ReconcileDao reconcileDao) {
        this.reconcileDao = reconcileDao;
    }

    @Override
    public void onEvent(TransactionEvent event) {
        Transaction tx = event.getTransaction();
        if (tx.getTerminalId() == null || !"CONSUME".equals(tx.getTxType())) {
            return;
        }
        String day = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        reconcileDao.accumulate(tx.getTerminalId(), day, tx.getActualAmount());
    }
}
