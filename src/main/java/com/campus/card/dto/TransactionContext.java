package com.campus.card.dto;

import com.campus.card.entity.Card;
import com.campus.card.entity.Account;
import com.campus.card.entity.TransactionItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工厂模式的产品创建上下文
 */
public class TransactionContext {

    private ConsumptionRequest request;
    private Card card;
    private Account account;
    private String txNo;
    private String txType;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String strategyType;
    private String terminalType;
    private String terminalNo;
    private String idempotencyKey;
    private String operator;
    private BigDecimal balanceAfter;
    private String originalTxNo;
    private String remark;
    private List<TransactionItem> items;

    public ConsumptionRequest getRequest() {
        return request;
    }

    public void setRequest(ConsumptionRequest request) {
        this.request = request;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getTxNo() {
        return txNo;
    }

    public void setTxNo(String txNo) {
        this.txNo = txNo;
    }

    public String getTxType() {
        return txType;
    }

    public void setTxType(String txType) {
        this.txType = txType;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getTerminalNo() {
        return terminalNo;
    }

    public void setTerminalNo(String terminalNo) {
        this.terminalNo = terminalNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getOriginalTxNo() {
        return originalTxNo;
    }

    public void setOriginalTxNo(String originalTxNo) {
        this.originalTxNo = originalTxNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

}
