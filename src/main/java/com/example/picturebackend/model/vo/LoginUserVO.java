package com.example.picturebackend.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 VO
 */
@Data
@ApiModel(description = "用户 VO")
public class LoginUserVO implements Serializable {

    /**
     * 用户 ID
     */
    @ApiModelProperty(value = "用户 ID", example = "1")
    private Long id;

    /**
     * 账号
     */
    @ApiModelProperty(value = "账号", example = "user123")
    private String userAccount;

    /**
     * 用户昵称
     */
    @ApiModelProperty(value = "用户昵称", example = "张三")
    private String username;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "用户头像", example = "https://example.com/avatar.jpg")
    private String userAvatar;

    /**
     * 用户简介
     */
    @ApiModelProperty(value = "用户简介", example = "这是一个用户简介")
    private String userProfile;

    /**
     * 用户角色（角色：user，admin）
     */
    @ApiModelProperty(value = "用户角色", example = "user")
    private String userRole;

    /**
     * 编辑时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "编辑时间", example = "2024-06-01 12:00:00")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间", example = "2024-06-01 12:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间", example = "2024-06-01 12:00:00")
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
