package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扫码授权设备白名单
 */
@Data
@TableName("scan_device")
public class ScanDevice {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备唯一标识（前端生成 deviceUid，用于区分授权设备）
     */
    private String deviceUid;

    /**
     * 设备备注名称（admin 配置）
     */
    private String deviceName;

    /**
     * 是否启用
     */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

