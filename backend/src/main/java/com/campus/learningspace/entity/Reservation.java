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

    /** 二维码有效期（预约二维码） */
    private LocalDateTime qrcodeExpireTime;

    private LocalDateTime checkinTime;

    /** 扫码尝试/成功的扫码时间（设备端扫码预约二维码） */
    private LocalDateTime qrcodeScanTime;

    /** 扫码设备唯一标识（设备端） */
    private String qrcodeScanDeviceUid;

    /**
     * 二维码扫码结果：
     * 0-未使用/未尝试
     * 1-预约成功
     * 2-非授权设备
     * 3-二维码已过期
     * 4-二维码无效/已使用
     */
    private Integer qrcodeScanStatus;

    private BigDecimal checkinLatitude;

    private BigDecimal checkinLongitude;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
