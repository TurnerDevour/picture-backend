package com.example.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.model.dto.space.SpaceAddDTO;
import com.example.picturebackend.model.dto.space.SpaceAnalyzeDTO;
import com.example.picturebackend.model.dto.space.SpaceQueryDTO;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

public interface SpaceService extends IService<Space> {

    /**
     * 添加空间
     *
     * @param spaceAddDTO 空间添加DTO
     * @param loginUser   登录用户信息
     *
     * @return 新增空间id
     */
    long addSpace(SpaceAddDTO spaceAddDTO, LoginUserVO loginUser);

    /**
     * 校验空间信息
     *
     * @param space 空间实体
     * @param add   是否为添加操作
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取查询条件
     *
     * @param spaceQueryDTO 查询条件
     *
     * @return QueryWrapper<Space>
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO);

    /**
     * 获取空间VO
     *
     * @param space   空间实体
     * @param request 请求对象
     *
     * @return SpaceVO
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间VO分页
     *
     * @param spacePage 空间分页
     * @param request   请求对象
     *
     * @return Page<SpaceVO>
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 自动填充限额数据
     *
     * @param space 空间实体
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 检查空间权限
     *
     * @param space     空间实体
     * @param loginUser 登录用户信息
     */
    void checkSpaceAuth(Space space, LoginUserVO loginUser);

    /**
     * 检查空间分析权限
     *
     * @param spaceAnalyzeDTO 空间分析DTO
     * @param loginUser       登录用户信息
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeDTO spaceAnalyzeDTO, LoginUserVO loginUser);
}

