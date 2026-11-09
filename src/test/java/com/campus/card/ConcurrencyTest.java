package com.campus.card;

import com.campus.card.config.IdempotencyCache;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** 并发测试：验证幂等键在多线程下只有一个请求能获得执行权。 */
class ConcurrencyTest {

    @Test
    void sameIdempotencyKeyOnlyOneThreadWins() throws Exception {
        IdempotencyCache cache = IdempotencyCache.getInstance();
        cache.clear();
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger accepted = new AtomicInteger(0);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (cache.tryAcquire("SAME-KEY")) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(1, accepted.get(), "同一幂等键并发 100 次，只允许 1 次通过");
    }

    @Test
    void differentKeysAllPassConcurrently() throws Exception {
        IdempotencyCache cache = IdempotencyCache.getInstance();
        cache.clear();
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger accepted = new AtomicInteger(0);
        for (int i = 0; i < threads; i++) {
            final String key = "KEY-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    if (cache.tryAcquire(key)) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(threads, accepted.get());
    }
}
