package com.campus.card.pattern.adapter;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.dto.ConsumptionRequest;
import com.campus.card.dto.RawTerminalMessage;
import com.campus.card.entity.TransactionItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 【适配器模式 · Adapter ②】校园超市（正方厂商）：JSON 报文
 * 格式 {"cardNo":"...","amount":15.00,"pin":"...","items":[{"name":"...","qty":2,"price":3.50}]}
 */
@Component
public class SupermarketAdapter implements TerminalAdapter {
    private final ObjectMapper objectMapper;

    public SupermarketAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String terminalType() {
        return "SUPERMARKET";
    }

    @Override
    public ConsumptionRequest adapt(RawTerminalMessage raw) {
        try {
            JsonNode root = objectMapper.readTree(raw.getPayload());
            ConsumptionRequest req = new ConsumptionRequest();
            req.setCardNo(root.path("cardNo").asText());
            req.setAmount(new BigDecimal(root.path("amount").asText("0")));
            req.setPin(root.path("pin").asText(""));
            req.setTerminalNo(raw.getTerminalNo());
            req.setTerminalType("SUPERMARKET");
            req.setIdempotencyKey(raw.getSign());
            req.setOperator("MART:" + raw.getTerminalNo());
            List<TransactionItem> items = new ArrayList<TransactionItem>();
            for (JsonNode n : root.path("items")) {
                TransactionItem item = new TransactionItem();
                item.setItemName(n.path("name").asText("商品"));
                item.setQuantity(n.path("qty").asInt(1));
                item.setUnitPrice(new BigDecimal(n.path("price").asText("0")));
                item.setAmount(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
                items.add(item);
            }
            req.setItems(items);
            return req;
        } catch (Exception e) {
            throw new BizException(ErrorCode.TERMINAL_PAYLOAD_INVALID, "超市 JSON 报文解析失败");
        }
    }
}
