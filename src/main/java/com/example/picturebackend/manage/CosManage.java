package com.example.picturebackend.manage;

import cn.hutool.core.io.FileUtil;
import com.example.picturebackend.config.CosClientConfig;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManage {

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传文件到腾讯云 COS
     *
     * @param key  文件在存储桶中的路径。
     * @param file 要上传的文件。
     *
     * @return 包含已上传对象信息的 PutObjectResult。
     */
    public PutObjectResult putObjectFile(String key, File file) {
        PutObjectRequest objectRequest = new PutObjectRequest(cosClientConfig.getBucketName(), key, file);
        return cosClientConfig.cosClient().putObject(objectRequest);
    }

    /**
     * 从腾讯云 COS 获取文件对象。
     *
     * @param key 文件在存储桶中的路径。
     *
     * @return 包含已获取对象信息的 COSObject。
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucketName(), key);
        return cosClientConfig.cosClient().getObject(getObjectRequest);
    }

    /**
     * 上传图片文件到腾讯云 COS，并获取图片的基本信息。
     *
     * @param key  图片在存储桶中的路径。
     * @param file 要上传的图片文件。
     *
     * @return 包含已上传对象信息的 PutObjectResult。
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucketName(), key, file);

        //获取图片的基本信息
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> ruleList = new ArrayList<>();
        // 设置图片转换为 WebP 格式的处理规则
        String webKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webKey);
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucketName());
        ruleList.add(compressRule);

        // 当图片大于 20KB 时，设置缩略图处理规则
        if (file.length() > 20 * 1024) {
            // 设置图片压缩处理规则
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            thumbnailRule.setBucket(cosClientConfig.getBucketName());
            ruleList.add(thumbnailRule);
        }

        picOperations.setRules(ruleList);
        putObjectRequest.setPicOperations(picOperations);
        return cosClientConfig.cosClient().putObject(putObjectRequest);
    }
}
