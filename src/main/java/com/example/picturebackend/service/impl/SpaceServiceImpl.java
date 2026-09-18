package com.example.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.model.dto.space.SpaceAddDTO;
import com.example.picturebackend.model.dto.space.SpaceAnalyzeDTO;
import com.example.picturebackend.model.dto.space.SpaceQueryDTO;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.SpaceLevelEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureVO;
import com.example.picturebackend.model.vo.SpaceVO;
import com.example.picturebackend.service.UserService;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.picturebackend.mapper.SpaceMapper;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.service.SpaceService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加空间
     *
     * @param spaceAddDTO 空间添加DTO
     * @param loginUser   登录用户信息
     *
     * @return 新增空间id
     */
    @Override
    public long addSpace(SpaceAddDTO spaceAddDTO, LoginUserVO loginUser) {
        // 1. 填充参数默认
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddDTO, space);
        // 默认值
        if (StrUtil.isBlank(spaceAddDTO.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddDTO.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 填充数据
        fillSpaceBySpaceLevel(space);

        // 2. 校验参数
        validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);

        // 3. 检验权限，非管理员只能创建普通级别的空间
        if (!userService.isAdmin(loginUser) && space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员只能创建普通级别的空间");
        }

        // 4. 控制同一用户只能创建一个私有空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "同一用户只能创建一个私有空间");

                // 5. 插入数据
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");
                return space.getId();
            });

            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    /**
     * 校验空间信息
     *
     * @param space 空间实体
     * @param add   是否为添加操作
     */
    @Override
    public void validSpace(Space space, boolean add) {
        // 1. 校验空间信息是否为空
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间信息为空");
        // 2. 取出space中的属性
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //3. 判断是创建还是更新, add 为true表示创建, false表示更新
        if (add) {
            // 4. 校验空间名称是否为空
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            // 5. 校验空间等级是否为空
            if (spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            }
        }
        //6.更新时，如果要更新空间级别，则需要校验空间级别是否为空
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
        }
        //7. 校验空间名称长度
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param spaceQueryDTO 查询条件
     *
     * @return QueryWrapper<Space>
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryDTO == null) {
            return queryWrapper;
        }

        // 取出查询条件中的值
        Long id = spaceQueryDTO.getId();
        Long userId = spaceQueryDTO.getUserId();
        String spaceName = spaceQueryDTO.getSpaceName();
        Integer spaceLevel = spaceQueryDTO.getSpaceLevel();
        String sortField = spaceQueryDTO.getSortField();
        String sortOrder = spaceQueryDTO.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotNull(spaceLevel), "space_level", spaceLevel);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "space_name", spaceName);

        // 处理排序条件
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取空间VO
     *
     * @param space   空间实体
     * @param request 请求对象
     *
     * @return SpaceVO
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.convertObjectToVO(space);

        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }

        return spaceVO;
    }

    /**
     * 获取空间VO分页
     *
     * @param spacePage 空间分页
     * @param request   请求对象
     *
     * @return Page<SpaceVO>
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::convertObjectToVO).collect(Collectors.toList());

        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        // 2. 填充用户信息到 SpaceVO
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 自动填充限额数据
     *
     * @param space 空间实体
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别自动填充限额
        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (levelEnum != null) {
            long maxSize = levelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = levelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 检查空间权限
     *
     * @param space     空间实体
     * @param loginUser 登录用户信息
     */
    @Override
    public void checkSpaceAuth(Space space, LoginUserVO loginUser) {
        // 1. 检查空间是否存在
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        // 2. 检查空间是否是当前用户或者管理员
        if (!loginUser.getId().equals(space.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 检查空间分析权限
     *
     * @param spaceAnalyzeDTO 空间分析DTO
     * @param loginUser       登录用户信息
     */
    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeDTO spaceAnalyzeDTO, LoginUserVO loginUser) {
        // 1. 检查权限
        if (spaceAnalyzeDTO.isQueryAll() || spaceAnalyzeDTO.isQueryPublic()) {
            // 全空间分析或者公共图库权限校验：仅管理员可访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "非管理员无权限访问全空间分析或公共图库");
        } else {
            // 私有空间图库校验
            Long spaceId = spaceAnalyzeDTO.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            Space space = spaceService.getById(spaceId);// 检查空间是否存在
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 检查空间是否属于当前用户
            checkSpaceAuth(space, loginUser);
        }
    }

    /**
     * 根据分析范围填充空间分析查询条件
     *
     * @param spaceAnalyzeDTO 空间分析DTO
     * @param queryWrapper    查询条件包装器
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeDTO spaceAnalyzeDTO, QueryWrapper<Picture> queryWrapper) {
        // 全空间分析：不需要额外条件
        if (spaceAnalyzeDTO.isQueryAll()) {
            return;
        }
        // 公共图库分析
        if (spaceAnalyzeDTO.isQueryPublic()) {
            queryWrapper.isNull("space_id");
            return;
        }
        // 私有空间分析
        Long spaceId = spaceAnalyzeDTO.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            queryWrapper.eq("space_id", spaceId);
            return;
        }
        // 如果没有指定分析范围，抛出异常
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询分析范围未指定");
    }
}
