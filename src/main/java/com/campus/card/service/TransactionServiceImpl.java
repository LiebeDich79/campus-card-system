package com.campus.card.service;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.config.IdempotencyCache;
import com.campus.card.config.SystemConfig;
import com.campus.card.dao.AccountDao;
import com.campus.card.dao.CardDao;
import com.campus.card.dao.TerminalDao;
import com.campus.card.dao.TransactionDao;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.DeductContext;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.dto.RefundRequest;
import com.campus.card.dto.TransactionContext;
import com.campus.card.dto.TxResult;
import com.campus.card.dto.ValidateContext;
import com.campus.card.entity.Account;
import com.campus.card.entity.Card;
import com.campus.card.entity.Terminal;
import com.campus.card.entity.Transaction;
import com.campus.card.pattern.adapter.TerminalAdapterRegistry;
import com.campus.card.pattern.chain.ValidationChain;
import com.campus.card.pattern.decorator.SettlementFacade;
import com.campus.card.pattern.factory.TransactionFactory;
import com.campus.card.pattern.observer.TransactionEventPublisher;
import com.campus.card.pattern.strategy.DeductStrategy;
import com.campus.card.pattern.strategy.DeductStrategyResolver;
import com.campus.card.util.IdGenerator;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 交易主流程编排（业务层核心）。
 * 稳定点：流程骨架与事务边界；变化点：全部委托给各设计模式，故本类不含任何算法分支。
 * 顺序：适配器 → 策略 → 责任链 → 工厂/建造者 → 装饰结算 → 观察者通知。
 */
@Service
public class TransactionServiceImpl implements TransactionService {
    private final CardDao cardDao;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final TerminalDao terminalDao;
    private final TerminalAdapterRegistry adapterRegistry;
    private final ValidationChain validationChain;
    private final DeductStrategyResolver strategyResolver;
    private final TransactionFactory transactionFactory;
    private final SettlementFacade settlement;
    private final TransactionEventPublisher publisher;

    public TransactionServiceImpl(CardDao cardDao, AccountDao accountDao, TransactionDao transactionDao,
                                  TerminalDao terminalDao, TerminalAdapterRegistry adapterRegistry,
                                  ValidationChain validationChain, DeductStrategyResolver strategyResolver,
                                  TransactionFactory transactionFactory, SettlementFacade settlement,
                                  TransactionEventPublisher publisher) {
        this.cardDao = cardDao;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.terminalDao = terminalDao;
        this.adapterRegistry = adapterRegistry;
        this.validationChain = validationChain;
        this.strategyResolver = strategyResolver;
        this.transactionFactory = transactionFactory;
        this.settlement = settlement;
        this.publisher = publisher;
    }

    /** 终端原始报文入口：适配器把异构报文转换为统一请求后再走主流程。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TxResult consumeRaw(RawTerminalMessage raw) {
        return consume(adapterRegistry.adapt(raw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TxResult consume(ConsumptionRequest request) {
        final String idemKey = request.getIdempotencyKey();
        Card card = cardDao.findByCardNo(request.getCardNo());
        if (card == null) {
            throw new BizException(ErrorCode.CARD_NOT_FOUND, "校园卡不存在: " + request.getCardNo());
        }
        Account account = accountDao.findByCardId(card.getId());

        try {
            // ① 策略：算实付。这里只管顺序，具体算法都在各自的策略类里
            DeductContext deduct = new DeductContext();
            deduct.setCard(card);
            deduct.setAccount(account);
            deduct.setAmount(request.getAmount());
            deduct.setTerminalType(request.getTerminalType());
            deduct.setConfig(SystemConfig.getInstance());
            DeductStrategy strategy = strategyResolver.resolve(request, card);
            deduct.setStrategy(strategy);
            BigDecimal payable = MoneyUtils.scale(deduct.calculate());

            // ② 责任链：幂等 → 卡状态 → 金额 → 限额 → 余额 → 原子扣款
            ValidateContext validate = new ValidateContext();
            validate.setRequest(request);
            validate.setCard(card);
            validate.setAccount(account);
            validate.setPayableAmount(payable);
            validate.setIdempotencyKey(idemKey);
            validationChain.execute(validate);
            if (validate.isRejected()) {
                throw new BizException(validate.getErrorCode(), validate.getMessage());
            }

            // ③ 工厂 + 建造者：生成交易聚合根与明细
            TransactionContext ctx = new TransactionContext();
            ctx.setTxNo(IdGenerator.txNo());
            ctx.setCard(card);
            ctx.setTxType("CONSUME");
            ctx.setOriginalAmount(request.getAmount());
            ctx.setPayableAmount(payable);
            ctx.setStrategyType(strategy.type());
            ctx.setTerminalNo(request.getTerminalNo());
            ctx.setTerminalType(request.getTerminalType());
            ctx.setItems(request.getItems());
            ctx.setIdempotencyKey(idemKey);
            ctx.setOperator(request.getOperator());
            ctx.setBalanceAfter(validate.getAccount().getBalance());
            Transaction tx = transactionFactory.createTransaction("CONSUME", ctx);
            Terminal terminal = terminalDao.findByTerminalNo(request.getTerminalNo());
            if (terminal != null) {
                tx.setTerminalId(terminal.getId());
            }

            // ④ 持久化：主表与明细在同一事务内写入
            transactionDao.insert(tx);
            transactionDao.insertItems(tx.getTxNo(), tx.getItems());

            // ⑤ 装饰链：基础结算 + 积分 + 审计日志
            settlement.settle(tx);

            // ⑥ 观察者：事务提交后通知对账、统计、推送与积分流水
            publisher.publishAfterCommit(tx);

            return buildResult(tx, validate.getAccount().getBalance(), "消费扣款成功");
        } catch (BizException e) {
            // 业务失败释放幂等键，允许修正参数后重试；重复请求本身不释放
            if (e.getErrorCode() != ErrorCode.DUPLICATE_REQUEST) {
                IdempotencyCache.getInstance().release(idemKey);
            }
            throw e;
        }
    }

    /** 退款/冲正：原交易置为 REVERSED，生成一笔反向交易，并回补账户余额。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TxResult refund(RefundRequest request) {
        Transaction origin = transactionDao.findByTxNo(request.getOriginalTxNo());
        if (origin == null) {
            throw new BizException(ErrorCode.TX_NOT_FOUND, "原交易不存在");
        }
        if (!"SUCCESS".equals(origin.getStatus()) || !"CONSUME".equals(origin.getTxType())) {
            throw new BizException(ErrorCode.TX_ALREADY_REVERSED, "原交易已冲正或不是消费交易");
        }
        BigDecimal amount = request.getAmount() == null ? origin.getActualAmount() : request.getAmount();
        if (!MoneyUtils.isPositive(amount) || amount.compareTo(origin.getActualAmount()) > 0) {
            throw new BizException(ErrorCode.REFUND_AMOUNT_INVALID, "退款金额必须大于 0 且不超过原实付金额");
        }
        if (transactionDao.findByIdempotencyKey(request.getIdempotencyKey()) != null) {
            throw new BizException(ErrorCode.DUPLICATE_REQUEST, "重复退款请求");
        }
        accountDao.atomicAdd(origin.getCardId(), amount);
        transactionDao.markReversed(origin.getTxNo());

        Card card = cardDao.findByCardNo(origin.getCardNo());
        TransactionContext ctx = new TransactionContext();
        ctx.setTxNo(IdGenerator.txNo());
        ctx.setCard(card);
        ctx.setTxType("REFUND");
        ctx.setOriginalAmount(amount);
        ctx.setPayableAmount(amount);
        ctx.setIdempotencyKey(request.getIdempotencyKey());
        ctx.setOperator(request.getOperator());
        ctx.setOriginalTxNo(origin.getTxNo());
        ctx.setRemark(request.getReason());
        Transaction tx = transactionFactory.createTransaction("REFUND", ctx);
        tx.setTerminalId(origin.getTerminalId());
        tx.setTerminalNo(origin.getTerminalNo());
        transactionDao.insert(tx);
        transactionDao.insertItems(tx.getTxNo(), tx.getItems());
        settlement.settle(tx);
        publisher.publishAfterCommit(tx);

        Account after = accountDao.findByCardId(origin.getCardId());
        return buildResult(tx, after.getBalance(), "退款冲正成功");
    }

    private TxResult buildResult(Transaction tx, BigDecimal balance, String message) {
        TxResult result = new TxResult();
        result.setTxNo(tx.getTxNo());
        result.setTxType(tx.getTxType());
        result.setAmount(tx.getAmount());
        result.setDiscountAmount(tx.getDiscountAmount());
        result.setActualAmount(tx.getActualAmount());
        result.setStrategyType(tx.getStrategyType());
        result.setBalance(balance);
        result.setStatus("SUCCESS");
        result.setMessage(message);
        result.setOccurredAt(new Date());
        return result;
    }
}
