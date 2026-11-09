package com.campus.card.pattern.strategy;

import com.campus.card.dto.DeductContext;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 【策略模式 · ConcreteStrategy】午餐补贴：减免固定金额，最低实付 0.01 元。 */
@Component
public class SubsidyStrategy implements DeductStrategy {
    @Override
    public String type() {
        return "SUBSIDY";
    }

    @Override
    public BigDecimal calculate(DeductContext context) {
        BigDecimal after = context.getAmount().subtract(context.getConfig().getSubsidyAmount());
        return MoneyUtils.scale(MoneyUtils.max(after, new BigDecimal("0.01")));
    }
}
