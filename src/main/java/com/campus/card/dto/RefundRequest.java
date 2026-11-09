package com.campus.card.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 退款/冲正请求
 */
public class RefundRequest {

    @NotBlank(message = "原交易号不能为空")
    private String originalTxNo;
    private BigDecimal amount;
    private String reason;
    private String idempotencyKey;
    private String operator;

    public String getOriginalTxNo() {
        return originalTxNo;
    }

    public void setOriginalTxNo(String originalTxNo) {
        this.originalTxNo = originalTxNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

}
