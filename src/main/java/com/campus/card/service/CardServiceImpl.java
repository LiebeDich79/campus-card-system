package com.campus.card.service;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.config.IdempotencyCache;
import com.campus.card.dao.AccountDao;
import com.campus.card.dao.CardDao;
import com.campus.card.dao.TransactionDao;
import com.campus.card.dto.RechargeRequest;
import com.campus.card.dto.TransactionContext;
import com.campus.card.dto.TxResult;
import com.campus.card.entity.Account;
import com.campus.card.entity.Card;
import com.campus.card.entity.Transaction;
import com.campus.card.pattern.decorator.SettlementFacade;
import com.campus.card.pattern.factory.TransactionFactory;
import com.campus.card.pattern.observer.TransactionEventPublisher;
import com.campus.card.util.IdGenerator;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** 卡片与账户业务实现：只做业务编排，横切逻辑交给代理，账务落库交给 DAO。 */
@Service
public class CardServiceImpl implements CardService {
    private final CardDao cardDao;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final TransactionFactory factory;
    private final SettlementFacade settlement;
    private final TransactionEventPublisher publisher;

    public CardServiceImpl(CardDao cardDao, AccountDao accountDao, TransactionDao transactionDao,
                           TransactionFactory factory, SettlementFacade settlement,
                           TransactionEventPublisher publisher) {
        this.cardDao = cardDao;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.factory = factory;
        this.settlement = settlement;
        this.publisher = publisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Card open(Card card, BigDecimal initialBalance) {
        if (card.getCardNo() == null || card.getCardNo().trim().length() == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "卡号不能为空");
        }
        if (cardDao.findByCardNo(card.getCardNo()) != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "卡号已存在: " + card.getCardNo());
        }
        BigDecimal balance = initialBalance == null ? BigDecimal.ZERO : initialBalance;
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ErrorCode.INVALID_AMOUNT, "初始余额不能为负");
        }
        card.setStatus("NORMAL");
        card.setVersion(0);
        cardDao.insert(card);
        Card saved = cardDao.findByCardNo(card.getCardNo());
        accountDao.create(saved.getId(), MoneyUtils.scale(balance));
        return saved;
    }

    @Override
    public Card detail(String cardNo) {
        Card card = cardDao.findByCardNo(cardNo);
        if (card == null) {
            throw new BizException(ErrorCode.CARD_NOT_FOUND, "校园卡不存在: " + cardNo);
        }
        return card;
    }

    @Override
    public List<Card> list() {
        return cardDao.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Card lost(String cardNo) {
        Card card = detail(cardNo);
        if (!"NORMAL".equals(card.getStatus())) {
            throw new BizException(ErrorCode.CARD_ABNORMAL, "仅正常状态的卡片可挂失");
        }
        cardDao.updateStatus(cardNo, "LOST");
        return detail(cardNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Card cancel(String cardNo) {
        Card card = detail(cardNo);
        if ("CANCELLED".equals(card.getStatus())) {
            throw new BizException(ErrorCode.CARD_ABNORMAL, "卡片已注销");
        }
        Account account = accountDao.findByCardId(card.getId());
        if (account != null && account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "账户仍有余额，请先办理退款再销户");
        }
        cardDao.updateStatus(cardNo, "CANCELLED");
        return detail(cardNo);
    }

    /** 充值：与消费共用工厂、装饰链与事件发布，体现模式在不同交易类型上的复用。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TxResult recharge(RechargeRequest request) {
        if (!MoneyUtils.isPositive(request.getAmount()) || MoneyUtils.overTwoDecimals(request.getAmount())) {
            throw new BizException(ErrorCode.INVALID_AMOUNT, "充值金额必须大于 0 且最多两位小数");
        }
        String key = request.getIdempotencyKey();
        if (key != null && transactionDao.findByIdempotencyKey(key) != null) {
            throw new BizException(ErrorCode.DUPLICATE_REQUEST, "重复充值请求");
        }
        Card card = detail(request.getCardNo());
        if (!"NORMAL".equals(card.getStatus())) {
            throw new BizException(ErrorCode.CARD_ABNORMAL, "卡片状态异常，不能充值");
        }
        accountDao.atomicAdd(card.getId(), request.getAmount());

        TransactionContext ctx = new TransactionContext();
        ctx.setTxNo(IdGenerator.txNo());
        ctx.setCard(card);
        ctx.setTxType("RECHARGE");
        ctx.setOriginalAmount(request.getAmount());
        ctx.setPayableAmount(request.getAmount());
        ctx.setIdempotencyKey(key);
        ctx.setOperator(request.getOperator());
        Transaction tx = factory.createTransaction("RECHARGE", ctx);
        transactionDao.insert(tx);
        transactionDao.insertItems(tx.getTxNo(), tx.getItems());
        settlement.settle(tx);
        publisher.publishAfterCommit(tx);

        Account after = accountDao.findByCardId(card.getId());
        IdempotencyCache.getInstance().release(key);
        TxResult result = new TxResult();
        result.setTxNo(tx.getTxNo());
        result.setTxType("RECHARGE");
        result.setAmount(tx.getAmount());
        result.setActualAmount(tx.getActualAmount());
        result.setDiscountAmount(tx.getDiscountAmount());
        result.setStrategyType("NORMAL");
        result.setStatus("SUCCESS");
        result.setBalance(after.getBalance());
        result.setMessage("充值成功");
        result.setOccurredAt(new Date());
        return result;
    }
}
