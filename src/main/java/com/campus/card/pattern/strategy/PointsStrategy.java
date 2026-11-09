package com.campus.card.pattern.strategy;

import com.campus.card.dto.DeductContext;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 【策略模式 · ConcreteStrategy】积分抵扣：每 100 积分抵 1 元，最多抵扣订单金额的 50%。
 * 该策略只负责"算钱"，积分的实际扣减由 PointsObserver 在结算阶段统一处理，
 * 保证金额计算与积分账务各自职责单一。
 */
@Component
public class PointsStrategy implements DeductStrategy {
    @Override
    public String type() {
        return "POINTS";
    }

    @Override
    public BigDecimal calculate(DeductContext context) {
        int points = context.getAccount() == null ? 0 : context.getAccount().getPoints();
        BigDecimal pointsWorth = new BigDecimal(points)
                .divide(new BigDecimal(context.getConfig().getPointsPerYuan()), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal half = context.getAmount().multiply(new BigDecimal("0.5"));
        BigDecimal discount = MoneyUtils.min(pointsWorth, half);
        return MoneyUtils.scale(context.getAmount().subtract(discount));
    }
}
