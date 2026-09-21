package com.example.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.model.dto.space.SpaceQueryDTO;
import com.example.picturebackend.model.dto.spaceuser.SpaceUserAddDTO;
import com.example.picturebackend.model.dto.spaceuser.SpaceUserQueryDTO;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.SpaceRoleEnum;
import com.example.picturebackend.model.vo.SpaceUserVO;
import com.example.picturebackend.model.vo.SpaceVO;
import com.example.picturebackend.service.SpaceService;
import com.example.picturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.picturebackend.mapper.SpaceUserMapper;
import com.example.picturebackend.model.entity.SpaceUser;
import com.example.picturebackend.service.SpaceUserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加空间用户
     *
     * @param spaceUserAddDTO 空间用户添加DTO
     *
     * @return 空间用户ID
     */
    @Override
    public long addSpaceUser(SpaceUserAddDTO spaceUserAddDTO) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceUserAddDTO == null, ErrorCode.PARAMS_ERROR);
        // 2. 添加空间用户
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddDTO, spaceUser);
        // 3. 校验空间成员是添加还是更新
        validSpaceUser(spaceUser, true);
        // 4. 插入数据库
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryDTO 查询条件
     *
     * @return QueryWrapper<SpaceUser>
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryDTO == null) {
            return queryWrapper;
        }

        // 取出查询条件中的值
        Long id = spaceUserQueryDTO.getId();
        Long userId = spaceUserQueryDTO.getUserId();
        Long spaceId = spaceUserQueryDTO.getSpaceId();
        String spaceRole = spaceUserQueryDTO.getSpaceRole();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "space_id", spaceId);
        queryWrapper.eq(StrUtil.isNotEmpty(spaceRole), "space_role", spaceRole);

        return queryWrapper;
    }

    /**
     * 获取空间用户VO
     *
     * @param spaceUser 空间用户实体
     * @param request   请求对象
     *
     * @return SpaceUserVO
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        SpaceUserVO spaceUserVO = SpaceUserVO.convertObjectToVO(spaceUser);

        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }

        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }

        return spaceUserVO;
    }

    /**
     * 获取空间用户VO列表
     *
     * @param spaceUserList 空间用户实体列表
     *
     * @return List<SpaceUserVO>
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 1. 校验参数
        if (ObjectUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 2. 遍历空间用户列表，获取VO对象
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::convertObjectToVO).collect(Collectors.toList());

        // 3. 返回空间用户VO列表,查询关联的用户和空间信息
        Set<Long> serIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 4. 批量查询用户信息和空间信息
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(serIdSet).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream().collect(Collectors.groupingBy(Space::getId));
        // 5.填充SpaceUserVO中的用户信息和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            if (userId != null && userId > 0) {
                List<User> userList = userIdUserListMap.get(userId);
                if (userList != null && !userList.isEmpty()) {
                    UserVO userVO = userService.getUserVO(userList.get(0));
                    spaceUserVO.setUser(userVO);
                }
            }
            Long spaceId = spaceUserVO.getSpaceId();
            if (spaceId != null && spaceId > 0) {
                List<Space> spaceList = spaceIdSpaceListMap.get(spaceId);
                if (spaceList != null && !spaceList.isEmpty()) {
                    SpaceVO spaceVO = spaceService.getSpaceVO(spaceList.get(0), null);
                    spaceUserVO.setSpace(spaceVO);
                }
            }
        });

        return spaceUserVOList;
    }

    /**
     * 校验空间成员对象
     *
     * @param spaceUser 空间成员
     * @param add       是否为添加还是更新操作
     */
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 2. 校验空间ID和用户ID
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        // 3. 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum enumByValue = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }
}

