package com.campus.card.pattern.strategy;

import com.campus.card.dto.DeductContext;

import java.math.BigDecimal;

/** 【策略模式 · Strategy】优惠扣款算法族的共同接口，返回最终实付金额。 */
public interface DeductStrategy {
    /** 策略类型标识，与数据库 strategy_type 一致（如 NORMAL / DISCOUNT / SUBSIDY / POINTS）。 */
    String type();

    /**
     * 计算该笔交易的「实付金额」。
     *
     * @param context 扣款上下文：含原始金额、持卡人、账户、终端类型与全局配置
     * @return 计算后的实付金额（已按 {@code MoneyUtils.scale} 四舍五入到分）
     */
    BigDecimal calculate(DeductContext context);
}
