package com.campus.card.pattern.proxy;

import com.campus.card.common.BizException;
import com.campus.card.common.ErrorCode;
import com.campus.card.config.SystemConfig;
import com.campus.card.dao.TerminalDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【代理模式 · Proxy】JDK 动态代理，为 TransactionService 统一附加横切逻辑：
 * ① 终端鉴权  ② 每秒限流  ③ 调用日志与耗时统计。
 * 业务实现类只关心业务，横切关注点不侵入任何业务代码。
 */
public class ServiceInvocationHandler implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(ServiceInvocationHandler.class);
    private final Object target;
    private final TerminalDao terminalDao;
    private final Map<String, Window> windows = new ConcurrentHashMap<String, Window>();

    public ServiceInvocationHandler(Object target, TerminalDao terminalDao) {
        this.target = target;
        this.terminalDao = terminalDao;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        authenticate();
        rateLimit(method.getName());
        long start = System.currentTimeMillis();
        try {
            Object result = method.invoke(target, args);
            log.info("[PROXY] {} 执行成功，耗时 {} ms", method.getName(), System.currentTimeMillis() - start);
            return result;
        } catch (InvocationTargetException e) {
            log.warn("[PROXY] {} 执行失败: {}", method.getName(), e.getTargetException().getMessage());
            throw e.getTargetException();
        }
    }

    /** 若请求头携带终端编号，则校验终端密钥与状态；浏览器管理端请求不受影响。 */
    private void authenticate() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes)) {
            return;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
        String terminalNo = request.getHeader("X-Terminal-No");
        if (terminalNo == null || terminalNo.length() == 0) {
            return;
        }
        String sign = request.getHeader("X-Terminal-Sign");
        if (!terminalDao.isValid(terminalNo, sign)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "终端未注册、已停用或签名缺失");
        }
    }

    private void rateLimit(String method) {
        int maxPerSecond = SystemConfig.getInstance().getMaxQpsPerTerminal();
        long second = System.currentTimeMillis() / 1000L;
        String key = method + ":" + second;
        Window w = windows.computeIfAbsent(key, k -> new Window());
        if (w.counter.incrementAndGet() > maxPerSecond) {
            windows.remove(method + ":" + (second - 1));
            throw new BizException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后重试");
        }
        if (windows.size() > 200) {
            windows.clear();   // 简单清理，生产环境建议改用 Redis + 令牌桶
        }
    }

    private static class Window {
        private final AtomicInteger counter = new AtomicInteger(0);
    }
}
