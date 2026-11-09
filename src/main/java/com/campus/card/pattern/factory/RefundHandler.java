package com.campus.card.pattern.factory;

import com.campus.card.dto.TransactionContext;
import com.campus.card.entity.Transaction;
import com.campus.card.pattern.builder.TransactionBuilder;
import org.springframework.stereotype.Component;

/** 【工厂模式 · ConcreteProduct】退款冲正交易，记录被冲正的原交易号。 */
@Component
public class RefundHandler implements TransactionHandler {
    @Override
    public String supportedType() {
        return "REFUND";
    }

    @Override
    public Transaction handle(TransactionContext c) {
        return new TransactionBuilder()
                .txNo(c.getTxNo())
                .card(c.getCard())
                .txType("REFUND")
                .amount(c.getOriginalAmount())
                .payable(c.getOriginalAmount())
                .strategy("NORMAL")
                .idempotencyKey(c.getIdempotencyKey())
                .operator(c.getOperator())
                .originalTxNo(c.getOriginalTxNo())
                .remark(c.getRemark())
                .build();
    }
}
