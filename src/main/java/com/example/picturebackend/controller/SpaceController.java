package com.example.picturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.DeleteRequest;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.model.dto.picture.*;
import com.example.picturebackend.model.dto.space.SpaceEditDTO;
import com.example.picturebackend.model.dto.space.SpaceQueryDTO;
import com.example.picturebackend.model.dto.space.SpaceUpdateDTO;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.SpaceVO;
import com.example.picturebackend.service.SpaceService;
import com.example.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;


    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        Long id = deleteRequest.getId();

        // 判断是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 此方法只有管理员可用
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDTO spaceUpdateDTO, HttpServletRequest request) {
        if (spaceUpdateDTO == null || spaceUpdateDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Space space = new Space();
        BeanUtil.copyProperties(spaceUpdateDTO, space);

        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);

        // 参数校验
        spaceService.validSpace(space, false);
        // 判断数据是否存在
        Long id = spaceUpdateDTO.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 此方法只有管理员可用
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Space result = spaceService.getById(id);
        ThrowUtils.throwIf(result == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
    }

    /**
     * 此方法只有管理员可用, 分页获取空间列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryDTO spaceQueryDTO) {
        long current = spaceQueryDTO.getCurrent();
        long size = spaceQueryDTO.getPageSize();
        Page<Space> picturePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryDTO));
        return ResultUtils.success(picturePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryDTO spaceQueryDTO, HttpServletRequest request) {
        long current = spaceQueryDTO.getCurrent();
        long size = spaceQueryDTO.getPageSize();

        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页数量不能超过20");

        Page<Space> picturePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryDTO));
        return ResultUtils.success(spaceService.getSpaceVOPage(picturePage, request));
    }

    /**
     * 此方法只有普通用户可用
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDTO spaceEditDTO, HttpServletRequest request) {
        if (spaceEditDTO == null || spaceEditDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        Long id = spaceEditDTO.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Space space = new Space();
        BeanUtil.copyProperties(spaceEditDTO, space);

        // 数据自动填充
        spaceService.fillSpaceBySpaceLevel(space);

        // 设置编辑时间
        space.setEditTime(LocalDateTime.now());
        spaceService.validSpace(space, false);

        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
