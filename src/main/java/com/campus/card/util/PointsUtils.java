package com.campus.card.util;

import com.campus.card.config.SystemConfig;
import com.campus.card.entity.Transaction;

import java.math.BigDecimal;

/** 积分变动规则：装饰者（改余额）与观察者（记流水）共用，保证账实一致。 */
public final class PointsUtils {
    private PointsUtils() {
    }

    public static int delta(Transaction tx, SystemConfig config) {
        if (tx == null || !"CONSUME".equals(tx.getTxType())) {
            return 0;
        }
        if ("POINTS".equals(tx.getStrategyType())) {
            // 积分抵扣：按优惠金额折算消耗积分（每 pointsPerYuan 积分抵 1 元）
            return -tx.getDiscountAmount().multiply(new BigDecimal(config.getPointsPerYuan())).intValue();
        }
        // 普通消费：每消费 1 元累计 1 积分
        return tx.getActualAmount().intValue();
    }
}
