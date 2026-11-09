package com.campus.card.service;

import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.dto.RefundRequest;
import com.campus.card.dto.TxResult;

/** 交易业务接口：代理模式的目标接口（RealSubject）。 */
public interface TransactionService {
    TxResult consume(ConsumptionRequest request);

    TxResult consumeRaw(RawTerminalMessage raw);

    TxResult refund(RefundRequest request);
}
