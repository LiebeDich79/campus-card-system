package com.campus.card.dto;

import com.campus.card.config.SystemConfig;
import com.campus.card.entity.Card;
import com.campus.card.entity.Account;
import com.campus.card.pattern.strategy.DeductStrategy;

import java.math.BigDecimal;

/**
 * 策略模式的上下文，持有当前策略
 */
public class DeductContext {

    private Card card;
    private Account account;
    private BigDecimal amount;
    private String terminalType;
    private DeductStrategy strategy;
    private SystemConfig config;

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public DeductStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(DeductStrategy strategy) {
        this.strategy = strategy;
    }

    public SystemConfig getConfig() {
        return config;
    }

    public void setConfig(SystemConfig config) {
        this.config = config;
    }


    /** 上下文把计算委托给当前策略对象，客户端不需要知道具体算法。 */
    public BigDecimal calculate() {
        if (strategy == null) {
            throw new IllegalStateException("未设置扣款策略");
        }
        return strategy.calculate(this);
    }
}
