package com.campus.card.pattern.chain;

import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ValidateContext;
import org.springframework.stereotype.Component;

/** 【责任链 · ConcreteHandler ⑤】余额预检；真正的并发安全由末端的原子 UPDATE 保证。 */
@Component
public class BalanceHandler extends ValidateHandler {
    @Override
    // 只是预检，真正的并发安全在末端那条原子 UPDATE，别指望这层能挡住并发
    protected void doValidate(ValidateContext ctx) {
        if (ctx.getAccount() == null || ctx.getAccount().getBalance().compareTo(ctx.getPayableAmount()) < 0) {
            ctx.reject(ErrorCode.BALANCE_NOT_ENOUGH, "账户余额不足");
        }
    }
}
