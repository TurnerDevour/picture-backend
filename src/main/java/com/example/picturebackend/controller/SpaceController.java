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
import com.example.picturebackend.manage.auth.SpaceUserAuthManager;
import com.example.picturebackend.model.dto.picture.*;
import com.example.picturebackend.model.dto.space.*;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.enums.SpaceLevelEnum;
import com.example.picturebackend.model.vo.*;
import com.example.picturebackend.service.SpaceService;
import com.example.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddDTO spaceAddDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        long newId = spaceService.addSpace(spaceAddDTO, loginUser);
        return ResultUtils.success(newId);
    }


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
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDTO spaceUpdateDTO) {
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
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
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

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevelVO>> listSpaceLevel() {
        // 获取所有空间枚举类
        List<SpaceLevelVO> spaceLevelVOList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevelVO(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxSize(),
                        spaceLevelEnum.getMaxCount()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelVOList);
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

    /**
     * 空间分析--获取空间使用情况
     */
    @PostMapping("/analyze/usage")
    public BaseResponse<SpaceUsageAnalyzeVO> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);

        SpaceUsageAnalyzeVO spaceUsageAnalyzeVO = spaceService.getSpaceUsageAnalyze(spaceUsageAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceUsageAnalyzeVO);
    }

    /**
     * 空间分析--获取空间分类分析数据
     */
    @PostMapping("/analyze/category")
    public BaseResponse<List<SpaceCategoryAnalyzeVO>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);

        List<SpaceCategoryAnalyzeVO> spaceCategoryAnalyzeVOList = spaceService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeVOList);
    }

    /**
     * 空间分析--获取空间标签分析数据
     */
    @PostMapping("/analyze/tag")
    public BaseResponse<List<SpaceTagAnalyzeVO>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        List<SpaceTagAnalyzeVO> spaceTagAnalyzeVOList = spaceService.getSpaceTagAnalyze(spaceTagAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceTagAnalyzeVOList);
    }

    /**
     * 空间分析--获取空间图片大小分析数据
     */
    @PostMapping("/analyze/size")
    public BaseResponse<List<SpaceSizeAnalyzeVO>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        List<SpaceSizeAnalyzeVO> spaceSizeAnalyzeVOList = spaceService.getSpaceSizeAnalyze(spaceSizeAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeVOList);
    }

    /**
     * 空间分析--获取空间用户分析数据
     */
    @PostMapping("/analyze/user")
    public BaseResponse<List<SpaceUserAnalyzeVO>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        List<SpaceUserAnalyzeVO> spaceUserAnalyzeVOList = spaceService.getSpaceUserAnalyze(spaceUserAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceUserAnalyzeVOList);
    }

    /**
     * 空间分析--获取空间排名分析数据
     */
    @PostMapping("/analyze/rank")
    public BaseResponse<List<SpaceRankAnalyzeVO>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        List<SpaceRankAnalyzeVO> spaceRankAnalyzeVOList = spaceService.getSpaceRankAnalyze(spaceRankAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceRankAnalyzeVOList);
    }


}
