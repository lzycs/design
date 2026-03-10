package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 共享学习计划展示 VO：含小组名称、关联研讨室名称
 */
@Data
public class StudyPlanVO {
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 关键时间节点，格式：节点名|日期时间,节点名|日期时间 */
    private String keyTimeNodes;
    /** 所属小组名称 */
    private String teamTitle;
    /** 关联研讨室名称（来自 reservation + classroom） */
    private String classroomName;
    /** 研讨室预约日期（用于展示） */
    private String reservationDate;
    /** 研讨室预约开始时间（用于展示） */
    private String reservationStartTime;
    /** 研讨室预约结束时间（用于展示） */
    private String reservationEndTime;
}
