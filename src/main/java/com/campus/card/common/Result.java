package com.campus.card.common;

/** 前后端统一响应结构 Result&lt;T&gt;。 */
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<T>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data);
    }

    public static <T> Result<T> fail(ErrorCode ec, String message) {
        return new Result<T>(ec.getCode(), message == null ? ec.getMessage() : message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
