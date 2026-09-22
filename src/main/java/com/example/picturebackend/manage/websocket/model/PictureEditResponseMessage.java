package com.example.picturebackend.manage.websocket.model;

import com.example.picturebackend.model.dto.user.UserVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "图片编辑响应消息，用于服务端向客户端返回的消息")
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    @ApiModelProperty(value = "消息类型，例如 INFO, ERROR, ENTER_EDIT, EXIT_EDIT, EDIT_ACTION", required = true)
    private String type;

    /**
     * 信息
     */
    @ApiModelProperty(value = "信息", required = true)
    private String message;

    /**
     * 执行的编辑动作
     */
    @ApiModelProperty(value = "执行的编辑动作", required = true)
    private String editAction;

    /**
     * 用户信息
     */
    @ApiModelProperty(value = "用户信息", required = true)
    private UserVO user;
}
