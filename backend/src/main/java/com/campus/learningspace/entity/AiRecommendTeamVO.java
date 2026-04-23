package com.campus.learningspace.entity;

import lombok.Data;

@Data
public class AiRecommendTeamVO {
    private Long id;
    private String title;
    private Integer status;
    private Integer currentCount;
    private Integer expectedCount;
}

