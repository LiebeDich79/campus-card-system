package com.campus.card.controller;

import com.campus.card.common.Result;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.dto.RefundRequest;
import com.campus.card.dto.TxResult;
import com.campus.card.service.TransactionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 表示层：消费、终端报文接入与退款接口。注入的是【代理模式】包装后的服务对象。 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(@Qualifier("transactionServiceProxy") TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/consume")
    public Result<TxResult> consume(@Valid @RequestBody ConsumptionRequest request) {
        return Result.ok(transactionService.consume(request));
    }

    @PostMapping("/consume/raw")
    public Result<TxResult> consumeRaw(@RequestBody RawTerminalMessage raw) {
        return Result.ok(transactionService.consumeRaw(raw));
    }

    @PostMapping("/refund")
    public Result<TxResult> refund(@RequestBody RefundRequest request) {
        return Result.ok(transactionService.refund(request));
    }
}
