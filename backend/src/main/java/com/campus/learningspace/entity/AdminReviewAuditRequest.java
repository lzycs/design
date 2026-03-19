package com.campus.learningspace.entity;

import lombok.Data;

/**
 * 管理端：评价审核请求
 */
@Data
public class AdminReviewAuditRequest {
    /**
     * true-通过(已展示), false-驳回(已屏蔽)
     */
    private Boolean approve;

    private String remark;
}

