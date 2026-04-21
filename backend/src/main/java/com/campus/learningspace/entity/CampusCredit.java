package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("campus_credit")
public class CampusCredit {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer creditScore;
    private String scoreComment;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
