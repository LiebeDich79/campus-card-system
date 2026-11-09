package com.campus.card.pattern.chain;

import com.campus.card.dto.ValidateContext;

/**
 * 【责任链模式 · Handler】抽象处理器，持有 next 引用。
 * handle 用模板方法固定"先校验自己、失败即短路、否则交给下一级"的骨架，
 * 子类只需实现 doValidate，符合单一职责原则。
 */
public abstract class ValidateHandler {
    protected ValidateHandler next;

    public ValidateHandler setNext(ValidateHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(ValidateContext context) {
        doValidate(context);
        if (context.isRejected()) {
            return;   // 短路：后续处理器不再执行
        }
        if (next != null) {
            next.handle(context);
        }
    }

    protected abstract void doValidate(ValidateContext context);
}
