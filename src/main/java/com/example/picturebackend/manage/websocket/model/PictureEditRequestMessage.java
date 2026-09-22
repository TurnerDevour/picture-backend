package com.example.picturebackend.manage.websocket.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "图片编辑请求消息，用于客户端与服务端之间的实时通信的消息传递参数")
public class PictureEditRequestMessage {

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    @ApiModelProperty(value = "消息类型，例如 'ENTER_EDIT', 'EXIT_EDIT', 'EDIT_ACTION'", required = true)
    private String type;

    /**
     * 执行的编辑动作
     */
    @ApiModelProperty(value = "执行的编辑动作", required = true)
    private String editAction;
}
