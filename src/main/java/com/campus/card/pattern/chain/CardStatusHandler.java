package com.campus.card.pattern.chain;

import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ValidateContext;
import org.springframework.stereotype.Component;

/** 【责任链 · ConcreteHandler ②】卡状态与交易密码校验。 */
@Component
public class CardStatusHandler extends ValidateHandler {
    @Override
    protected void doValidate(ValidateContext ctx) {
        if (ctx.getCard() == null) {
            ctx.reject(ErrorCode.CARD_NOT_FOUND, "校园卡不存在");
            return;
        }
        if (!"NORMAL".equals(ctx.getCard().getStatus())) {
            ctx.reject(ErrorCode.CARD_ABNORMAL, "校园卡状态异常: " + ctx.getCard().getStatus());
            return;
        }
        String pin = ctx.getRequest() == null ? null : ctx.getRequest().getPin();
        String realPin = ctx.getCard().getPin();
        if (pin != null && pin.length() > 0 && realPin != null && !realPin.equals(pin)) {
            ctx.reject(ErrorCode.PIN_INCORRECT, "交易密码错误");
        }
    }
}
