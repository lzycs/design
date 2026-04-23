package com.campus.learningspace.entity;

import lombok.Data;

@Data
public class AiRecommendResourceVO {
    private Long id;
    private String title;
    private String category;
    private Integer isFree;
    private Double price;
    private String publisherName;
}

