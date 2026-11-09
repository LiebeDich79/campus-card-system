/**
 * 建造者模式（Builder，创建型模式）。
 *
 * <p>一笔交易聚合根 {@code Transaction} 字段众多，且可能携带若干 {@code TransactionItem} 明细。
 * 本包提供 {@code TransactionBuilder}，以链式调用逐步装配字段与明细，
 * 避免又长又易错的" telescoping constructor "（叠加式构造函数），
 * 并在 {@code build()} 时校验必填项，提前拦截不完整的交易对象。</p>
 */
package com.campus.card.pattern.builder;
