package com.campus.card.pattern.builder;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.entity.Card;
import com.campus.card.entity.Transaction;
import com.campus.card.entity.TransactionItem;
import com.campus.card.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 【建造者模式 · Builder】交易单字段多、可选字段多（优惠、策略、明细、冲正号），
 * 用链式构建把"构造过程"与"最终表示"分离，避免出现十几个参数的构造方法。
 */
public class TransactionBuilder {
    private final Transaction tx = new Transaction();
    private final List<TransactionItem> items = new ArrayList<TransactionItem>();

    public TransactionBuilder txNo(String txNo) {
        tx.setTxNo(txNo);
        return this;
    }

    public TransactionBuilder card(Card card) {
        if (card != null) {
            tx.setCardId(card.getId());
            tx.setCardNo(card.getCardNo());
        }
        return this;
    }

    public TransactionBuilder terminalNo(String terminalNo) {
        tx.setTerminalNo(terminalNo);
        return this;
    }

    public TransactionBuilder txType(String txType) {
        tx.setTxType(txType);
        return this;
    }

    public TransactionBuilder amount(BigDecimal amount) {
        tx.setAmount(MoneyUtils.scale(amount));
        return this;
    }

    public TransactionBuilder payable(BigDecimal payable) {
        BigDecimal actual = MoneyUtils.scale(payable);
        tx.setActualAmount(actual);
        BigDecimal original = tx.getAmount() == null ? actual : tx.getAmount();
        tx.setDiscountAmount(MoneyUtils.scale(original.subtract(actual)));
        return this;
    }

    public TransactionBuilder strategy(String strategy) {
        tx.setStrategyType(strategy);
        return this;
    }

    public TransactionBuilder idempotencyKey(String key) {
        tx.setIdempotencyKey(key);
        return this;
    }

    public TransactionBuilder operator(String operator) {
        tx.setOperator(operator);
        return this;
    }

    public TransactionBuilder originalTxNo(String originalTxNo) {
        tx.setOriginalTxNo(originalTxNo);
        return this;
    }

    public TransactionBuilder remark(String remark) {
        tx.setRemark(remark);
        return this;
    }

    /** 单个消费明细，链式逐项添加。 */
    public TransactionBuilder item(String name, int quantity, BigDecimal unitPrice) {
        TransactionItem item = new TransactionItem();
        item.setItemName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(MoneyUtils.scale(unitPrice));
        item.setAmount(MoneyUtils.scale(unitPrice.multiply(new BigDecimal(quantity))));
        items.add(item);
        return this;
    }

    public TransactionBuilder items(List<TransactionItem> list) {
        if (list != null) {
            items.addAll(list);
        }
        return this;
    }

    /** 统一做必填校验与默认值，保证出厂的交易对象一定合法。 */
    public Transaction build() {
        if (tx.getTxNo() == null || tx.getActualAmount() == null || tx.getCardNo() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "交易单缺少必填字段");
        }
        if (!MoneyUtils.isPositive(tx.getActualAmount())) {
            throw new BizException(ErrorCode.INVALID_AMOUNT, "实付金额必须大于 0");
        }
        if (items.isEmpty()) {
            item(tx.getTxType() + " 交易", 1, tx.getActualAmount());
        }
        for (TransactionItem i : items) {
            i.setTxNo(tx.getTxNo());
        }
        tx.setItems(new ArrayList<TransactionItem>(items));
        tx.setStatus("SUCCESS");
        tx.setCreatedAt(new Date());
        return tx;
    }
}
