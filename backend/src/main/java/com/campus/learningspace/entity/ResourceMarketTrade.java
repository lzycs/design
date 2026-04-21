package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource_market_trade")
public class ResourceMarketTrade {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;
    private Long publisherId;
    private Long receiverId;
    /** 1-待面交确认 2-已完成 3-已取消 */
    private Integer status;
    private String note;
    private String meetingPlace;
    private LocalDateTime meetingTime;
    private Integer confirmPublisher;
    private Integer confirmReceiver;
    private LocalDateTime confirmTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String itemTitle;

    @TableField(exist = false)
    private String publisherName;
}
