package com.example.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.mapper.SpaceUserMapper;
import com.example.picturebackend.model.dto.space.*;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.SpaceUser;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.SpaceLevelEnum;
import com.example.picturebackend.model.enums.SpaceRoleEnum;
import com.example.picturebackend.model.enums.SpaceTypeEnum;
import com.example.picturebackend.model.vo.*;
import com.example.picturebackend.mapper.PictureMapper;
import com.example.picturebackend.service.UserService;
import org.springframework.stereotype.Service;
import com.example.picturebackend.mapper.SpaceMapper;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.service.SpaceService;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserMapper spaceUserMapper;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private TransactionTemplate transactionTemplate;

    //@Resource
    //private DynamicShardingManager dynamicShardingManager;

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
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue()); // 默认普通级别
        }
        if (spaceAddDTO.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue()); // 默认私有空间
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

        // 4. 控制同一用户只能创建一个私有空间,以及创建一个团队空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "同一用户只能创建一个私有空间");

                // 5. 插入数据
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间创建失败");

                // 6. 如果是团队空间，关联新增空间成员信息
                if (space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserMapper.insert(spaceUser) > 0;
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "空间成员创建失败");
                }
                // 7. 动态创建分表
                //dynamicShardingManager.createSpacePictureTable(space);

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
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        //3. 判断是创建还是更新, add 为true表示创建, false表示更新
        if (add) {
            // 4. 校验空间名称是否为空
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            // 5. 校验空间等级是否为空
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        // 6. 校验空间名称长度
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 7. 更新时，如果要更新空间级别，则需要校验空间级别是否为空
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
        }
        // 8. 校验空间类型是否为空
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
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
        Integer spaceType = spaceQueryDTO.getSpaceType();
        String sortField = spaceQueryDTO.getSortField();
        String sortOrder = spaceQueryDTO.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotNull(spaceLevel), "space_level", spaceLevel);
        queryWrapper.eq(ObjectUtil.isNotNull(spaceType), "space_type", spaceType);
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
            Space space = this.getById(spaceId);// 检查空间是否存在
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 检查空间是否属于当前用户
            checkSpaceAuth(space, loginUser);
        }
    }

    /**
     * 获取空间使用分析数据
     *
     * @param spaceAnalyzeDTO 空间分析DTO
     * @param loginUser       登录用户信息
     *
     * @return SpaceUsageAnalyzeVO
     */
    @Override
    public SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceAnalyzeDTO spaceAnalyzeDTO, LoginUserVO loginUser) {
        ThrowUtils.throwIf(spaceAnalyzeDTO == null, ErrorCode.PARAMS_ERROR, "空间分析参数为空");
        // 1. 检查是否是全空间分析还是公共图库分析，非管理员无权限访问
        if (spaceAnalyzeDTO.isQueryAll() || spaceAnalyzeDTO.isQueryPublic()) {
            // 2. 检查是否是管理员
            boolean isAdmin = userService.isAdmin(loginUser);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR, "非管理员无权限访问全空间分析或公共图库");
            // 3. 统计公共图库的使用情况
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("pic_size");
            if (!spaceAnalyzeDTO.isQueryAll()) {
                queryWrapper.isNull("space_id");
            }
            List<Object> pictureObjList = pictureMapper.selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            long usedCount = pictureObjList.size();
            SpaceUsageAnalyzeVO spaceUsageAnalyzeVO = new SpaceUsageAnalyzeVO();
            spaceUsageAnalyzeVO.setUsedSize(usedSize);
            spaceUsageAnalyzeVO.setUsedCount(usedCount);
            // 4. 公共图库分析时，无上限，无比例
            spaceUsageAnalyzeVO.setMaxSize(null);
            spaceUsageAnalyzeVO.setMaxCount(null);
            spaceUsageAnalyzeVO.setSizeUsageRatio(null);
            spaceUsageAnalyzeVO.setCountUsageRatio(null);
            return spaceUsageAnalyzeVO;
        } else {
            // 5. 查询指定空间的使用情况
            Long spaceId = spaceAnalyzeDTO.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            Space space = this.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 6. 检查空间权限
            checkSpaceAuth(space, loginUser);
            // 7. 统计空间的使用情况
            SpaceUsageAnalyzeVO spaceUsageAnalyzeVO = new SpaceUsageAnalyzeVO();
            spaceUsageAnalyzeVO.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeVO.setUsedSize(space.getTotalSize());
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeVO.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeVO.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeVO.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeVO.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeVO;
        }
    }

    /**
     * 获取空间分类分析数据
     *
     * @param spaceCategoryAnalyzeDTO 空间分类分析DTO
     * @param loginUser               登录用户信息
     *
     * @return List<SpaceCategoryAnalyzeVO>
     */
    @Override
    public List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, LoginUserVO loginUser) {
        // 1. 检查参数是否为空
        ThrowUtils.throwIf(spaceCategoryAnalyzeDTO == null, ErrorCode.PARAMS_ERROR, "空间分类分析参数为空");
        // 2. 检查空间权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDTO, loginUser);
        // 3，构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeDTO, queryWrapper);
        // 4. 分组查询图片分类统计数据
        queryWrapper.select("category AS category", "COUNT(*) AS count", "SUM(pic_size) AS totalSize").groupBy("category");
        // 5. 执行查询并转换结果
        return pictureMapper.selectMaps(queryWrapper).stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeVO(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * 获取空间标签分析数据
     *
     * @param spaceTagAnalyzeDTO 空间标签分析DTO
     * @param loginUser          登录用户信息
     *
     * @return List<SpaceTagAnalyzeVO>
     */
    @Override
    public List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, LoginUserVO loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceTagAnalyzeDTO == null, ErrorCode.PARAMS_ERROR, "空间标签分析参数为空");
        // 2. 检查空间分析权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeDTO, loginUser);
        // 3. 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeDTO, queryWrapper);
        // 4. 查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureMapper.selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        // 5. 合并所有标签并统计标签出现次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 6. 将统计结果转换为 SpaceTagAnalyzeVO 列表并按照出现次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeVO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间大小分析数据
     *
     * @param spaceSizeAnalyzeDTO 空间大小分析DTO
     * @param loginUser           登录用户信息
     *
     * @return List<SpaceSizeAnalyzeVO>
     */
    @Override
    public List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, LoginUserVO loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceSizeAnalyzeDTO == null, ErrorCode.PARAMS_ERROR, "空间大小分析参数为空");
        // 2. 检查空间分析权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeDTO, loginUser);
        // 3. 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeDTO, queryWrapper);
        // 4. 查询所有符合条件的图片大小
        queryWrapper.select("pic_size");
        List<Long> picSizeList = pictureMapper.selectObjs(queryWrapper)
                .stream()
                .map(result -> ((Number) result).longValue())
                .collect(Collectors.toList());
        // 5. 定义大小分段范围
        Map<String, Object> sizeRangList = new LinkedHashMap<>();
        sizeRangList.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRangList.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRangList.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRangList.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        // 6. 将统计结果转换为 SpaceSizeAnalyzeVO 列表
        return sizeRangList.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeVO(entry.getKey(), (Long) entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间用户分析数据
     *
     * @param spaceUserAnalyzeDTO 空间用户分析DTO
     * @param loginUser           登录用户信息
     *
     * @return List<SpaceUserAnalyzeVO>
     */
    @Override
    public List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, LoginUserVO loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceUserAnalyzeDTO == null, ErrorCode.PARAMS_ERROR, "空间用户分析参数为空");
        // 2. 检查空间分析权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeDTO, loginUser);
        // 3. 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeDTO.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "user_id", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeDTO, queryWrapper);
        // 4. 分析维度：按每日/每周/每月统计图片上传数量
        String timeDimension = spaceUserAnalyzeDTO.getTimeDimension();
        switch (timeDimension) {
            case "day":
                // 按每日统计
                queryWrapper.select("DATE_FORMAT(create_time, '%Y-%m-%d') as period", "COUNT(*) as count");
                break;
            case "week":
                // 按每周统计
                queryWrapper.select("YEARWEEK(create_time, 1) as period", "COUNT(*) as count");
                break;
            case "month":
                // 按每月统计
                queryWrapper.select("DATE_FORMAT(create_time, '%Y-%m') as period", "COUNT(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度不支持");
        }
        // 5. 分组查询和排序
        queryWrapper.groupBy("period").orderByAsc("period");
        // 6. 执行查询并转换结果
        return pictureMapper.selectMaps(queryWrapper).stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeVO(period, count);
                }).collect(Collectors.toList());
    }

    /**
     * 获取空间排名分析数据
     *
     * @param spaceRankAnalyzeDTO 空间排名分析DTO
     * @param loginUser           登录用户信息
     *
     * @return List<SpaceRankAnalyzeVO>
     */
    @Override
    public List<SpaceRankAnalyzeVO> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, LoginUserVO loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);

        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "space_name AS spaceName", "user_id AS userId", "total_size AS totalSize")
                .orderByDesc("total_size")
                .last("LIMIT " + spaceRankAnalyzeDTO.getTopN()); // 取前 N 名

        // 查询结果并转换为 VO，避免返回多余字段
        List<Space> spaceList = this.list(queryWrapper);
        return spaceList.stream()
                .map(space -> {
                    SpaceRankAnalyzeVO vo = new SpaceRankAnalyzeVO();
                    BeanUtil.copyProperties(space, vo);
                    return vo;
                })
                .collect(Collectors.toList());
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
