package com.example.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 图片
 */
@ApiModel(description = "图片")
@Data
@TableName(value = "picture")
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 图片 url
     */
    @TableField(value = "url")
    @ApiModelProperty(value = "图片 url")
    private String url;

    /**
     * 图片名称
     */
    @TableField(value = "`name`")
    @ApiModelProperty(value = "图片名称")
    private String name;

    /**
     * 简介
     */
    @TableField(value = "introduction")
    @ApiModelProperty(value = "简介")
    private String introduction;

    /**
     * 分类
     */
    @TableField(value = "category")
    @ApiModelProperty(value = "分类")
    private String category;

    /**
     * 标签（JSON 数组）
     */
    @TableField(value = "tags")
    @ApiModelProperty(value = "标签（JSON 数组）")
    private String tags;

    /**
     * 图片体积
     */
    @TableField(value = "pic_size")
    @ApiModelProperty(value = "图片体积")
    private Long picSize;

    /**
     * 图片宽度
     */
    @TableField(value = "pic_width")
    @ApiModelProperty(value = "图片宽度")
    private Integer picWidth;

    /**
     * 图片高度
     */
    @TableField(value = "pic_height")
    @ApiModelProperty(value = "图片高度")
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @TableField(value = "pic_scale")
    @ApiModelProperty(value = "图片宽高比例")
    private Double picScale;

    /**
     * 图片格式
     */
    @TableField(value = "pic_format")
    @ApiModelProperty(value = "图片格式")
    private String picFormat;

    /**
     * 缩略图 url
     */
    @TableField(value = "thumbnail_url")
    @ApiModelProperty(value = "缩略图 url")
    private String thumbnailUrl;

    /**
     * 创建用户 id
     */
    @TableField(value = "user_id")
    @ApiModelProperty(value = "创建用户 id")
    private Long userId;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    @TableField(value = "review_status")
    @ApiModelProperty(value = "状态：0-待审核; 1-通过; 2-拒绝")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @TableField(value = "review_message")
    @ApiModelProperty(value = "审核信息")
    private String reviewMessage;

    /**
     * 审核人 id
     */
    @TableField(value = "reviewer_id")
    @ApiModelProperty(value = "审核人 id")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @TableField(value = "review_time")
    @ApiModelProperty(value = "审核时间")
    private LocalDateTime reviewTime;

    /**
     * 空间 id（为空表示公共空间）
     */
    @TableField(value = "space_id")
    @ApiModelProperty(value = "空间 id（为空表示公共空间）")
    private Long spaceId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    @TableField(value = "edit_time")
    @ApiModelProperty(value = "编辑时间")
    private LocalDateTime editTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "is_delete")
    @TableLogic
    @ApiModelProperty(value = "是否删除")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
