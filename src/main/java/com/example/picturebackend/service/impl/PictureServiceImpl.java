package com.example.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.manage.FileManage;
import com.example.picturebackend.model.dto.file.UploadPictureResult;
import com.example.picturebackend.model.dto.picture.PictureQueryDTO;
import com.example.picturebackend.model.dto.picture.PictureUploadDTO;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureVO;
import com.example.picturebackend.service.UserService;
import org.springframework.stereotype.Service;
import com.example.picturebackend.mapper.PictureMapper;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.service.PictureService;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManage fileManage;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     *
     * @param multipartFile    图片文件
     * @param pictureUploadDTO 图片上传信息
     * @param loginUser        登录用户
     *
     * @return PictureVO
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadDTO pictureUploadDTO, LoginUserVO loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 1. 判断新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadDTO != null) {
            pictureId = pictureUploadDTO.getId();
        }

        // 如果是更新图片，则需要判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 2. 上传图片到云存储
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManage.uploadPicture(multipartFile, uploadPathPrefix);

        // 3. 保存图片信息到数据库
        Picture picture = createPicture(loginUser, uploadPictureResult, pictureId);
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

    private static Picture createPicture(LoginUserVO loginUser, UploadPictureResult uploadPictureResult, Long pictureId) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getName());
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

