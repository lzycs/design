package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备配对 token（admin 生成，设备扫码兑换并绑定 deviceUid）
 */
@Data
@TableName("scan_device_pair_token")
public class ScanDevicePairToken {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 一次性 token，写入二维码用于配对
     */
    private String token;

    /**
     * 过期时间
     */
    private LocalDateTime expiredTime;

    /**
     * 是否已使用：0-未使用，1-已使用
     */
    private Integer usedStatus;

    private LocalDateTime usedTime;

    /**
     * 兑换到的 deviceUid
     */
    private String redeemedDeviceUid;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

