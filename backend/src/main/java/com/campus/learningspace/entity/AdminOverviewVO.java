package com.campus.learningspace.entity;

import lombok.Data;

/**
 * 管理端：首页概览统计
 */
@Data
public class AdminOverviewVO {
    private Integer pendingRepairCount;
    private Integer pendingReviewCount;
    private Integer classroomTotal;
}

