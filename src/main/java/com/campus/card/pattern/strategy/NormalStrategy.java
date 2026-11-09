package com.campus.card.pattern.strategy;

import com.campus.card.dto.DeductContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 【策略模式 · ConcreteStrategy】普通扣款：原价支付。 */
@Component
public class NormalStrategy implements DeductStrategy {
    @Override
    public String type() {
        return "NORMAL";
    }

    @Override
    public BigDecimal calculate(DeductContext context) {
        return context.getAmount();
    }
}
