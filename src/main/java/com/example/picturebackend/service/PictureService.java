package com.example.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.model.dto.picture.PictureQueryDTO;
import com.example.picturebackend.model.dto.picture.PictureUploadDTO;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile    图片文件
     * @param pictureUploadDTO 图片上传信息
     * @param loginUser        登录用户
     *
     * @return PictureVO
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadDTO pictureUploadDTO, LoginUserVO loginUser);

    /**
     * 获取查询条件
     *
     * @param pictureQueryDTO 查询条件
     *
     * @return QueryWrapper<Picture>
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO);

    /**
     * 获取单个图片VO
     *
     * @param picture 图片实体
     * @param request 请求对象
     *
     * @return PictureVO
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片VO分页
     *
     * @param picturePage 图片分页
     * @param request     请求对象
     *
     * @return Page<PictureVO>
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片信息
     *
     * @param picture 图片实体
     */
    void validPicture(Picture picture);
}

