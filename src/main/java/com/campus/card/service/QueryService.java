package com.campus.card.service;

import java.util.List;
import java.util.Map;

/** 查询与统计业务接口。 */
public interface QueryService {
    Map<String, Object> transactions(String cardNo, String terminalType, String startDate,
                                     String endDate, int page, int size);

    Map<String, Object> summary(String startDate, String endDate);

    List<Map<String, Object>> notifications();
}
