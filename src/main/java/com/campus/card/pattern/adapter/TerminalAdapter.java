package com.campus.card.pattern.adapter;

import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;

/** 【适配器模式 · Target】系统统一的终端报文转换接口。 */
public interface TerminalAdapter {
    String terminalType();

    ConsumptionRequest adapt(RawTerminalMessage raw);
}
