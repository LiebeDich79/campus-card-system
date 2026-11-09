package com.campus.card.pattern.factory;

import com.campus.card.dto.TransactionContext;
import com.campus.card.entity.Transaction;
import com.campus.card.pattern.builder.TransactionBuilder;
import org.springframework.stereotype.Component;

/** 【工厂模式 · ConcreteProduct】消费交易：携带优惠金额、实付金额与命中策略。 */
@Component
public class ConsumeHandler implements TransactionHandler {
    @Override
    public String supportedType() {
        return "CONSUME";
    }

    @Override
    public Transaction handle(TransactionContext c) {
        return new TransactionBuilder()
                .txNo(c.getTxNo())
                .card(c.getCard())
                .terminalNo(c.getTerminalNo())
                .txType("CONSUME")
                .amount(c.getOriginalAmount())
                .payable(c.getPayableAmount())
                .strategy(c.getStrategyType())
                .idempotencyKey(c.getIdempotencyKey())
                .operator(c.getOperator())
                .items(c.getItems())
                .build();
    }
}
