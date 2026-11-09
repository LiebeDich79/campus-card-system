package com.campus.card.pattern.chain;

import com.campus.card.common.ErrorCode;
import com.campus.card.config.SystemConfig;
import com.campus.card.dao.AccountDao;
import com.campus.card.dto.ValidateContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 【责任链 · ConcreteHandler ④】单笔限额与单日累计限额。 */
@Component
public class LimitHandler extends ValidateHandler {
    private final AccountDao accountDao;

    public LimitHandler(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    protected void doValidate(ValidateContext ctx) {
        SystemConfig config = SystemConfig.getInstance();
        BigDecimal amount = ctx.getRequest().getAmount();
        if (amount.compareTo(config.getSingleLimit()) > 0) {
            ctx.reject(ErrorCode.LIMIT_EXCEEDED, "超出单笔限额 " + config.getSingleLimit() + " 元");
            return;
        }
        BigDecimal today = accountDao.todayConsumed(ctx.getCard().getId());
        if (today.add(ctx.getPayableAmount()).compareTo(config.getDailyLimit()) > 0) {
            ctx.reject(ErrorCode.LIMIT_EXCEEDED, "超出单日累计限额 " + config.getDailyLimit() + " 元");
        }
    }
}
