package com.example.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.model.dto.picture.PictureQueryDTO;
import com.example.picturebackend.model.dto.picture.PictureReviewDTO;
import com.example.picturebackend.model.dto.picture.PictureUploadByBatchDTO;
import com.example.picturebackend.model.dto.picture.PictureUploadDTO;
import com.example.picturebackend.model.entity.Picture;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource      图片文件
     * @param pictureUploadDTO 图片上传信息
     * @param loginUser        登录用户
     *
     * @return PictureVO
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, LoginUserVO loginUser);

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

    /**
     * 图片审核
     *
     * @param pictureReviewDTO 图片审核信息
     * @param loginUser        登录用户
     */
    void pictureReview(PictureReviewDTO pictureReviewDTO, LoginUserVO loginUser);

    /**
     * 填充图片审核信息
     *
     * @param picture   图片实体
     * @param loginUser 登录用户
     */
    void fillReviewInfo(Picture picture, LoginUserVO loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchDTO 批量上传请求
     * @param loginUser               登录用户
     *
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, LoginUserVO loginUser);
}

