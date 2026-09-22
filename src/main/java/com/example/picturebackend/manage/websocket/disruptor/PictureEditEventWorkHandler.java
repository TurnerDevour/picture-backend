package com.example.picturebackend.manage.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.example.picturebackend.manage.websocket.PictureEditHandle;
import com.example.picturebackend.manage.websocket.model.PictureEditRequestMessage;
import com.example.picturebackend.manage.websocket.model.PictureEditResponseMessage;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.PictureEditMessageTypeEnum;
import com.example.picturebackend.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 图片编辑事件处理器（消费者）
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandle pictureEditHandle;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage requestMessage = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();
        String messageType = requestMessage.getType(); // 获取消息类型
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.valueOf(messageType);

        switch (messageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandle.handleEnterEditMessage(requestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                pictureEditHandle.handleEditActionMessage(requestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                pictureEditHandle.handleExitEditMessage(requestMessage, session, user, pictureId);
                break;
            default:
                PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
                responseMessage.setType(PictureEditMessageTypeEnum.ERROR.name());
                responseMessage.setMessage("未知的消息类型");
                responseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(responseMessage)));
                log.warn("未知的消息类型: {}", messageType);
        }
    }
}
