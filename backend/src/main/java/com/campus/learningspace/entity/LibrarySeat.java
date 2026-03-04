package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("library_seat")
public class LibrarySeat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long libraryId;

    private String seatLabel;

    private Integer floor;

    private Integer rowNum;

    private Integer colNum;

    /**
     * 座位类型: 1-普通座位, 2-带插座, 3-静音区座位, 4-小组研讨座
     */
    private Integer seatType;

    /**
     * 状态: 0-停用, 1-可预约, 2-维修中
     */
    private Integer status;

    /**
     * 实时状态: 0-空闲, 1-使用中, 2-已预约
     */
    private Integer realTimeStatus;

    private String equipment;

    private BigDecimal environmentScore;

    private Integer totalReviews;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

