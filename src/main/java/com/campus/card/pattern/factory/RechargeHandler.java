package com.campus.card.pattern.factory;

import com.campus.card.dto.TransactionContext;
import com.campus.card.entity.Transaction;
import com.campus.card.pattern.builder.TransactionBuilder;
import org.springframework.stereotype.Component;

/** 【工厂模式 · ConcreteProduct】充值交易。 */
@Component
public class RechargeHandler implements TransactionHandler {
    @Override
    public String supportedType() {
        return "RECHARGE";
    }

    @Override
    public Transaction handle(TransactionContext c) {
        return new TransactionBuilder()
                .txNo(c.getTxNo())
                .card(c.getCard())
                .txType("RECHARGE")
                .amount(c.getOriginalAmount())
                .payable(c.getOriginalAmount())
                .strategy("NORMAL")
                .idempotencyKey(c.getIdempotencyKey())
                .operator(c.getOperator())
                .remark("柜台/线上充值")
                .build();
    }
}
