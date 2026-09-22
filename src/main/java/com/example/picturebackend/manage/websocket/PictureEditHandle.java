package com.example.picturebackend.manage.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.example.picturebackend.manage.websocket.disruptor.PictureEditEventProducer;
import com.example.picturebackend.manage.websocket.model.PictureEditRequestMessage;
import com.example.picturebackend.manage.websocket.model.PictureEditResponseMessage;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.PictureEditActionEnum;
import com.example.picturebackend.model.enums.PictureEditMessageTypeEnum;
import com.example.picturebackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PictureEditHandle extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    @Resource
    private ObjectMapper objectMapper;

    // 保存每个图片的编辑状态：key为pictureId, value为当前正在编辑该图片的用户id
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存每个图片的 WebSocketSession 集合：key为pictureId, value为当前正在编辑该图片的 WebSocketSession 集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 当 WebSocket 连接建立时触发
     *
     * @param session 当前的 WebSocketSession
     *
     * @throws Exception 抛出异常
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // 保存session到对应的集合中
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 构建消息响应对象
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 已加入编辑", user.getUsername());
        responseMessage.setMessage(message);
        // 广播消息给指定图片的所有 WebSocketSession
        broadcastToPicture(pictureId, responseMessage);
    }

    /**
     * 处理接收到的文本消息
     *
     * @param session 当前的 WebSocketSession
     * @param message 关闭消息
     *
     * @throws Exception 抛出异常
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // 处理接收到来自客户端的消息，将消息解析为 PictureEditRequestMessage 对象
        PictureEditRequestMessage requestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 将消息发布到 Disruptor 的事件队列中，由 PictureEditEventWorkHandler 处理
        pictureEditEventProducer.publishEvent(requestMessage, session, user, pictureId);
    }


    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);

        // 从集合中移除当前的 WebSocketSession
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        // 构建消息响应对象
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 已离开编辑", user.getUsername());
        responseMessage.setMessage(message);
        responseMessage.setUser(userService.getUserVO(user));
        // 广播消息给指定图片的所有 WebSocketSession
        broadcastToPicture(pictureId, responseMessage);
    }

    // 处理用户开始编辑图片的消息
    public void handleEnterEditMessage(PictureEditRequestMessage requestMessage, @NonNull WebSocketSession session, User user, Long pictureId) throws Exception {
        // 检查是否有其他用户正在编辑该图片
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 将当前用户标记为正在编辑该图片
            pictureEditingUsers.put(pictureId, user.getId());
            // 构建消息响应对象
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 已开始编辑图片", user.getUsername());
            responseMessage.setMessage(message);
            responseMessage.setUser(userService.getUserVO(user));
            // 广播消息给指定图片的所有 WebSocketSession
            broadcastToPicture(pictureId, responseMessage);
        }
    }

    // 处理用户进行编辑操作的消息
    public void handleEditActionMessage(PictureEditRequestMessage requestMessage, @NonNull WebSocketSession session, User user, Long pictureId) throws Exception {
        // 获取当前正在编辑该图片的用户ID
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = requestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 检查当前用户是否是正在编辑该图片的用户
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 构建消息响应对象
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户 %s 执行了编辑操作: %s", user.getUsername(), actionEnum.getText());
            responseMessage.setMessage(message);
            responseMessage.setEditAction(editAction);
            responseMessage.setUser(userService.getUserVO(user));
            // 广播给指定图片的所有 WebSocketSession，排除当前用户的 session
            broadcastToPicture(pictureId, responseMessage, session);
        }
    }

    // 处理用户退出编辑图片的消息
    public void handleExitEditMessage(PictureEditRequestMessage requestMessage, @NonNull WebSocketSession session, User user, Long pictureId) throws Exception {
        // 检查当前用户是否是正在编辑该图片的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 构建消息响应对象
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 已退出编辑图片", user.getUsername());
            responseMessage.setMessage(message);
            responseMessage.setUser(userService.getUserVO(user));
            // 广播消息给指定图片的所有 WebSocketSession
            broadcastToPicture(pictureId, responseMessage);
        }
    }

    /**
     * 广播消息给指定图片的所有 WebSocketSession，排除指定的 WebSocketSession
     *
     * @param pictureId      图片ID
     * @param message        消息对象
     * @param excludeSession 要排除的 WebSocketSession
     *
     * @throws Exception 抛出异常
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage message, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 使用 Spring 管理的 ObjectMapper，已配置 JavaTimeModule 和 Long 转 String
            String jsonStrMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonStrMessage);
            for (WebSocketSession session : sessionSet) {
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue; // 排除指定的 WebSocketSession
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }

    }

    /**
     * 广播消息给指定图片的所有 WebSocketSession
     *
     * @param pictureId 图片ID
     * @param message   消息对象
     *
     * @throws Exception 抛出异常
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage message) throws Exception {
        broadcastToPicture(pictureId, message, null);
    }

}
