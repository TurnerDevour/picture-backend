package com.example.picturebackend.manage.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) {
        String url = (String) inputSource;
        try {
            HttpUtil.downloadFile(url, file);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片下载失败");
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 返回带后缀的文件名，便于父类模板方法正确提取图片格式后缀
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected void checkPicture(Object inputSource) {
        String url = (String) inputSource;
        // 1. 检验 URL 不能为空
        ThrowUtils.throwIf(url == null || url.isEmpty(), ErrorCode.PARAMS_ERROR, "图片 URL 不能为空");

        // 2. 校验 URL 是否合法，仅支持 http 和 https 协议
        try {
            URL urlObj = new URL(url);
            String protocol = urlObj.getProtocol();
            ThrowUtils.throwIf(!protocol.equals("http") && !protocol.equals("https"), ErrorCode.PARAMS_ERROR, "图片 URL 协议不合法，仅支持 http 和 https");
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 URL 不合法");
        }

        // 3. 发送 HEAD 请求，检查图片是否存在
        try (HttpResponse response = HttpUtil.createRequest(Method.HEAD, url).execute()) {
            // 4. 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 5. 校验 Content-Type 是否为图片类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 定义允许的图片类型
                List<String> allowedContentTypes = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
                ThrowUtils.throwIf(!allowedContentTypes.contains(contentType), ErrorCode.PARAMS_ERROR, "图片 URL Content-Type 不合法，仅支持 jpg、jpeg、png、webp");
            }

            // 6. 校验 Content-Length 是否超过 2MB
            String contentLength = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                long length = Long.parseLong(contentLength);
                final long MAX_SIZE = 2 * 1024 * 1024L; // 限制图片大小为 2MB
                ThrowUtils.throwIf(length > MAX_SIZE, ErrorCode.PARAMS_ERROR, "图片 URL Content-Length 超过 2MB");
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 URL 不合法");
        }
    }
}
