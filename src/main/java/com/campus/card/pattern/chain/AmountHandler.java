package com.campus.card.pattern.chain;

import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ValidateContext;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 【责任链 · ConcreteHandler ③】金额合法性：非空、正数、最多两位小数、实付非负。 */
@Component
public class AmountHandler extends ValidateHandler {
    @Override
    protected void doValidate(ValidateContext ctx) {
        BigDecimal amount = ctx.getRequest() == null ? null : ctx.getRequest().getAmount();
        if (!MoneyUtils.isPositive(amount) || MoneyUtils.overTwoDecimals(amount)) {
            ctx.reject(ErrorCode.INVALID_AMOUNT, "金额必须为大于 0 且最多两位小数的数值");
            return;
        }
        if (ctx.getPayableAmount() == null || ctx.getPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            ctx.reject(ErrorCode.INVALID_AMOUNT, "优惠后实付金额非法");
        }
    }
}
