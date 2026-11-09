package com.campus.card.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** 交易流水号生成器：时间前缀 + 进程内自增序列，兼顾可读性与唯一性。 */
public final class IdGenerator {
    private static final AtomicLong SEQ = new AtomicLong(0);

    private IdGenerator() {
    }

    public static String txNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        long seq = SEQ.incrementAndGet() % 1000000L;
        return "TX" + time + String.format("%06d", seq);
    }

    public static String idempotencyKey(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
