package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AiPlanGenerateResponse {
    private LocalDate startDate;
    private String goalType;
    private String goalText;
    private Integer dailyMinutes;
    private List<AiPlanDayVO> days;
    /** 若通过“生成并保存”接口调用，则返回保存的 StudyPlan IDs */
    private List<Long> savedStudyPlanIds;
}
