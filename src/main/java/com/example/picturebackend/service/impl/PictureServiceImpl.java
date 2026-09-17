package com.example.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import com.example.picturebackend.manage.CosManage;
import com.example.picturebackend.manage.upload.FilePictureUpload;
import com.example.picturebackend.manage.upload.PictureUploadTemplate;
import com.example.picturebackend.manage.upload.UrlPictureUpload;
import com.example.picturebackend.mapper.PictureMapper;
import com.example.picturebackend.model.dto.file.UploadPictureResult;
import com.example.picturebackend.model.dto.picture.*;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.Space;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.PictureReviewStatusEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureVO;
import com.example.picturebackend.service.PictureService;
import com.example.picturebackend.service.SpaceService;
import com.example.picturebackend.service.UserService;
import com.example.picturebackend.utils.ColorSimilarUtils;
import com.qcloud.cos.exception.CosClientException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManage cosManage;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 上传图片
     *
     * @param inputSource      图片文件或图片URL
     * @param pictureUploadDTO 图片上传信息
     * @param loginUser        登录用户
     *
     * @return PictureVO
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, LoginUserVO loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 校验空间是否存在
        Long spaceId = pictureUploadDTO.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 仅本人可上传图片到该空间
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限上传图片到该空间");
            }

            // 校验空间条数
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数已满，无法上传图片");
            }

            // 校验空间大小
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小已满，无法上传图片");
            }
        }

        // 1. 判断新增图片还是更新图片
        Long pictureId = pictureUploadDTO.getId();
        // 更新前的旧图片信息（用于更新后清理旧文件）
        Picture oldPicture = null;

        // 如果是更新图片，则需要判断图片是否存在
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 校验空间是否一致
            // 没传spaceId，则复用原有的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了spaceId，则校验是否一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不一致，无法更新图片");
                }
            }
        }

        // 2. 上传图片到云存储
        String uploadPathPrefix = null;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        Picture picture = createPicture(loginUser, uploadPictureResult, pictureId, spaceId, pictureUploadDTO);

        // 补充审核信息
        fillReviewInfo(picture, loginUser);

        // 3. 保存图片信息到数据库
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "保存图片信息失败");
            if (finalSpaceId != null) {
                // 更新空间的总数和总大小
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("total_size = total_size + " + picture.getPicSize())
                        .setSql("total_count = total_count + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新空间信息失败");
            }

            return picture;
        });


        // 4. 如果是更新图片，则清理旧图片文件
        if (oldPicture != null) {
            clearPictureFile(oldPicture);
        }

        return PictureVO.convertObjectToVO(picture);
    }

    /**
     * 获取查询条件
     *
     * @param pictureQueryDTO 查询条件
     *
     * @return QueryWrapper<Picture>
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryDTO == null) {
            return queryWrapper;
        }

        // 取出查询条件中的值
        Long id = pictureQueryDTO.getId();
        String name = pictureQueryDTO.getName();
        String introduction = pictureQueryDTO.getIntroduction();
        String category = pictureQueryDTO.getCategory();
        List<String> tags = pictureQueryDTO.getTags();
        Long picSize = pictureQueryDTO.getPicSize();
        Integer picWidth = pictureQueryDTO.getPicWidth();
        Integer picHeight = pictureQueryDTO.getPicHeight();
        Double picScale = pictureQueryDTO.getPicScale();
        String picFormat = pictureQueryDTO.getPicFormat();
        Long userId = pictureQueryDTO.getUserId();
        Long reviewerId = pictureQueryDTO.getReviewerId();
        String reviewMessage = pictureQueryDTO.getReviewMessage();
        Integer reviewStatus = pictureQueryDTO.getReviewStatus();
        String searchText = pictureQueryDTO.getSearchText();
        Long spaceId = pictureQueryDTO.getSpaceId();
        boolean nullSpaceId = pictureQueryDTO.isNullSpaceId();
        LocalDateTime startEditTime = pictureQueryDTO.getStartEditTime();
        LocalDateTime endEditTime = pictureQueryDTO.getEndEditTime();
        String sortField = pictureQueryDTO.getSortField();
        String sortOrder = pictureQueryDTO.getSortOrder();

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("name", searchText))
                    .or().like("introduction", searchText);
        }

        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), "user_id", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "pic_format", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotNull(picSize), "pic_size", picSize);
        queryWrapper.eq(ObjectUtil.isNotNull(picWidth), "pic_width", picWidth);
        queryWrapper.eq(ObjectUtil.isNotNull(picHeight), "pic_height", picHeight);
        queryWrapper.eq(ObjectUtil.isNotNull(picScale), "pic_scale", picScale);
        queryWrapper.eq(ObjectUtil.isNotNull(reviewerId), "reviewer_id", reviewerId);
        queryWrapper.eq(ObjectUtil.isNotNull(reviewStatus), "review_status", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "review_message", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "space_id", spaceId);
        queryWrapper.isNull(nullSpaceId, "space_id");
        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime), "edit_time", startEditTime);
        queryWrapper.le(ObjectUtil.isNotEmpty(endEditTime), "edit_time", endEditTime);

        // 处理标签JSON数组查询条件
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // 使用 like 查询 JSON 数组中的标签，确保标签被双引号包裹
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 处理排序条件
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取单个图片VO
     *
     * @param picture 图片实体
     * @param request 请求对象
     *
     * @return PictureVO
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.convertObjectToVO(picture);

        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }

        return pictureVO;
    }

    /**
     * 获取图片VO分页
     *
     * @param picturePage 图片分页
     * @param request     请求对象
     *
     * @return Page<PictureVO>
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::convertObjectToVO).collect(Collectors.toList());

        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));

        // 2. 填充用户信息到 PictureVO
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片信息
     *
     * @param picture 图片实体
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片信息为空");

        Long pictureId = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数就要校验
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureId), ErrorCode.PARAMS_ERROR, "图片id不能为空");
        // url 为空时不校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 512, ErrorCode.PARAMS_ERROR, "图片url过长");
        }
        // introduction 为空时不校验
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 512, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }
    }

    /**
     * 图片审核
     *
     * @param pictureReviewDTO 图片审核信息
     * @param loginUser        登录用户
     */
    @Override
    public void pictureReview(PictureReviewDTO pictureReviewDTO, LoginUserVO loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewDTO == null, ErrorCode.PARAMS_ERROR, "图片审核信息为空");
        Long id = pictureReviewDTO.getId();
        Integer reviewStatus = pictureReviewDTO.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);// 校验审核状态是否合法
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 3. 校验图片是否重复，如果图片已经是该状态，则无需审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 4. 操作数据库
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewDTO, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(LocalDateTime.now());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "图片审核失败");
    }

    /**
     * 填充图片审核信息
     *
     * @param picture   图片实体
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewInfo(Picture picture, LoginUserVO loginUser) {
        // 如果是管理员，则自动填充审核信息直接过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动审核通过");
            picture.setReviewTime(LocalDateTime.now());
        } else {
            // 如果是普通用户，创建图片和更新图片时，审核状态为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchDTO 批量上传请求
     * @param loginUser               登录用户
     *
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, LoginUserVO loginUser) {
        // 1. 校验参数
        String searchText = pictureUploadByBatchDTO.getSearchText();
        Integer count = pictureUploadByBatchDTO.getCount();
        String namePrefix = pictureUploadByBatchDTO.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        ThrowUtils.throwIf(StrUtil.isBlank(searchText) || count == null || count <= 0 || count >= 30, ErrorCode.PARAMS_ERROR, "参数错误");

        // 2. 抓取地址
        String fetchUrl = String.format("https://www.bing.com/images/async?q=%s&mmasync=1", searchText);

        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        // 3. 解析图片的HTML，获取图片的URL
        Element dgControlDIV = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(dgControlDIV)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = dgControlDIV.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前图片URL为空，跳过：{}", fileUrl);
                continue;
            }

            // 4. 处理图片URL中出现的转义字符
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 5. 上传图片
            try {
                PictureUploadDTO pictureUploadDTO = new PictureUploadDTO();

                if (StrUtil.isNotBlank(namePrefix)) {
                    // 如果 namePrefix 不为空，则使用 namePrefix + "_" + (uploadCount + 1) 作为图片名称
                    pictureUploadDTO.setPicName(namePrefix + "_" + (uploadCount + 1));
                }

                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadDTO, loginUser);
                log.info("成功上传图片：{}", pictureVO.getUrl());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传图片失败：{}", fileUrl, e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    /**
     * 清理COS图片
     *
     * @param oldPicture 旧图片实体
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 1. 判断图片URL是否仍被其他图片引用
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        // 该URL仍被其他图片引用，则不能删除对应的文件
        if (count != null && count > 0) {
            log.info("图片被其他用户使用，无法删除：{}", oldPicture.getUrl());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片被其他用户使用，无法删除");
        }

        // 2. 从pictureUrl中获取图片的key
        String key = pictureUrl.substring(pictureUrl.indexOf(".com/"));

        // 3. 删除图片
        try {
            cosManage.deleteObject(key);

            if (StrUtil.isNotEmpty(oldPicture.getThumbnailUrl())) {
                String thumbnailKey = oldPicture.getThumbnailUrl().substring(oldPicture.getThumbnailUrl().indexOf(".com/"));
                cosManage.deleteObject(thumbnailKey);
            }
        } catch (CosClientException e) {
            log.error("删除图片失败：{}", oldPicture.getUrl(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除图片失败");
        }
    }

    /**
     * 创建图片实体
     *
     * @param loginUser           登录用户
     * @param uploadPictureResult 上传结果
     * @param pictureId           图片ID
     * @param pictureUploadDTO    图片上传信息
     *
     * @return Picture
     */
    private static Picture createPicture(LoginUserVO loginUser, UploadPictureResult uploadPictureResult, Long pictureId, Long spaceId, PictureUploadDTO pictureUploadDTO) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getName();
        if (pictureUploadDTO != null && StrUtil.isNotBlank(pictureUploadDTO.getPicName())) {
            picName = pictureUploadDTO.getPicName();
        }
        picture.setName(picName);
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());

        // 4.如果 pictureId 不为空，则更新图片信息，否则新增图片信息
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(LocalDateTime.now());
        }
        return picture;
    }

    /**
     * 校验图片权限
     *
     * @param picture   图片实体
     * @param loginUser 登录用户
     */
    @Override
    public void checkPictureAuth(Picture picture, LoginUserVO loginUser) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 如果spaceId为空，则为公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 如果spaceId不为空，则为私人图库，仅空间创建者可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 删除图片
     *
     * @param pictureId 图片ID
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(long pictureId, LoginUserVO loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(oldPicture, loginUser);
        // 操作数据库
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放空间的总数和总大小
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("total_size = total_size - " + oldPicture.getPicSize())
                        .setSql("total_count = total_count - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "更新空间信息失败");
            }

            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片信息
     *
     * @param pictureEditDTO 图片编辑信息
     * @param loginUser      登录用户
     */
    @Override
    public void editPicture(PictureEditDTO pictureEditDTO, LoginUserVO loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditDTO, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditDTO.getTags()));
        // 设置编辑时间
        picture.setEditTime(LocalDateTime.now());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditDTO.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(oldPicture, loginUser);
        // 补充审核参数
        this.fillReviewInfo(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId   空间ID
     * @param picColor  图片颜色
     * @param loginUser 登录用户
     *
     * @return List<PictureVO>
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, LoginUserVO loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "请先登录");
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
        }
        // 3. 查询该空间下的所有图片，筛选出颜色中必须包含主色调
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 4. 如果没有图片，则返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 5. 将目标颜色转为Color对象
        Color targetColor = Color.decode(picColor);
        // 6. 计算相似度并排序，取前12个
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12)
                .collect(Collectors.toList());
        // 7. 将排序后的图片列表转换为PictureVO列表并返回
        return sortedPictureList.stream().map(PictureVO::convertObjectToVO).collect(Collectors.toList());
    }

    /**
     * 批量编辑图片信息
     *
     * @param pictureEditByBatchDTO 批量编辑图片信息 DTO
     * @param loginUser             登录用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchDTO pictureEditByBatchDTO, LoginUserVO loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureEditByBatchDTO == null, ErrorCode.PARAMS_ERROR, "参数错误");
        List<Long> pictureIdList = pictureEditByBatchDTO.getPictureIdList();
        Long spaceId = pictureEditByBatchDTO.getSpaceId();
        String category = pictureEditByBatchDTO.getCategory();
        List<String> tags = pictureEditByBatchDTO.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList) || spaceId == null, ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "请先登录");
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
        }
        // 3. 查询指定图片，仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();

        if (pictureList.isEmpty()) {
            return;
        }

        // 4. 更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });

        // 批量重命名
        String nameRule = pictureEditByBatchDTO.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);


        // 5. 批量更新图片信息
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量更新图片信息失败");
    }

    /**
     * 根据命名规则批量重命名图片
     *
     * @param pictureList 图片列表
     * @param nameRule    命名规则
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }

        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String newName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(newName);
            }
        } catch (Exception e) {
            log.error("批量重命名图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量重命名图片失败");
        }
    }

}

