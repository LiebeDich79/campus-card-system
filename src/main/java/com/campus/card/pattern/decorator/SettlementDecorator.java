package com.campus.card.pattern.decorator;

/** 【装饰模式 · Decorator】持有被装饰对象，子类可以在调用前后叠加功能。 */
public abstract class SettlementDecorator implements SettlementFacade {
    protected final SettlementFacade wrapped;

    protected SettlementDecorator(SettlementFacade wrapped) {
        this.wrapped = wrapped;
    }
}
