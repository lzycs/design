package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AiDeviationCheckResponse {
    private Long userId;
    private LocalDate date;
    /** 逾期未完成学习计划（study_plan.status=1 且 plan_date<=date） */
    private List<Long> overdueStudyPlanIds;
    /** 今日预约未签到（reservation.status=1 且 start_time 已过去） */
    private List<Long> missedCheckinReservationIds;
}

