package com.campus.card.common;

/** 统一业务错误码：前端据此做提示与重试决策。 */
public enum ErrorCode {
    OK(0, "成功"),
    BAD_REQUEST(400, "请求参数非法"),
    UNAUTHORIZED(401, "未授权的访问"),
    RATE_LIMITED(429, "访问过于频繁"),
    INTERNAL_ERROR(500, "系统内部错误"),
    CARD_NOT_FOUND(1001, "校园卡不存在"),
    CARD_ABNORMAL(1002, "校园卡已挂失/冻结/注销"),
    PIN_INCORRECT(1003, "交易密码错误"),
    INVALID_AMOUNT(1004, "交易金额非法"),
    BALANCE_NOT_ENOUGH(1005, "账户余额不足"),
    LIMIT_EXCEEDED(1006, "超出单笔或单日限额"),
    DUPLICATE_REQUEST(1007, "重复的交易请求"),
    TX_NOT_FOUND(1008, "原交易不存在"),
    TX_ALREADY_REVERSED(1009, "原交易已冲正"),
    REFUND_AMOUNT_INVALID(1010, "退款金额超过原交易实付金额"),
    UNSUPPORTED_TX_TYPE(1011, "不支持的交易类型"),
    TERMINAL_NOT_FOUND(1012, "终端不存在或已停用"),
    TERMINAL_PAYLOAD_INVALID(1013, "终端报文解析失败"),
    CONCURRENT_CONFLICT(1014, "并发冲突，请重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
