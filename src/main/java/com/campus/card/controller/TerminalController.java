package com.campus.card.controller;

import com.campus.card.common.Result;
import com.campus.card.entity.Terminal;
import com.campus.card.service.ReconcileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terminals")
public class TerminalController {
    private final ReconcileService reconcileService;

    public TerminalController(ReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    @GetMapping
    public Result<List<Terminal>> list() {
        return Result.ok(reconcileService.terminals());
    }
}
