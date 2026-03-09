package com.campus.learningspace.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 教室详情页展示：教室基础信息 + 热度星级 + 综合评分（历史评分均值）
 */
@Data
public class ClassroomDetailVO {
    private Classroom classroom;
    /** 热度星级 1-5，按签到次数排名：前20%为5星，20%-40%为4星，依次降序 */
    private Integer popularityStars;
    /** 该教室历史评分的平均值（综合评分） */
    private BigDecimal averageScore;
    /** 该教室历史评价条数 */
    private Integer totalReviews;
    /** 该教室签到次数（已签到/已完成的预约数） */
    private Long checkinCount;
}
