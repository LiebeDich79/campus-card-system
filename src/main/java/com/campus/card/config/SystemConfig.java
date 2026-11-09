package com.campus.card.config;

import java.math.BigDecimal;

/**
 * 【单例模式】全局唯一的业务参数与限额配置。
 * 采用静态内部类 Holder 实现，由 JVM 类加载机制保证线程安全与懒加载；
 * 配置值由 CampusProperties 在容器启动时注入，运行期以 volatile 保证可见性。
 */
public final class SystemConfig {
    private volatile BigDecimal singleLimit = new BigDecimal("200.00");
    private volatile BigDecimal dailyLimit = new BigDecimal("1000.00");
    private volatile BigDecimal studentDiscount = new BigDecimal("0.90");
    private volatile BigDecimal teacherDiscount = new BigDecimal("0.95");
    private volatile BigDecimal subsidyAmount = new BigDecimal("2.00");
    private volatile BigDecimal waterPricePerLiter = new BigDecimal("0.02");
    private volatile BigDecimal accessFee = new BigDecimal("0.10");
    private volatile int pointsPerYuan = 100;
    private volatile int maxQpsPerTerminal = 20;

    private SystemConfig() {
        // 私有构造，禁止外部 new
    }

    // 没用双重检查锁，静态内部类由 JVM 保证懒加载和线程安全，代码也短一截
    private static class Holder {
        private static final SystemConfig INSTANCE = new SystemConfig();
    }

    public static SystemConfig getInstance() {
        return Holder.INSTANCE;
    }

    /** 由 Spring 配置注入，保持单例唯一的同时支持参数可配置。 */
    public void apply(BigDecimal singleLimit, BigDecimal dailyLimit, BigDecimal studentDiscount,
                      BigDecimal teacherDiscount, BigDecimal subsidyAmount, BigDecimal waterPricePerLiter,
                      BigDecimal accessFee, int pointsPerYuan, int maxQpsPerTerminal) {
        this.singleLimit = singleLimit;
        this.dailyLimit = dailyLimit;
        this.studentDiscount = studentDiscount;
        this.teacherDiscount = teacherDiscount;
        this.subsidyAmount = subsidyAmount;
        this.waterPricePerLiter = waterPricePerLiter;
        this.accessFee = accessFee;
        this.pointsPerYuan = pointsPerYuan;
        this.maxQpsPerTerminal = maxQpsPerTerminal;
    }

    public BigDecimal getSingleLimit() {
        return singleLimit;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public BigDecimal getStudentDiscount() {
        return studentDiscount;
    }

    public BigDecimal getTeacherDiscount() {
        return teacherDiscount;
    }

    public BigDecimal getSubsidyAmount() {
        return subsidyAmount;
    }

    public BigDecimal getWaterPricePerLiter() {
        return waterPricePerLiter;
    }

    public BigDecimal getAccessFee() {
        return accessFee;
    }

    public int getPointsPerYuan() {
        return pointsPerYuan;
    }

    public int getMaxQpsPerTerminal() {
        return maxQpsPerTerminal;
    }
}
