package com.campus.card.common;

/** 业务异常：责任链短路、策略拒绝、DAO 失败等统一抛出本异常。 */
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
