package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AiPlanGenerateRequest {
    private Long userId;
    private String goalType; // exam | cert | review
    private String goalText;
    private Integer dailyMinutes;
    private LocalDate startDate;
    private Integer days; // 1-14
}
