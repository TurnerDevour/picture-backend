package com.example.picturebackend.manage.upload;

import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        try {
            multipartFile.transferTo(file);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void checkPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;

        // 1. 校验文件不能为空
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 2. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long MAX_SIZE = 2 * 1024 * 1024L; // 限制文件大小为 2MB
        ThrowUtils.throwIf(fileSize > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");

        // 3. 校验文件类型
        final List<String> ALLOWED_TYPES = Arrays.asList("jpg", "jpeg", "png", "webp");
        String originalFilename = multipartFile.getOriginalFilename();
        String fileType = null;
        if (originalFilename != null) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        ThrowUtils.throwIf(!ALLOWED_TYPES.contains(fileType), ErrorCode.PARAMS_ERROR, "文件类型不支持");
    }
}
