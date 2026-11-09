package com.campus.card.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.campus.card.entity.TransactionItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统一消费请求：所有终端经适配器后都转换为该对象
 */
public class ConsumptionRequest {

    @NotBlank(message = "卡号不能为空")
    private String cardNo;
    private String terminalNo;
    private String terminalType;
    @NotNull(message = "消费金额不能为空")
    @DecimalMin(value = "0.01", message = "消费金额必须大于 0")
    private BigDecimal amount;
    private String pin;
    private String strategyType;
    private String idempotencyKey;
    private String operator;
    private List<TransactionItem> items;

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getTerminalNo() {
        return terminalNo;
    }

    public void setTerminalNo(String terminalNo) {
        this.terminalNo = terminalNo;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
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

    public List<TransactionItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionItem> items) {
        this.items = items;
    }

}
