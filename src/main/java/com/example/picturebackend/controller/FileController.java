package com.example.picturebackend.controller;

import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.common.BaseResponse;
import com.example.picturebackend.common.ResultUtils;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.manage.CosManage;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Resource
    private CosManage cosManage;

    @PostMapping("/test/upload")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<String> testUpload(@RequestPart("file") MultipartFile multipartFile) {
        // 1. 文件名/文件路径
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", filename);
        File dest = null;
        try {
            // 2. 创建临时文件
            dest = File.createTempFile(filePath, null);
            // 3. 将文件写入临时文件
            multipartFile.transferTo(dest);
            PutObjectResult result = cosManage.putObjectFile(filePath, dest);
            log.info("文件上传成功，ETag: {}", result.getETag());
            // 4. 返回文件路径
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (dest != null && dest.exists()) {
                // 删除临时文件
                boolean deleted = dest.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", dest.getAbsolutePath());
                }
            }
        }
    }

    @PostMapping("/test/download")
    @AuthCheck(mustRole = "admin")
    public void testDownload(String filePath, HttpServletResponse response) {
        try (
                COSObject cosObject = cosManage.getObject(filePath);
                COSObjectInputStream cosObjectInputStream = cosObject.getObjectContent();
                ServletOutputStream outputStream = response.getOutputStream()
        ) {
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);
            // 将文件内容写入响应输出流
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cosObjectInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件下载失败, filePath: {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        }
    }
}
