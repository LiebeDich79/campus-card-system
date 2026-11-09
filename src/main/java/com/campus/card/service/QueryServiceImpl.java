package com.campus.card.service;

import com.campus.card.dao.NotifyLogDao;
import com.campus.card.dao.TransactionDao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 查询统计实现：分页明细、按终端/时段/类别汇总、消息记录。 */
@Service
public class QueryServiceImpl implements QueryService {
    private final TransactionDao transactionDao;
    private final NotifyLogDao notifyLogDao;

    public QueryServiceImpl(TransactionDao transactionDao, NotifyLogDao notifyLogDao) {
        this.transactionDao = transactionDao;
        this.notifyLogDao = notifyLogDao;
    }

    @Override
    public Map<String, Object> transactions(String cardNo, String terminalType, String startDate,
                                            String endDate, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("total", transactionDao.count(cardNo, startDate, endDate));
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("list", transactionDao.query(cardNo, terminalType, startDate, endDate, safePage, safeSize));
        return data;
    }

    @Override
    public Map<String, Object> summary(String startDate, String endDate) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("startDate", startDate);
        data.put("endDate", endDate);
        data.put("consumeTotal", transactionDao.sumActual(startDate, endDate, "CONSUME"));
        data.put("rechargeTotal", transactionDao.sumActual(startDate, endDate, "RECHARGE"));
        data.put("refundTotal", transactionDao.sumActual(startDate, endDate, "REFUND"));
        data.put("txCount", transactionDao.count(null, startDate, endDate));
        data.put("groupByTerminal", transactionDao.groupByTerminal(startDate, endDate));
        BigDecimal diff = ((BigDecimal) data.get("consumeTotal"))
                .subtract((BigDecimal) data.get("refundTotal"));
        data.put("netAmount", diff);
        return data;
    }

    @Override
    public List<Map<String, Object>> notifications() {
        return notifyLogDao.recent(20);
    }
}
