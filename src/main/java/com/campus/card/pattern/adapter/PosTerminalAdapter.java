package com.campus.card.pattern.adapter;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.entity.TransactionItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 【适配器模式 · Adapter ①】食堂 POS（新中新厂商）：竖线分隔报文
 * 格式 CARDNO|AMOUNT|PIN|ITEM:qty:price;ITEM:qty:price
 */
@Component
public class PosTerminalAdapter implements TerminalAdapter {
    @Override
    public String terminalType() {
        return "POS";
    }

    @Override
    public ConsumptionRequest adapt(RawTerminalMessage raw) {
        // 这个 -1 别删：末尾明细段为空时会少一段，之前就这么丢过明细
        String[] parts = raw.getPayload().split("\\|", -1);
        if (parts.length < 3) {
            throw new BizException(ErrorCode.TERMINAL_PAYLOAD_INVALID, "POS 报文至少需要 3 段");
        }
        ConsumptionRequest req = new ConsumptionRequest();
        req.setCardNo(parts[0].trim());
        req.setAmount(parseAmount(parts[1].trim()));
        req.setPin(parts[2].trim());
        req.setTerminalNo(raw.getTerminalNo());
        req.setTerminalType("POS");
        req.setIdempotencyKey(raw.getSign());
        req.setOperator("POS:" + raw.getTerminalNo());
        List<TransactionItem> items = new ArrayList<TransactionItem>();
        if (parts.length >= 4 && parts[3].trim().length() > 0) {
            for (String seg : parts[3].split(";")) {
                String[] col = seg.split(":");
                if (col.length == 3) {
                    TransactionItem item = new TransactionItem();
                    item.setItemName(col[0]);
                    item.setQuantity(Integer.parseInt(col[1]));
                    item.setUnitPrice(new BigDecimal(col[2]));
                    item.setAmount(new BigDecimal(col[2]).multiply(new BigDecimal(col[1])));
                    items.add(item);
                }
            }
        }
        req.setItems(items);
        return req;
    }

    /** 数字格式异常转换为统一业务异常，避免把运行时异常直接暴露给终端。 */
    private BigDecimal parseAmount(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.TERMINAL_PAYLOAD_INVALID, "POS 报文金额非法: " + text);
        }
    }
}
