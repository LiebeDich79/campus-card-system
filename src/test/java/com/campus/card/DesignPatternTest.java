package com.campus.card;

import com.campus.card.common.BizException;
import com.campus.card.config.SystemConfig;
import com.campus.card.config.IdempotencyCache;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.DeductContext;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.dto.ValidateContext;
import com.campus.card.entity.Account;
import com.campus.card.entity.Card;
import com.campus.card.entity.Transaction;
import com.campus.card.pattern.adapter.PosTerminalAdapter;
import com.campus.card.pattern.builder.TransactionBuilder;
import com.campus.card.pattern.chain.ValidateHandler;
import com.campus.card.pattern.factory.ConsumeHandler;
import com.campus.card.pattern.factory.RechargeHandler;
import com.campus.card.pattern.factory.RefundHandler;
import com.campus.card.pattern.factory.TransactionFactory;
import com.campus.card.pattern.observer.TransactionEvent;
import com.campus.card.pattern.observer.TransactionObserver;
import com.campus.card.pattern.observer.TransactionSubject;
import com.campus.card.pattern.strategy.DiscountStrategy;
import com.campus.card.pattern.strategy.NormalStrategy;
import com.campus.card.pattern.strategy.SubsidyStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设计模式有效性单元测试（纯 JUnit，不依赖数据库与 Spring 容器）。
 * 运行：mvn test -Dtest=DesignPatternTest
 */
class DesignPatternTest {

    private Card studentCard() {
        Card card = new Card();
        card.setId(1L);
        card.setCardNo("202601010001");
        card.setCardType("STUDENT");
        card.setStatus("NORMAL");
        card.setPin("123456");
        card.setVersion(0);
        return card;
    }

    private Account account(String balance, int points) {
        Account a = new Account();
        a.setCardId(1L);
        a.setBalance(new BigDecimal(balance));
        a.setPoints(points);
        a.setVersion(0);
        return a;
    }

    private DeductContext deduct(String amount, Card card, Account acc, Object strategy) {
        DeductContext ctx = new DeductContext();
        ctx.setCard(card);
        ctx.setAccount(acc);
        ctx.setAmount(new BigDecimal(amount));
        ctx.setTerminalType("POS");
        ctx.setConfig(SystemConfig.getInstance());
        ctx.setStrategy((com.campus.card.pattern.strategy.DeductStrategy) strategy);
        return ctx;
    }

    @Test
    void factoryCreatesProductsByType() {
        TransactionFactory factory = new TransactionFactory(Arrays.asList(
                new ConsumeHandler(), new RechargeHandler(), new RefundHandler()));
        assertEquals(3, factory.size());
        assertEquals("CONSUME", factory.create("CONSUME").supportedType());
        assertTrue(factory.create("REFUND") instanceof RefundHandler);
        assertThrows(BizException.class, () -> factory.create("UNKNOWN"));
    }

    @Test
    void builderBuildsCompleteTransaction() {
        Transaction tx = new TransactionBuilder()
                .txNo("TX20260101000001")
                .card(studentCard())
                .txType("CONSUME")
                .amount(new BigDecimal("15.00"))
                .payable(new BigDecimal("13.50"))
                .strategy("DISCOUNT")
                .item("米饭", 1, new BigDecimal("2.00"))
                .item("红烧肉", 1, new BigDecimal("11.50"))
                .build();
        assertEquals(2, tx.getItems().size());
        assertEquals(new BigDecimal("1.50"), tx.getDiscountAmount());
        assertEquals(new BigDecimal("13.50"), tx.getActualAmount());
        assertEquals("SUCCESS", tx.getStatus());
        // 缺少必填字段时建造者拒绝出厂
        assertThrows(BizException.class, () -> new TransactionBuilder().build());
    }

    @Test
    void strategySwitchesAlgorithmsAtRuntime() {
        Card card = studentCard();
        Account acc = account("500.00", 1200);
        assertEquals(new BigDecimal("15.00"),
                deduct("15.00", card, acc, new NormalStrategy()).calculate());
        assertEquals(new BigDecimal("13.50"),
                deduct("15.00", card, acc, new DiscountStrategy()).calculate());
        assertEquals(new BigDecimal("13.00"),
                deduct("15.00", card, acc, new SubsidyStrategy()).calculate());
    }

    @Test
    void chainExecutesInOrderAndShortCircuits() {
        List<String> order = new ArrayList<String>();
        ValidateHandler first = new ValidateHandler() {
            @Override
            protected void doValidate(ValidateContext ctx) {
                order.add("first");
            }
        };
        ValidateHandler reject = new ValidateHandler() {
            @Override
            protected void doValidate(ValidateContext ctx) {
                order.add("reject");
                ctx.reject(com.campus.card.common.ErrorCode.INVALID_AMOUNT, "金额非法");
            }
        };
        ValidateHandler third = new ValidateHandler() {
            @Override
            protected void doValidate(ValidateContext ctx) {
                order.add("third");
            }
        };
        first.setNext(reject).setNext(third);
        ValidateContext ctx = new ValidateContext();
        first.handle(ctx);
        assertEquals(Arrays.asList("first", "reject"), order);
        assertTrue(ctx.isRejected());
    }

    @Test
    void singletonIsUniqueAndIdempotencyWorks() {
        assertSame(SystemConfig.getInstance(), SystemConfig.getInstance());
        assertSame(IdempotencyCache.getInstance(), IdempotencyCache.getInstance());
        IdempotencyCache cache = IdempotencyCache.getInstance();
        cache.clear();
        assertTrue(cache.tryAcquire("K-001"));
        assertFalse(cache.tryAcquire("K-001"));
        cache.release("K-001");
        assertTrue(cache.tryAcquire("K-001"));
    }

    @Test
    void observerNotifiesAllSubscribers() {
        final AtomicInteger counter = new AtomicInteger(0);
        TransactionSubject subject = new TransactionSubject(new ArrayList<TransactionObserver>());
        subject.attach(new TransactionObserver() {
            @Override
            public void onEvent(TransactionEvent event) {
                counter.incrementAndGet();
            }
        });
        subject.attach(new TransactionObserver() {
            @Override
            public void onEvent(TransactionEvent event) {
                counter.addAndGet(10);
            }
        });
        assertEquals(2, subject.count());
        subject.publish(new TransactionEvent(new Transaction()));
        assertEquals(11, counter.get());
    }

    @Test
    void adapterConvertsVendorPayload() {
        RawTerminalMessage raw = new RawTerminalMessage();
        raw.setTerminalNo("POS-01");
        raw.setTerminalType("POS");
        raw.setSign("POS-01-0001");
        raw.setPayload("202601010001|15.00|123456|米饭:1:2.00;红烧肉:1:13.00");
        ConsumptionRequest req = new PosTerminalAdapter().adapt(raw);
        assertEquals("202601010001", req.getCardNo());
        assertEquals(new BigDecimal("15.00"), req.getAmount());
        assertEquals(2, req.getItems().size());
    }
}
