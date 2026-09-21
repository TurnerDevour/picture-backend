package com.example.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.model.dto.spaceuser.SpaceUserAddDTO;
import com.example.picturebackend.model.dto.spaceuser.SpaceUserQueryDTO;
import com.example.picturebackend.model.entity.SpaceUser;
import com.example.picturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间用户
     *
     * @param spaceUserAddDTO 空间用户添加DTO
     *
     * @return 空间用户ID
     */
    long addSpaceUser(SpaceUserAddDTO spaceUserAddDTO);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryDTO 查询条件
     *
     * @return QueryWrapper<SpaceUser>
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO);

    /**
     * 获取空间用户视图对象
     *
     * @param spaceUser 空间用户实体
     * @param request   请求对象
     *
     * @return SpaceUserVO
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间用户视图对象列表
     *
     * @param spaceUserList 空间用户实体列表
     *
     * @return List<SpaceUserVO>
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 校验空间用户是否合法
     *
     * @param spaceUser 空间用户实体
     * @param add       是否为添加操作
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);
}

