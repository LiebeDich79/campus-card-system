package com.campus.card.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 【单例模式】进程内幂等键缓存，配合数据库 uk_idempotency_key 唯一索引实现"防重复消费"。
 * 用 ConcurrentHashMap.putIfAbsent 的原子性保证同一幂等键在并发下只有一个请求能获得执行权。
 */
public final class IdempotencyCache {
    private static final long TTL_MILLIS = 10 * 60 * 1000L;
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<String, Long>();

    private IdempotencyCache() {
    }

    private static class Holder {
        private static final IdempotencyCache INSTANCE = new IdempotencyCache();
    }

    public static IdempotencyCache getInstance() {
        return Holder.INSTANCE;
    }

    /** @return true 表示首次请求，可以继续执行；false 表示重复请求。 */
    public boolean tryAcquire(String key) {
        if (key == null || key.trim().length() == 0) {
            return true;   // 未携带幂等键时，由数据库唯一索引兜底
        }
        long now = System.currentTimeMillis();
        Long previous = cache.putIfAbsent(key, now);
        if (previous == null) {
            return true;
        }
        if (now - previous > TTL_MILLIS) {
            cache.remove(key, previous);
            return cache.putIfAbsent(key, now) == null;
        }
        return false;
    }

    /** 业务失败或事务回滚时释放，避免误拦截后续重试。 */
    public void release(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }
}
