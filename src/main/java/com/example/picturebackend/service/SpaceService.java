package com.example.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.model.dto.space.*;
import com.example.picturebackend.model.dto.space.SpaceSizeAnalyzeDTO;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.vo.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 获取空间使用分析数据
     *
     * @param spaceAnalyzeDTO 空间分析DTO
     * @param loginUser       登录用户信息
     *
     * @return SpaceUsageAnalyzeVO
     */
    SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceAnalyzeDTO spaceAnalyzeDTO, LoginUserVO loginUser);

    /**
     * 获取空间分类分析数据
     *
     * @param spaceCategoryAnalyzeDTO 空间分类分析DTO
     * @param loginUser               登录用户信息
     *
     * @return List<SpaceCategoryAnalyzeVO>
     */
    List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, LoginUserVO loginUser);

    /**
     * 获取空间标签分析数据
     *
     * @param spaceTagAnalyzeDTO 空间标签分析DTO
     * @param loginUser          登录用户信息
     *
     * @return List<SpaceTagAnalyzeVO>
     */
    List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, LoginUserVO loginUser);

    /**
     * 获取空间大小分析数据
     *
     * @param spaceSizeAnalyzeDTO 空间大小分析DTO
     * @param loginUser           登录用户信息
     *
     * @return List<SpaceSizeAnalyzeVO>
     */
    List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, LoginUserVO loginUser);

    /**
     * 获取空间用户分析数据
     *
     * @param spaceUserAnalyzeDTO 空间用户分析DTO
     * @param loginUser           登录用户信息
     *
     * @return List<SpaceUserAnalyzeVO>
     */
    List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, LoginUserVO loginUser);

    /**
     * 获取空间排名分析数据
     *
     * @param spaceRankAnalyzeDTO 空间排名分析DTO
     * @param loginUser           登录用户信息
     *
     * @return List<SpaceRankAnalyzeVO>
     */
    List<SpaceRankAnalyzeVO> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, LoginUserVO loginUser);
}

