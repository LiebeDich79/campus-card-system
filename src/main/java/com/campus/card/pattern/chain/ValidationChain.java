package com.campus.card.pattern.chain;

import com.campus.card.dto.ValidateContext;
import org.springframework.stereotype.Component;

/**
 * 【责任链模式 · 链装配器】集中定义校验顺序，链上节点可自由增删或调整顺序，
 * 而无需改动交易主流程（变化点被隔离在装配配置里）。
 */
@Component
public class ValidationChain {
    private final ValidateHandler head;

    public ValidationChain(IdempotencyHandler h1, CardStatusHandler h2, AmountHandler h3,
                           LimitHandler h4, BalanceHandler h5, DeductHandler h6) {
        this.head = h1;
        // 想加一道校验：加个 Handler Bean，再往这行链里插一次即可
        h1.setNext(h2).setNext(h3).setNext(h4).setNext(h5).setNext(h6);
    }

    public void execute(ValidateContext context) {
        head.handle(context);
    }
}
