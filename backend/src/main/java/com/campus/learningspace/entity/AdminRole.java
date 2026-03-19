package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_role")
public class AdminRole {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * 1-最大权限管理员, 2-审核评论员, 3-维修人员
     */
    private Integer roleType;

    /**
     * 对应 user.role 值（用于登录匹配）
     */
    private Integer userRole;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

