package com.campus.card.pattern.strategy;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.entity.Card;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【策略模式 · 策略选择器】把"何时用哪个策略"的判断集中到一处，
 * 业务编排代码里不再出现 if/else 叠加；新增策略只需实现接口并注册为 Spring Bean。
 */
@Component
public class DeductStrategyResolver {
    private final Map<String, DeductStrategy> strategies = new ConcurrentHashMap<String, DeductStrategy>();

    public DeductStrategyResolver(List<DeductStrategy> list) {
        for (DeductStrategy s : list) {
            strategies.put(s.type(), s);
        }
    }

    public DeductStrategy resolve(ConsumptionRequest request, Card card) {
        // 1) 请求显式指定优先（前端可让用户主动选择优惠方式）
        if (request.getStrategyType() != null && request.getStrategyType().length() > 0) {
            return byType(request.getStrategyType());
        }
        // 2) 午餐时段（11:00-13:00）食堂 POS 的学生卡自动享受补贴
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if ("POS".equals(request.getTerminalType()) && "STUDENT".equals(card.getCardType())
                && hour >= 11 && hour < 13) {
            return byType("SUBSIDY");
        }
        // 3) 学生/教师卡默认折扣，其余按原价
        if ("STUDENT".equals(card.getCardType()) || "TEACHER".equals(card.getCardType())) {
            return byType("DISCOUNT");
        }
        return byType("NORMAL");
    }

    public DeductStrategy byType(String type) {
        DeductStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "未知的优惠策略: " + type);
        }
        return strategy;
    }
}
