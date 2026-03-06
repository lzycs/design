package com.campus.learningspace.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报修列表/详情展示对象：在 Repair 基础上补充地点、维修人员等展示字段。
 */
@Data
public class RepairVO {
    private Long id;
    private Long userId;
    private Long classroomId;
    private String title;
    private String description;
    private Integer type;
    private Integer priority;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
    private String handleRemark;
    private Long handlerId;

    /** 地点展示：教学楼名称-房间号，如 第一教学楼-101 */
    private String location;
    /** 维修人员姓名 */
    private String handlerName;
    /** 维修人员电话 */
    private String handlerPhone;
}
