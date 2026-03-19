package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("building")
public class Building {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * 楼栋号（用于后台基础数据维护）
     */
    private String buildingNumber;

    private String address;

    private Integer floorCount;

    private String description;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
