package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_availability")
public class UserAvailability {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 1-7（周一到周日） */
    private Integer weekDay;
    private Long slotId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
