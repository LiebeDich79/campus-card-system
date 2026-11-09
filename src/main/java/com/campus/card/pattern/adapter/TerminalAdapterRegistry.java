package com.campus.card.pattern.adapter;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 【适配器模式 · 注册表】按终端类型选择适配器，异构终端对业务层完全透明。 */
@Component
public class TerminalAdapterRegistry {
    private final Map<String, TerminalAdapter> adapters = new ConcurrentHashMap<String, TerminalAdapter>();

    public TerminalAdapterRegistry(List<TerminalAdapter> list) {
        for (TerminalAdapter adapter : list) {
            adapters.put(adapter.terminalType(), adapter);
        }
    }

    public ConsumptionRequest adapt(RawTerminalMessage raw) {
        TerminalAdapter adapter = adapters.get(raw.getTerminalType());
        if (adapter == null) {
            throw new BizException(ErrorCode.TERMINAL_NOT_FOUND, "没有适配的终端类型: " + raw.getTerminalType());
        }
        return adapter.adapt(raw);
    }

    public int size() {
        return adapters.size();
    }
}
