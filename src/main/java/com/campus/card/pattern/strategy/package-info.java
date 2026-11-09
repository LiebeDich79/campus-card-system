/**
 * 策略模式（Strategy，行为型模式）。
 *
 * <p>优惠扣款算法随卡类型、时段、终端而变（原价 / 折扣 / 补贴 / 积分抵扣），且算法可能继续增加。
 * 本包定义 {@code DeductStrategy} 策略接口与四种具体策略，
 * 由 {@code DeductStrategyResolver} 在运行时按规则选择策略，消除主流程中的 if/else 叠加，
 * 新增优惠方式只需实现接口并注册为 Spring Bean。</p>
 */
package com.campus.card.pattern.strategy;
