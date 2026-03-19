package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_menu")
public class AdminMenu {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /**
     * 前端路由路径（如 /admin/repairs）
     */
    private String path;

    /**
     * 所需权限Key；为空表示所有管理员可见
     */
    private String permissionKey;

    private Integer sortOrder;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

