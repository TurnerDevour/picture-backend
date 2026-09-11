package com.example.picturebackend.manage;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.picturebackend.config.CosClientConfig;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Slf4j
@Component
public class FileManage {

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
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String prefix) {
        // 1. 校验图片
        checkPicture(multipartFile);

        // 2. 上传图片地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", prefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时图片
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 4. 上传图片到 COS
            PutObjectResult pictureObject = cosManage.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = pictureObject.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 5. 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);

            return uploadPictureResult;

        } catch (Exception e) {
            log.error("上传图片失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            // 3. 删除临时图片
            if (file != null) {
                deleteTempFile(file);
            }
        }
    }


    /**
     * 校验图片
     *
     * @param multipartFile 文件
     */
    public void checkPicture(MultipartFile multipartFile) {
        // 1. 校验文件不能为空
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 2. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long MAX_SIZE = 1024 * 1024L; // 1MB
        ThrowUtils.throwIf(fileSize * 2 > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");

        // 3. 校验文件类型
        final List<String> ALLOWED_TYPES = Arrays.asList("jpg", "jpeg", "png", "webp");
        String originalFilename = multipartFile.getOriginalFilename();
        String fileType = null;
        if (originalFilename != null) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        ThrowUtils.throwIf(!ALLOWED_TYPES.contains(fileType), ErrorCode.PARAMS_ERROR, "文件类型不支持");
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

}
