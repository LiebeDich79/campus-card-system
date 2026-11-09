package com.campus.card.pattern.chain;

import com.campus.card.common.ErrorCode;
import com.campus.card.config.IdempotencyCache;
import com.campus.card.dao.TransactionDao;
import com.campus.card.dto.ValidateContext;
import com.campus.card.entity.Transaction;
import org.springframework.stereotype.Component;

/** 【责任链 · ConcreteHandler ①】幂等校验：内存缓存 + 数据库唯一索引双保险。 */
@Component
public class IdempotencyHandler extends ValidateHandler {
    private final TransactionDao transactionDao;

    public IdempotencyHandler(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Override
    protected void doValidate(ValidateContext ctx) {
        String key = ctx.getIdempotencyKey();
        if (key == null || key.trim().length() == 0) {
            return;
        }
        if (!IdempotencyCache.getInstance().tryAcquire(key)) {
            ctx.reject(ErrorCode.DUPLICATE_REQUEST, "重复提交：该幂等键正在处理中");
            return;
        }
        Transaction exists = transactionDao.findByIdempotencyKey(key);
        if (exists != null) {
            IdempotencyCache.getInstance().release(key);
            ctx.reject(ErrorCode.DUPLICATE_REQUEST, "重复消费：该交易已入账");
        }
    }
}
