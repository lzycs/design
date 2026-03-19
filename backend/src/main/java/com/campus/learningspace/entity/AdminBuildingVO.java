package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBuildingVO {
    private Long id;
    private String name;
    private String buildingNumber;
    /**
     * 位置（沿用 building.address）
     */
    private String location;
    private Integer status;
    private LocalDateTime createTime;
}

