package com.example.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 创建用户 id
     */
    @TableField(value = "user_id")
    @ApiModelProperty(value = "创建用户 id")
    private Long userId;

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
    @ApiModelProperty(value = "是否删除")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
