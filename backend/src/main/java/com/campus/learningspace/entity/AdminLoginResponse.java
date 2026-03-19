package com.campus.learningspace.entity;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String token;
    private Long adminUserId;
    private String username;
    private String realName;
    /**
     * 1-最大权限管理员, 2-审核评论员, 3-维修人员
     */
    private Integer roleType;
}

