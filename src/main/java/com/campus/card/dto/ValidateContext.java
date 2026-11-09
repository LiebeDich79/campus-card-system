package com.campus.card.dto;

import com.campus.card.common.ErrorCode;
import com.campus.card.entity.Card;
import com.campus.card.entity.Account;

import java.math.BigDecimal;

/**
 * 责任链各处理器共享的校验上下文
 */
public class ValidateContext {

    private ConsumptionRequest request;
    private Card card;
    private Account account;
    private BigDecimal payableAmount;
    private String idempotencyKey;
    private boolean rejected;
    private ErrorCode errorCode;
    private String message;
    private boolean deducted;

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

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDeducted() {
        return deducted;
    }

    public void setDeducted(boolean deducted) {
        this.deducted = deducted;
    }


    /** 责任链短路：记录第一个失败原因，后续处理器不再执行。 */
    public void reject(ErrorCode code, String message) {
        this.rejected = true;
        this.errorCode = code;
        this.message = message;
    }
}
