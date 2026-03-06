package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协作/组队列表与详情展示：含发起人姓名、更新时间等。
 */
@Data
public class TeamRequestVO {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String tags;
    private Integer expectedCount;
    private Integer currentCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 发起人姓名 */
    private String creatorName;
}
