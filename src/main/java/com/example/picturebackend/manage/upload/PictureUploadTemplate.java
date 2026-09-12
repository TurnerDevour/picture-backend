package com.example.picturebackend.manage.upload;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.picturebackend.config.CosClientConfig;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.manage.CosManage;
import com.example.picturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManage cosManage;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     *
     * @param multipartFile 文件
     * @param prefix        前缀
     *
     * @return 上传结果
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String prefix) {
        // 1. 校验图片
        checkPicture(inputSource);

        // 2. 上传图片地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", prefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时图片
            file = File.createTempFile(uploadPath, null);

            // 4. 处理图片
            processFile(inputSource, file);

            // 5. 上传图片到 COS
            PutObjectResult pictureObject = cosManage.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = pictureObject.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 6. 封装返回结果
            return buildResult(imageInfo, file, originalFilename, uploadPath);
        } catch (Exception e) {
            log.error("uploadPicture方法 - 上传图片到 COS 失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片到 COS 失败");
        } finally {
            // 7. 删除临时图片
            if (file != null) {
                deleteTempFile(file);
            }
        }
    }

    /**
     * 构建上传结果
     *
     * @param imageInfo        图片信息
     * @param file             文件
     * @param originalFilename 原始文件名
     * @param uploadPath       上传路径
     *
     * @return 上传结果
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, File file, String originalFilename, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setName(originalFilename);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 删除临时图片
     *
     * @param file 文件
     */
    public void deleteTempFile(File file) {
        // 1. 校验文件不能为空
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 2. 删除临时图片
        boolean deleted = file.delete();
        if (!deleted) {
            log.error("删除临时图片失败: {}", file.getAbsolutePath());
        }
    }

    /**
     * 处理图片
     *
     * @param inputSource 输入源
     * @param file        文件
     */
    protected abstract void processFile(Object inputSource, File file);

    /**
     * 获取原始文件名
     *
     * @param inputSource 输入源
     *
     * @return 原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验图片
     *
     * @param inputSource 输入源
     */
    protected abstract void checkPicture(Object inputSource);
}
