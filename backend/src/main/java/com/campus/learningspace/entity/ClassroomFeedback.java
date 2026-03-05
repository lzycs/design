package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("classroom_feedback")
public class ClassroomFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long classroomId;

    private Long reservationId;

    /**
     * 整体环境评分 1-5
     */
    private Integer envScore;

    /**
     * 设备设施评分 1-5
     */
    private Integer equipScore;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 状态: 1-待评价(预留), 2-已评价
     */
    private Integer status;

    private LocalDateTime usedStartTime;

    private LocalDateTime usedEndTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

