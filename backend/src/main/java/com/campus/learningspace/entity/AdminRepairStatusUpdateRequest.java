package com.campus.learningspace.entity;

import lombok.Data;

/**
 * 管理端：报修状态更新请求
 */
@Data
public class AdminRepairStatusUpdateRequest {
    /**
     * 1-待处理, 2-处理中, 3-已修复, 4-已关闭(驳回)
     */
    private Integer status;

    private String handleRemark;
}

