/**
 * 装饰模式（Decorator，结构型模式）。
 *
 * <p>交易成功后需要叠加若干"结算增强"：积分累计、审计日志写入等，且增强可自由组合、可插拔。
 * 本包以 {@code SettlementDecorator} 为抽象装饰者，{@code DefaultSettlement} 为基础实现，
 * {@code PointsDecorator}、{@code AuditLogDecorator} 为具体装饰者；
 * {@code SettlementFacade} 按配置组装装饰链，使增强顺序与组合对调用方透明。</p>
 */
package com.campus.card.pattern.decorator;
