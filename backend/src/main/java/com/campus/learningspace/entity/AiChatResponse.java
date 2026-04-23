package com.campus.learningspace.entity;

import lombok.Data;

import java.util.List;

@Data
public class AiChatResponse {
    private String text;
    private String provider;
    private List<AiRecommendResourceVO> resources;
    private List<AiRecommendTeamVO> teams;
}
