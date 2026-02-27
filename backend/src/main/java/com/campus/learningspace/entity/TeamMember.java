package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("team_member")
public class TeamMember {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamRequestId;

    private Long userId;

    private Integer role;

    private LocalDateTime joinTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
