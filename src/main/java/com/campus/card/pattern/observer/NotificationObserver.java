package com.campus.card.pattern.observer;

import com.campus.card.dao.NotifyLogDao;
import com.campus.card.entity.Transaction;
import org.springframework.stereotype.Component;

/** 【观察者模式 · ConcreteObserver ③】交易完成后向持卡人推送消息并留痕。 */
@Component
public class NotificationObserver implements TransactionObserver {
    private final NotifyLogDao notifyLogDao;

    public NotificationObserver(NotifyLogDao notifyLogDao) {
        this.notifyLogDao = notifyLogDao;
    }

    @Override
    public void onEvent(TransactionEvent event) {
        Transaction tx = event.getTransaction();
        String content = "卡号 " + tx.getCardNo() + " 完成 " + tx.getTxType()
                + "，金额 " + tx.getActualAmount() + " 元，交易号 " + tx.getTxNo();
        notifyLogDao.insert(tx.getTxNo(), "PUSH", content);
    }
}
