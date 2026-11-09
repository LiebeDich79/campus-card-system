package com.campus.card.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 金额工具：统一保留两位小数（四舍五入），所有金额运算必须经由此类。 */
public final class MoneyUtils {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private MoneyUtils() {
    }

    public static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /** 判断小数位数是否超过 2 位（如 1.005 属于非法输入）。 */
    public static boolean overTwoDecimals(BigDecimal value) {
        return value != null && value.scale() > 2;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
