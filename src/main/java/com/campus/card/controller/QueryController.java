package com.campus.card.controller;

import com.campus.card.common.Result;
import com.campus.card.service.QueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/transactions")
    public Result<Map<String, Object>> transactions(
            @RequestParam(required = false) String cardNo,
            @RequestParam(required = false) String terminalType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String start = startDate == null ? LocalDate.now().minusDays(30).toString() : startDate;
        String end = endDate == null ? LocalDate.now().toString() : endDate;
        return Result.ok(queryService.transactions(cardNo, terminalType, start, end, page, size));
    }

    @GetMapping("/statistics/summary")
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String start = startDate == null ? LocalDate.now().minusDays(30).toString() : startDate;
        String end = endDate == null ? LocalDate.now().toString() : endDate;
        return Result.ok(queryService.summary(start, end));
    }

    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> notifications() {
        return Result.ok(queryService.notifications());
    }
}
