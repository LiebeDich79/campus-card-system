/**
 * 责任链模式（Chain of Responsibility，行为型模式）。
 *
 * <p>消费扣款前的校验步骤（幂等 → 卡状态 → 金额合法性 → 限额 → 余额 → 原子扣款）
 * 可能随风控/运营需求增减或调整顺序。本包把每一步抽成独立的 {@code ValidateHandler}，
 * 由 {@code ValidationChain} 负责装配与串联。新增或调整校验只需增删 Handler
 * 或修改装配顺序，交易主流程保持不变（开闭原则）。</p>
 */
package com.campus.card.pattern.chain;
