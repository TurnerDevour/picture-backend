package com.example.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 用户表
 */
@ApiModel(description = "用户表")
@Data
@TableName(value = "`user`")
public class User implements Serializable {
    /**
     * 用户 ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "用户 ID")
    private Long id;

    /**
     * 账号
     */
    @TableField(value = "user_account")
    @ApiModelProperty(value = "账号")
    private String userAccount;

    /**
     * 密码
     */
    @TableField(value = "user_password")
    @ApiModelProperty(value = "密码")
    private String userPassword;

    /**
     * 用户昵称
     */
    @TableField(value = "username")
    @ApiModelProperty(value = "用户昵称")
    private String username;

    /**
     * 用户头像
     */
    @TableField(value = "user_avatar")
    @ApiModelProperty(value = "用户头像")
    private String userAvatar;

    /**
     * 用户简介
     */
    @TableField(value = "user_profile")
    @ApiModelProperty(value = "用户简介")
    private String userProfile;

    /**
     * 用户角色（角色：user，admin）
     */
    @TableField(value = "user_role")
    @ApiModelProperty(value = "用户角色（角色：user，admin）")
    private String userRole;

    /**
     * 编辑时间
     */
    @TableField(value = "edit_time")
    @ApiModelProperty(value = "编辑时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

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

    /**
     * 是否删除（0：未删除，1：已删除）
     */
    @TableLogic
    @TableField(value = "is_delete")
    @ApiModelProperty(value = "是否删除（0：未删除，1：已删除）")
    private Integer isDelete;

    /**
     * 账号是否激活
     */
    @TableField(value = "account_active")
    @ApiModelProperty(value = "账号是否激活")
    private String accountActive;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
