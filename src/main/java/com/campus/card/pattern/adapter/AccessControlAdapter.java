package com.campus.card.pattern.adapter;

import com.campus.card.config.SystemConfig;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import org.springframework.stereotype.Component;

/** 【适配器模式 · Adapter ③】门禁（达实厂商）：报文仅为卡号，金额取系统配置的固定通行费。 */
@Component
public class AccessControlAdapter implements TerminalAdapter {
    @Override
    public String terminalType() {
        return "ACCESS";
    }

    @Override
    public ConsumptionRequest adapt(RawTerminalMessage raw) {
        ConsumptionRequest req = new ConsumptionRequest();
        req.setCardNo(raw.getPayload().trim());
        req.setAmount(SystemConfig.getInstance().getAccessFee());
        req.setTerminalNo(raw.getTerminalNo());
        req.setTerminalType("ACCESS");
        req.setStrategyType("NORMAL");
        req.setIdempotencyKey(raw.getSign());
        req.setOperator("ACCESS:" + raw.getTerminalNo());
        return req;
    }
}
