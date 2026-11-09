package com.campus.card.service;

import com.campus.card.entity.Terminal;

import java.util.List;
import java.util.Map;

/** 对账与终端查询业务接口。 */
public interface ReconcileService {
    List<Map<String, Object>> settle(String startDate, String endDate);

    List<Map<String, Object>> records();

    List<Terminal> terminals();
}
