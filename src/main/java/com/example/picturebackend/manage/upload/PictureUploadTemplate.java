package com.example.picturebackend.manage.upload;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.picturebackend.config.CosClientConfig;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import com.example.picturebackend.manage.CosManage;
import com.example.picturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManage cosManage;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 允许的图片后缀
     */
    private static final List<String> ALLOWED_IMAGE_SUFFIX = Arrays.asList("jpg", "jpeg", "png", "webp");

    /**
     * 上传图片
     *
     * @param inputSource 文件
     * @param prefix      前缀
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

            // 4.1 修正图片后缀：当原始文件名缺少合法图片后缀时（如 Bing 批量抓图地址），
            // 根据文件实际内容检测格式，保证上传后的图片地址带有正确后缀
            String validSuffix = getValidSuffix(originalFilename, file);
            if (!StrUtil.equalsIgnoreCase(FileUtil.getSuffix(originalFilename), validSuffix)) {
                uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, validSuffix);
                uploadPath = String.format("/%s/%s", prefix, uploadFilename);
            }

            // 5. 上传图片到 COS
            PutObjectResult pictureObject = cosManage.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = pictureObject.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = pictureObject.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                // 如果有缩略图，则使用缩略图作为返回结果，否则使用压缩后的图片
                CIObject thumbnailObject = compressedCiObject;
                if (objectList.size() > 1) {
                    thumbnailObject = objectList.get(1);
                }
                return buildResult(originalFilename, compressedCiObject, thumbnailObject, imageInfo);
            }

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
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**
     * 构建上传结果
     *
     * @param originFilename     原始文件名
     * @param compressedCiObject 压缩后的 CIObject
     *
     * @return 上传结果
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailObject, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailObject.getKey());
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
     * 获取有效的图片后缀
     * <p>
     * 当原始文件名中的后缀不是合法图片后缀时（例如通过 URL 批量抓图时，Bing 图片地址通常不带 .jpg/.png 后缀），
     * 根据已下载的临时文件内容检测实际图片格式，确保上传后的图片地址带有正确后缀。
     *
     * @param originalFilename 原始文件名
     * @param file             已下载的临时文件
     *
     * @return 有效的图片后缀
     */
    private String getValidSuffix(String originalFilename, File file) {
        String suffix = FileUtil.getSuffix(originalFilename);
        if (StrUtil.isNotBlank(suffix) && ALLOWED_IMAGE_SUFFIX.contains(suffix.toLowerCase())) {
            return suffix.toLowerCase();
        }
        return detectImageFormat(file);
    }

    /**
     * 根据文件内容（魔数）检测图片格式
     *
     * @param file 图片文件
     *
     * @return 图片后缀，无法识别时兜底返回 jpg
     */
    private String detectImageFormat(File file) {
        String type = FileTypeUtil.getType(file);
        if (StrUtil.isNotBlank(type) && ALLOWED_IMAGE_SUFFIX.contains(type.toLowerCase())) {
            return type.toLowerCase();
        }
        // WebP 以 RIFF....WEBP 标识，部分工具无法识别，这里手动兜底
        byte[] header = new byte[12];
        try (FileInputStream in = new FileInputStream(file)) {
            int len = in.read(header);
            if (len >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "webp";
            }
        } catch (IOException ignored) {
            // 忽略读取异常，走兜底逻辑
        }
        return "jpg";
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
