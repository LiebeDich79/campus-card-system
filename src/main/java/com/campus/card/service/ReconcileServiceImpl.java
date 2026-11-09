package com.campus.card.service;

import com.campus.card.dao.ReconcileDao;
import com.campus.card.dao.TerminalDao;
import com.campus.card.entity.Terminal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 对账实现：按终端 + 日期汇总交易流水，输出差异核对结果。 */
@Service
public class ReconcileServiceImpl implements ReconcileService {
    private final ReconcileDao reconcileDao;
    private final TerminalDao terminalDao;

    public ReconcileServiceImpl(ReconcileDao reconcileDao, TerminalDao terminalDao) {
        this.reconcileDao = reconcileDao;
        this.terminalDao = terminalDao;
    }

    @Override
    public List<Map<String, Object>> settle(String startDate, String endDate) {
        return reconcileDao.settle(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> records() {
        return reconcileDao.findAll();
    }

    @Override
    public List<Terminal> terminals() {
        return terminalDao.findAll();
    }
}
