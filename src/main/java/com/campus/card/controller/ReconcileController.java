package com.campus.card.controller;

import com.campus.card.common.Result;
import com.campus.card.service.ReconcileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reconcile")
public class ReconcileController {
    private final ReconcileService reconcileService;

    public ReconcileController(ReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    @PostMapping("/settle")
    public Result<List<Map<String, Object>>> settle(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        String start = startDate == null ? LocalDate.now().toString() : startDate;
        String end = endDate == null ? LocalDate.now().toString() : endDate;
        return Result.ok(reconcileService.settle(start, end));
    }

    @GetMapping("/records")
    public Result<List<Map<String, Object>>> records() {
        return Result.ok(reconcileService.records());
    }
}
