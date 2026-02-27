package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long classroomId;

    private LocalDate reservationDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer duration;

    private String purpose;

    private Integer status;

    private String qrcode;

    private LocalDateTime checkinTime;

    private BigDecimal checkinLatitude;

    private BigDecimal checkinLongitude;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
