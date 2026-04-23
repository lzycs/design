package com.campus.learningspace.entity;

import lombok.Data;

@Data
public class AiPlanTaskVO {
    private String title;
    private Integer minutes;
    private String suggestedSlotLabel;
}
