package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("classroom")
public class Classroom {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long buildingId;

    private String name;

    private String roomNumber;

    private Integer floor;

    private Integer type;

    private Integer capacity;

    private String equipment;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer checkinRadius;

    private Integer status;

    private Integer realTimeStatus;

    private BigDecimal environmentScore;

    private Integer totalReviews;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
