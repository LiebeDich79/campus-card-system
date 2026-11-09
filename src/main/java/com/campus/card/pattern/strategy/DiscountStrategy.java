package com.campus.card.pattern.strategy;

import com.campus.card.dto.DeductContext;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 【策略模式 · ConcreteStrategy】折扣扣款：学生 9 折、教师 95 折，折扣率来自单例配置。 */
@Component
public class DiscountStrategy implements DeductStrategy {
    @Override
    public String type() {
        return "DISCOUNT";
    }

    @Override
    // 别图省事换回 double：15.00 * 0.9 会算出 13.500000000000002
    public BigDecimal calculate(DeductContext context) {
        BigDecimal rate = "STUDENT".equals(context.getCard().getCardType())
                ? context.getConfig().getStudentDiscount() : context.getConfig().getTeacherDiscount();
        return MoneyUtils.scale(context.getAmount().multiply(rate));
    }
}
