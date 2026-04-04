package com.campus.learningspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.reservation")
public class ReservationProperties {

    /**
     * 每人每周最多预约次数（与库表配置互补，库无记录时使用）
     */
    private int maxPerWeek = 4;

    /**
     * 单次最长时长（分钟）
     */
    private int maxDurationMinutes = 240;
}
