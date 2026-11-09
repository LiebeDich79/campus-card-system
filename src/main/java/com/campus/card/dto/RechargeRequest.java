package com.campus.card.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 充值请求
 */
public class RechargeRequest {

    @NotBlank(message = "卡号不能为空")
    private String cardNo;
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    private BigDecimal amount;
    private String idempotencyKey;
    private String operator;

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
