package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_permission")
public class AdminPermission {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String permKey;

    private String name;

    private String description;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

