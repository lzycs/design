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

    /**
     * 资源类型: 1-教室, 2-图书馆座位
     */
    private Integer resourceType;

    /**
     * 教室ID (当resource_type=1时)
     */
    private Long classroomId;

    /**
     * 图书馆座位ID (当resource_type=2时)
     */
    private Long librarySeatId;

    private LocalDate reservationDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer duration;

    private String purpose;

    /**
     * 状态: 1-待签到, 2-已签到, 3-已完成, 4-已取消, 5-已违约
     */
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
