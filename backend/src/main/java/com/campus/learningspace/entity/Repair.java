package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair")
public class Repair {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long classroomId;

    private String title;

    private String description;

    private String images;

    private Integer type;

    private Integer priority;

    private Integer status;

    private Long handlerId;

    private LocalDateTime handleTime;

    private String handleRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
