package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_message")
public class TeamMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamRequestId;

    private Long senderId;

    private String senderName;

    /**
     * 1-文本消息, 2-文件消息
     */
    private Integer type;

    private String content;

    private String fileName;

    private Long fileSize;

    /**
     * 客户端消息ID（用于重试幂等）
     */
    private String clientMsgId;

    /**
     * 1-正常, 2-撤回, 3-删除
     */
    private Integer status;

    /**
     * 0-正常, 1-已撤回
     */
    private Integer recalled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

