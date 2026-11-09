package com.campus.card.pattern.chain;

import com.campus.card.common.ErrorCode;
import com.campus.card.dao.AccountDao;
import com.campus.card.dto.ValidateContext;
import org.springframework.stereotype.Component;

/** 【责任链 · ConcreteHandler ⑥（末端）】在事务内执行原子扣款，失败即短路回滚。 */
@Component
public class DeductHandler extends ValidateHandler {
    private final AccountDao accountDao;

    public DeductHandler(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    // rows==0 有两种可能：余额不够，或 version 被别的请求改了；都按冲突回滚，让客户端重试
    protected void doValidate(ValidateContext ctx) {
        int rows = accountDao.atomicDeduct(ctx.getCard().getId(), ctx.getPayableAmount(),
                ctx.getAccount().getVersion());
        if (rows == 0) {
            ctx.reject(ErrorCode.CONCURRENT_CONFLICT, "扣款失败：余额不足或并发冲突，请重试");
            return;
        }
        ctx.setDeducted(true);
        ctx.getAccount().setBalance(ctx.getAccount().getBalance().subtract(ctx.getPayableAmount()));
        ctx.getAccount().setVersion(ctx.getAccount().getVersion() + 1);
    }
}
