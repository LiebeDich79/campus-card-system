package com.campus.card.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;

/** 把 application.yml 的配置注入 SystemConfig 单例，兼顾"单例唯一"与"配置外部化"。 */
@Component
public class CampusProperties {
    @Value("${campus.single-limit:200.00}") private BigDecimal singleLimit;
    @Value("${campus.daily-limit:1000.00}") private BigDecimal dailyLimit;
    @Value("${campus.student-discount:0.90}") private BigDecimal studentDiscount;
    @Value("${campus.teacher-discount:0.95}") private BigDecimal teacherDiscount;
    @Value("${campus.subsidy-amount:2.00}") private BigDecimal subsidyAmount;
    @Value("${campus.water-price-per-liter:0.02}") private BigDecimal waterPricePerLiter;
    @Value("${campus.access-fee:0.10}") private BigDecimal accessFee;
    @Value("${campus.points-per-yuan:100}") private int pointsPerYuan;
    @Value("${campus.max-qps-per-terminal:20}") private int maxQpsPerTerminal;

    @PostConstruct
    public void applyToSingleton() {
        SystemConfig.getInstance().apply(singleLimit, dailyLimit, studentDiscount, teacherDiscount,
                subsidyAmount, waterPricePerLiter, accessFee, pointsPerYuan, maxQpsPerTerminal);
    }
}
