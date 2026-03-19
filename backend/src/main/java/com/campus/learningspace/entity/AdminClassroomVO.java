package com.campus.learningspace.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端：教室信息列表展示对象（包含教学楼名称）
 */
@Data
public class AdminClassroomVO {
    private Long id;
    private Long buildingId;
    private String buildingName;

    private String name;
    private String roomNumber;
    private Integer floor;
    private Integer type;
    private Integer capacity;
    private String equipment;
    private Integer status;

    /**
     * 是否可用：status==1 则可用（1），否则不可用（0）
     */
    private Integer isAvailable;

    private BigDecimal latitude;
    private BigDecimal longitude;
}

