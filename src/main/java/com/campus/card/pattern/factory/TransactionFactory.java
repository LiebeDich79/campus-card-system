package com.campus.card.pattern.factory;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.dto.TransactionContext;
import com.campus.card.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【工厂模式 · Creator】按交易类型返回对应处理器。
 * 通过 Spring 构造器注入全部 TransactionHandler 实现自动形成注册表，
 * 客户端只依赖工厂与产品接口，新增交易类型无需修改任何客户端代码（开闭原则）。
 */
@Component
public class TransactionFactory {
    private final Map<String, TransactionHandler> registry = new ConcurrentHashMap<String, TransactionHandler>();

    public TransactionFactory(List<TransactionHandler> handlers) {
        for (TransactionHandler handler : handlers) {
            registry.put(handler.supportedType(), handler);
        }
    }

    // 不写 switch：加一种交易类型就加一个 Handler Bean，这里一行都不用动
    public TransactionHandler create(String txType) {
        TransactionHandler handler = registry.get(txType);
        if (handler == null) {
            throw new BizException(ErrorCode.UNSUPPORTED_TX_TYPE, "不支持的交易类型: " + txType);
        }
        return handler;
    }

    public Transaction createTransaction(String txType, TransactionContext context) {
        return create(txType).handle(context);
    }

    public int size() {
        return registry.size();
    }
}
