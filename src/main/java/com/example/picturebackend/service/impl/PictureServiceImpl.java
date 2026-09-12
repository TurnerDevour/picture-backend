package com.example.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.manage.upload.FilePictureUpload;
import com.example.picturebackend.manage.upload.PictureUploadTemplate;
import com.example.picturebackend.manage.upload.UrlPictureUpload;
import com.example.picturebackend.mapper.PictureMapper;
import com.example.picturebackend.model.dto.file.UploadPictureResult;
import com.example.picturebackend.model.dto.picture.PictureQueryDTO;
import com.example.picturebackend.model.dto.picture.PictureReviewDTO;
import com.example.picturebackend.model.dto.picture.PictureUploadByBatchDTO;
import com.example.picturebackend.model.dto.picture.PictureUploadDTO;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.enums.PictureReviewStatusEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureVO;
import com.example.picturebackend.service.PictureService;
import com.example.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        // 1. 判断新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadDTO != null) {
            pictureId = pictureUploadDTO.getId();
        }

        // 如果是更新图片，则需要判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可更新
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 2. 上传图片到云存储
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        Picture picture = createPicture(loginUser, uploadPictureResult, pictureId, pictureUploadDTO);

        // 补充审核信息
        fillReviewInfo(picture, loginUser);

        // 3. 保存图片信息到数据库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "保存图片信息失败");
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

    private static Picture createPicture(LoginUserVO loginUser, UploadPictureResult uploadPictureResult, Long pictureId, PictureUploadDTO pictureUploadDTO) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getName();
        if (pictureUploadDTO != null && StrUtil.isNotBlank(pictureUploadDTO.getPicName())) {
            picName = pictureUploadDTO.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        // 4.如果 pictureId 不为空，则更新图片信息，否则新增图片信息
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(LocalDateTime.now());
        }
        return picture;
    }
}

