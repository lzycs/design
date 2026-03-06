package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协作成员展示：含用户姓名。
 */
@Data
public class TeamMemberVO {
    private Long id;
    private Long teamRequestId;
    private Long userId;
    private Integer role;
    private LocalDateTime joinTime;

    /** 成员姓名（来自 user.real_name） */
    private String memberName;
}
