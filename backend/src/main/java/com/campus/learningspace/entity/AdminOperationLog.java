package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_operation_log")
public class AdminOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminUserId;

    private Long adminRoleId;

    private String action;

    /**
     * 操作详情（JSON/文本）
     */
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

