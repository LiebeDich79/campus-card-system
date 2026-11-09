/**
 * 适配器模式（Adapter，结构型模式）。
 *
 * <p>校园内的消费终端由不同厂商提供（食堂 POS、超市收银、门禁、水控等），
 * 各厂商上报的报文格式、字段命名、金额单位、时间戳格式都不一致。
 * 本包把这些"异构终端原始报文"统一适配成系统内部的标准 {@code ConsumptionRequest}，
 * 使下游交易主流程只依赖标准请求，而不必感知任何终端差异（隔离变化点）。</p>
 *
 * <ul>
 *   <li>{@code TerminalAdapter}：适配器接口（目标接口），定义 {@code adapt()} 契约。</li>
 *   <li>{@code PosTerminalAdapter} / {@code SupermarketAdapter} /
 *       {@code AccessControlAdapter} / {@code WaterControlAdapter}：
 *       四个具体适配器，分别解析各自厂商的字段与单位。</li>
 *   <li>{@code TerminalAdapterRegistry}：适配器注册表，按终端类型路由到对应适配器。</li>
 * </ul>
 */
package com.campus.card.pattern.adapter;
