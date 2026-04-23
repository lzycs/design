package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AiPlanDayVO {
    private LocalDate date;
    private List<AiPlanTaskVO> tasks;
}
