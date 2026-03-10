package com.campus.learningspace.dto;

import lombok.Data;

/**
 * 创建学习计划（可选关联研讨室预约）
 */
@Data
public class CreateStudyPlanDTO {
    private Long teamRequestId;
    private Long userId;
    private String title;
    private String description;
    private String planDate;      // yyyy-MM-dd
    private String startTime;    // HH:mm 或 HH:mm:ss
    private String endTime;

    /** 关键时间节点，格式：节点名|日期时间,节点名|日期时间 */
    private String keyTimeNodes;

    /** 可选：关联研讨室时填写 */
    private Long classroomId;
    private String reservationDate;  // yyyy-MM-dd
    private String resStartTime;     // HH:mm:ss
    private String resEndTime;       // HH:mm:ss
}
