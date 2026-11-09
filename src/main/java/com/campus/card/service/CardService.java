package com.campus.card.service;

import com.campus.card.dto.RechargeRequest;
import com.campus.card.dto.TxResult;
import com.campus.card.entity.Card;

import java.math.BigDecimal;
import java.util.List;

/** 卡片与账户业务接口（业务层）。 */
public interface CardService {
    Card open(Card card, BigDecimal initialBalance);

    Card detail(String cardNo);

    List<Card> list();

    Card lost(String cardNo);

    Card cancel(String cardNo);

    TxResult recharge(RechargeRequest request);
}
