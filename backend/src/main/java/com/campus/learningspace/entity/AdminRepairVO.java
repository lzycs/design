package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端：报修工单列表/详情展示对象（包含申请人/维修人员/地点信息）
 */
@Data
public class AdminRepairVO {
    private Long id;
    private Long userId;

    private String requesterName;
    private String requesterPhone;

    private String title;
    private String description;
    private Integer type;
    private Integer priority;
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime handleTime;
    private String handleRemark;

    private Long handlerId;
    private String handlerName;
    private String handlerPhone;

    private String location;
}

