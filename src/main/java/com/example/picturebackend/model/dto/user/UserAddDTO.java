package com.example.picturebackend.model.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@ApiModel(description = "用户添加")
public class UserAddDTO implements Serializable {

    /**
     * 用户昵称
     */
    @ApiModelProperty(value = "用户昵称", required = true)
    @NotNull(message = "用户昵称不能为空")
    @Size(min = 2, max = 20, message = "用户昵称长度必须在2到20个字符之间")
    private String username;

    /**
     * 账号
     */
    @ApiModelProperty(value = "用户账号", required = true)
    @NotNull(message = "用户账号不能为空")
    @Size(min = 4, max = 20, message = "用户账号长度必须在4到20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户账号只能包含字母、数字和下划线")
    private String userAccount;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "用户头像")
    @Size(max = 255, message = "用户头像长度不能超过255个字符")
    private String userAvatar;

    /**
     * 用户简介
     */
    @ApiModelProperty(value = "用户简介")
    @Size(max = 500, message = "用户简介长度不能超过500个字符")
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    @ApiModelProperty(value = "用户角色")
    private String userRole;

    private static final long serialVersionUID = 1L;
}
