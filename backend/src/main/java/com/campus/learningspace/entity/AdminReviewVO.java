package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端：评价审核列表/详情展示对象（包含用户名/教室位置等）
 */
@Data
public class AdminReviewVO {
    private Long id;
    private Long userId;

    private String reviewerName;

    private Long classroomId;
    private String location;

    private Integer score;
    private String content;
    private String tags;

    /**
     * 0-待审核, 1-已展示, 2-已屏蔽
     */
    private Integer status;

    private LocalDateTime createTime;
}

