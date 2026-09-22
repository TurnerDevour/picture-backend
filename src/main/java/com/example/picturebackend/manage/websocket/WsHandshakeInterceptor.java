package com.example.picturebackend.manage.websocket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.example.picturebackend.constant.SpaceUserPermissionConstant;
import com.example.picturebackend.manage.auth.SpaceUserAuthManager;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.SpaceTypeEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.service.PictureService;
import com.example.picturebackend.service.SpaceService;
import com.example.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

            //获取当前登录用户实体
            User user = userService.getCurrentLoginUserEntity(servletRequest);
            if (ObjUtil.isEmpty(user)) {
                log.error("WebSocket连接失败，用户未登录");
                return false;
            }
            // 脱敏后的用户信息，用于权限校验
            LoginUserVO loginUser = BeanUtil.copyProperties(user, LoginUserVO.class);

            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("WebSocket连接失败，缺少pictureId参数");
                return false;
            }
            // 检验用户是否有权限访问该图片
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("WebSocket连接失败，图片不存在");
                return false;
            }
            // 检查空间
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)) {
                    log.error("WebSocket连接失败，空间不存在");
                    return false;
                }
                // 检查用户是否有权限访问团队空间
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("WebSocket连接失败，空间类型不正确");
                    return false;
                }
            }
            // 获取权限列表·
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("WebSocket连接失败，用户没有权限编辑该图片");
                return false;
            }

            // 将用户信息和图片信息放入attributes中，供WebSocketHandler使用
            attributes.put("user", user);
            attributes.put("picture", picture);
            attributes.put("pictureId", Long.valueOf(pictureId));
        }

        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
