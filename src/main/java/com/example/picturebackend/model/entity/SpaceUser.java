package com.example.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 空间用户关联
 */
@ApiModel(description = "空间用户关联")
@Data
@TableName(value = "space_user")
public class SpaceUser {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 空间 id
     */
    @TableField(value = "space_id")
    @ApiModelProperty(value = "空间 id")
    private Long spaceId;

    /**
     * 用户 id
     */
    @TableField(value = "user_id")
    @ApiModelProperty(value = "用户 id")
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    @TableField(value = "space_role")
    @ApiModelProperty(value = "空间角色：viewer/editor/admin")
    private String spaceRole;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
