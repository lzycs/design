package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reservation_limit_config")
public class ReservationLimitConfig {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private Integer maxPerWeek;

    private Integer maxDurationMinutes;

    private LocalDateTime updateTime;
}
