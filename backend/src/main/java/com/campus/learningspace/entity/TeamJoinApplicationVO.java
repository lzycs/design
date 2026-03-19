package com.campus.learningspace.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 加入小组申请展示 VO（含申请人姓名、小组标题）
 */
@Data
public class TeamJoinApplicationVO {
    private Long id;
    private Long teamRequestId;
    private Long applicantId;
    private String reason;
    private Integer status;
    private LocalDateTime createTime;

    /** 申请人姓名 */
    private String applicantName;

    /** 所申请的小组标题 */
    private String teamTitle;
}
