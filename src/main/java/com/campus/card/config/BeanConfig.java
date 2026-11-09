package com.campus.card.config;

import com.campus.card.dao.AccountDao;
import com.campus.card.dao.NotifyLogDao;
import com.campus.card.dao.TerminalDao;
import com.campus.card.dao.TransactionDao;
import com.campus.card.pattern.decorator.AuditLogDecorator;
import com.campus.card.pattern.decorator.DefaultSettlement;
import com.campus.card.pattern.decorator.PointsDecorator;
import com.campus.card.pattern.decorator.SettlementFacade;
import com.campus.card.pattern.proxy.ServiceInvocationHandler;
import com.campus.card.service.TransactionService;
import com.campus.card.service.TransactionServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Proxy;

/**
 * 【代理模式 + 装饰模式 · 装配点】
 * 1) 把 TransactionService 包装成 JDK 动态代理 Bean（@Primary），Controller 注入的即是代理；
 * 2) 手动组装结算装饰链：DefaultSettlement → PointsDecorator → AuditLogDecorator。
 * 装配顺序集中在一处，调整横切功能与装饰层数无需改动业务实现。
 * 中间有一版把装饰顺序散在各 Service 里，改一次要翻三四个文件，后来统一收回这里。
 */
@Configuration
public class BeanConfig {

    @Bean
    @Primary
    public TransactionService transactionServiceProxy(TransactionServiceImpl target, TerminalDao terminalDao) {
        return (TransactionService) Proxy.newProxyInstance(
                TransactionService.class.getClassLoader(),
                new Class<?>[]{TransactionService.class},
                new ServiceInvocationHandler(target, terminalDao));
    }

    @Bean
    public SettlementFacade settlementFacade(TransactionDao transactionDao, AccountDao accountDao,
                                             NotifyLogDao notifyLogDao) {
        SettlementFacade base = new DefaultSettlement(transactionDao);
        SettlementFacade withPoints = new PointsDecorator(base, accountDao);
        return new AuditLogDecorator(withPoints, notifyLogDao);
    }
}
