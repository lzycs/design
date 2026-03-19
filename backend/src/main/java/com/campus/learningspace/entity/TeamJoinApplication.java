package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 加入小组申请表
 * status: 0-待审核, 1-已通过, 2-已拒绝
 */
@Data
@TableName("team_join_application")
public class TeamJoinApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请加入的小组ID */
    private Long teamRequestId;

    /** 申请人用户ID */
    private Long applicantId;

    /** 申请理由 */
    private String reason;

    /** 状态: 0-待审核, 1-已通过, 2-已拒绝 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
