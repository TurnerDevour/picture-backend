package com.example.picturebackend.model.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
@ApiModel(description = "用户注册")
public class UserRegisterDTO implements Serializable {

    /**
     * 用户账号
     */
    @ApiModelProperty(value = "用户账号", required = true)
    @NotNull(message = "用户账号不能为空")
    @Size(min = 4, max = 20, message = "用户账号长度必须在4到20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户账号只能包含字母、数字和下划线")
    private String userAccount;

    /**
     * 用户密码
     */
    @ApiModelProperty(value = "用户密码", required = true)
    @NotNull(message = "用户密码不能为空")
    @Size(min = 6, max = 20, message = "用户密码长度必须在6到20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户密码只能包含字母、数字和下划线")
    private String userPassword;

    /**
     * 校验密码
     */
    @ApiModelProperty(value = "校验密码", required = true)
    @NotNull(message = "校验密码不能为空")
    @Size(min = 6, max = 20, message = "校验密码长度必须在6到20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "校验密码只能包含字母、数字和下划线")
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}
