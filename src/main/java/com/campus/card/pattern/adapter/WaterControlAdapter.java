package com.campus.card.pattern.adapter;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.config.SystemConfig;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 【适配器模式 · Adapter ④】水控（新中新厂商）：报文为 CARDNO|用水量(升)，按单价换算金额。 */
@Component
public class WaterControlAdapter implements TerminalAdapter {
    @Override
    public String terminalType() {
        return "WATER";
    }

    @Override
    public ConsumptionRequest adapt(RawTerminalMessage raw) {
        String[] parts = raw.getPayload().split("\\|", -1);
        if (parts.length < 2) {
            throw new BizException(ErrorCode.TERMINAL_PAYLOAD_INVALID, "水控报文格式应为 CARDNO|LITERS");
        }
        BigDecimal liters = new BigDecimal(parts[1].trim());
        ConsumptionRequest req = new ConsumptionRequest();
        req.setCardNo(parts[0].trim());
        req.setAmount(MoneyUtils.scale(liters.multiply(SystemConfig.getInstance().getWaterPricePerLiter())));
        req.setTerminalNo(raw.getTerminalNo());
        req.setTerminalType("WATER");
        req.setStrategyType("NORMAL");
        req.setIdempotencyKey(raw.getSign());
        req.setOperator("WATER:" + raw.getTerminalNo());
        return req;
    }
}
