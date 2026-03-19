package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

@Data
@TableName("admin_role_permission")
public class AdminRolePermission {
    private Long roleId;
    private Long permissionId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

