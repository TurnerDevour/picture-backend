package com.example.picturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
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
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.enums.PictureReviewStatusEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureTagCategoryVO;
import com.example.picturebackend.model.vo.PictureVO;
import com.example.picturebackend.service.PictureService;
import com.example.picturebackend.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES) // 缓存 5 分钟移除
                    .build();


    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadDTO pictureUploadDTO, HttpServletRequest request) {
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadDTO, loginUser);

        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadDTO pictureUploadDTO, HttpServletRequest request) {
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUploadDTO.getUrl(), pictureUploadDTO, loginUser);

        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        Long id = deleteRequest.getId();

        // 判断是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 清理cos图片
        pictureService.deletePicture(oldPicture);

        return ResultUtils.success(true);
    }

    /**
     * 此方法只有管理员可用
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDTO pictureUpdateDTO, HttpServletRequest request) {
        if (pictureUpdateDTO == null || pictureUpdateDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateDTO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateDTO.getTags()));
        pictureService.validPicture(picture);
        Long id = pictureUpdateDTO.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 补充审核信息
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        pictureService.fillReviewInfo(picture, loginUser);

        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 此方法只有管理员可用
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Picture result = pictureService.getById(id);
        ThrowUtils.throwIf(result == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 此方法只有管理员可用
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryDTO pictureQueryDTO) {
        long current = pictureQueryDTO.getCurrent();
        long size = pictureQueryDTO.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryDTO));
        return ResultUtils.success(picturePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryDTO pictureQueryDTO, HttpServletRequest request) {
        long current = pictureQueryDTO.getCurrent();
        long size = pictureQueryDTO.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页数量不能超过20");
        pictureQueryDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryDTO));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 使用 Redis 缓存分页结果，减少数据库压力
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCache(@RequestBody PictureQueryDTO pictureQueryDTO, HttpServletRequest request) {
        long current = pictureQueryDTO.getCurrent();
        long size = pictureQueryDTO.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页数量不能超过20");
        pictureQueryDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //使用 Redis 来存储分页结果
        // 1. 生成缓存键
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
        String hashKey = DigestUtil.md5Hex(queryCondition.getBytes());
        String cacheKey = "picture:listPictureVOByPage:" + hashKey;

        // 2. 从缓存中获取数据
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String cachedValue = operations.get(cacheKey);
        if (cachedValue != null) {
            // 3. 如果缓存中有数据，直接返回
            Type pageType = new TypeReference<Page<PictureVO>>() {
            }.getType();
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, pageType, false);
            return ResultUtils.success(cachedPage);
        }
        // 4. 如果缓存中没有数据，查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryDTO));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 5. 将查询结果存入缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 6. 设置缓存过期时间为 5-10 分钟, 随机 0-300 秒，防止缓存雪崩
        int cacheExpireTime = (5 * 60) + RandomUtil.randomInt(0, 300);
        operations.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 使用本地缓存（Caffeine）来存储分页结果
     */
    @PostMapping("/list/page/vo/caffeine")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCaffeine(@RequestBody PictureQueryDTO pictureQueryDTO, HttpServletRequest request) {
        long current = pictureQueryDTO.getCurrent();
        long size = pictureQueryDTO.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页数量不能超过20");
        pictureQueryDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //使用 本地缓存 来存储分页结果
        // 1. 生成缓存键
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
        String hashKey = DigestUtil.md5Hex(queryCondition.getBytes());
        String cacheKey = "picture:listPictureVOByPage:" + hashKey;

        // 2. 从本地缓存中获取数据
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 3. 如果缓存中有数据，直接返回
            Type pageType = new TypeReference<Page<PictureVO>>() {
            }.getType();
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, pageType, false);
            return ResultUtils.success(cachedPage);
        }
        // 4. 如果缓存中没有数据，查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryDTO));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 5. 将查询结果存入缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(cacheKey, cacheValue);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 先使用本地缓存，如果本地缓存没有，再使用 Redis 缓存（多级缓存策略）
     */
    @PostMapping("/list/page/vo/cache/dual")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCacheDual(@RequestBody PictureQueryDTO pictureQueryDTO, HttpServletRequest request) {
        long current = pictureQueryDTO.getCurrent();
        long size = pictureQueryDTO.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页数量不能超过20");
        pictureQueryDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 1. 生成缓存键
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
        String hashKey = DigestUtil.md5Hex(queryCondition.getBytes());
        String cacheKey = "picture:listPictureVOByPage:" + hashKey;

        // 2. 从本地缓存中获取数据
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存中有数据，直接返回
            Type pageType = new TypeReference<Page<PictureVO>>() {
            }.getType();
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, pageType, false);
            return ResultUtils.success(cachedPage);
        }

        //3.如果在本地缓存中没有数据，从 Redis 缓存中获取数据
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        cachedValue = operations.get(cacheKey);
        if (cachedValue != null) {
            // 如果缓存中有数据，直接返回
            Type pageType = new TypeReference<Page<PictureVO>>() {
            }.getType();
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, pageType, false);
            return ResultUtils.success(cachedPage);
        }

        // 4. 如果缓存中没有数据，查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryDTO));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        // 5. 将查询结果存入缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);

        // 6. 将查询结果存入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);

        // 7. 设置缓存过期时间为 5-10 分钟, 随机 0-300 秒，防止缓存雪崩
        int cacheExpireTime = (5 * 60) + RandomUtil.randomInt(0, 300);
        operations.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return ResultUtils.success(pictureVOPage);
    }


    /**
     * 此方法只有普通用户可用
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDTO pictureEditDTO, HttpServletRequest request) {
        if (pictureEditDTO == null || pictureEditDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        Long id = pictureEditDTO.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人可编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditDTO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditDTO.getTags()));
        picture.setEditTime(LocalDateTime.now());
        pictureService.validPicture(picture);

        // 补充审核信息
        pictureService.fillReviewInfo(picture, loginUser);

        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategoryVO> listPictureTagCategory() {
        PictureTagCategoryVO pictureTagCategory = new PictureTagCategoryVO();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewDTO pictureReviewDTO, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewDTO == null || pictureReviewDTO.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取登录用户
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        // 3. 调用 service 进行审核
        pictureService.pictureReview(pictureReviewDTO, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchDTO pictureUploadByBatchDTO, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureUploadByBatchDTO == null, ErrorCode.PARAMS_ERROR);
        // 2. 获取登录用户
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);
        // 3. 调用 service 进行批量上传
        int successCount = pictureService.uploadPictureByBatch(pictureUploadByBatchDTO, loginUser);
        return ResultUtils.success(successCount);
    }
}
