package com.campus.card.controller;

import com.campus.card.common.Result;
import com.campus.card.dto.RechargeRequest;
import com.campus.card.dto.TxResult;
import com.campus.card.entity.Card;
import com.campus.card.service.CardService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** 表示层：账户与卡片管理 REST 接口。 */
@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public Result<Card> open(@RequestBody Card card,
                             @RequestParam(required = false, defaultValue = "0") BigDecimal balance) {
        return Result.ok(cardService.open(card, balance));
    }

    @GetMapping
    public Result<List<Card>> list() {
        return Result.ok(cardService.list());
    }

    @GetMapping("/{cardNo}")
    public Result<Card> detail(@PathVariable String cardNo) {
        return Result.ok(cardService.detail(cardNo));
    }

    @PutMapping("/{cardNo}/lost")
    public Result<Card> lost(@PathVariable String cardNo) {
        return Result.ok(cardService.lost(cardNo));
    }

    @DeleteMapping("/{cardNo}")
    public Result<Card> cancel(@PathVariable String cardNo) {
        return Result.ok(cardService.cancel(cardNo));
    }

    @PostMapping("/{cardNo}/recharge")
    public Result<TxResult> recharge(@PathVariable String cardNo, @RequestBody RechargeRequest request) {
        request.setCardNo(cardNo);
        return Result.ok(cardService.recharge(request));
    }
}
