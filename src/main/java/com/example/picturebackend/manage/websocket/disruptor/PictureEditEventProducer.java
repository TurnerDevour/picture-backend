package com.example.picturebackend.manage.websocket.disruptor;

import com.example.picturebackend.manage.websocket.model.PictureEditRequestMessage;
import com.example.picturebackend.model.entity.User;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 图片编辑事件生产者
 */
@Slf4j
@Component
public class PictureEditEventProducer {

    @Resource
    Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    /**
     * 发布图片编辑事件
     *
     * @param requestMessage 消息
     * @param session        当前用户的 session
     * @param user           当前用户
     * @param pictureId      图片 id
     */
    public void publishEvent(PictureEditRequestMessage requestMessage, WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        long next = ringBuffer.next();// 获取下一个可用的序号
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setPictureEditRequestMessage(requestMessage);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅关闭 disruptor
     */
    public void shutdown(){
        pictureEditEventDisruptor.shutdown();
    }
}
