/**
 * 代理模式（Proxy，结构型模式）。
 *
 * <p>交易服务需要统一附加"横切关注点"：终端鉴权、按终端限流、调用日志与耗时统计。
 * 本包通过 JDK 动态代理 {@code ServiceInvocationHandler} 包装真实的 {@code TransactionService}，
 * Controller 注入的是代理对象，业务实现类因此保持纯净、零侵入，
 * 新增横切逻辑也只需改代理一处。</p>
 */
package com.campus.card.pattern.proxy;
