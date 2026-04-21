package com.campus.learningspace.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("resource_market_item")
public class ResourceMarketItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String description;
    private String category;
    /** 1-实物资料 2-文档资料 3-技能服务 */
    private Integer resourceType;
    private BigDecimal price;
    /** 0-非免费 1-免费 */
    private Integer isFree;
    /** 1-原创 2-转载 */
    private Integer originType;
    private String sourceReference;
    private Long courseId;
    private Long teamRequestId;
    private String tags;
    private String images;
    private String recommendedPlace;
    /** 0-待审核 1-已上架 2-驳回 3-已下架 4-流转中 5-已完成 */
    private Integer status;
    private Long auditBy;
    private String auditRemark;
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String publisherName;
}
