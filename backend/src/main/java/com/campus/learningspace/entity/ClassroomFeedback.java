package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("classroom_feedback")
public class ClassroomFeedback {

    public static final int STATUS_PENDING_EVALUATION = 1;
    public static final int STATUS_PENDING_AUDIT = 2;
    public static final int STATUS_APPROVED = 3;
    public static final int STATUS_REJECTED = 4;

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
     * 状态: 1-待评价, 2-待审核, 3-审核通过, 4-审核驳回
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

