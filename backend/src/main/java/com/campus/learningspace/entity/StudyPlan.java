package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("study_plan")
public class StudyPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamRequestId;

    private Long userId;

    private Long reservationId;

    private String title;

    private String description;

    private LocalDate planDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
