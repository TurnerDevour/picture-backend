package com.example.picturebackend.manage;

import com.example.picturebackend.config.CosClientConfig;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

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
        putObjectRequest.setPicOperations(picOperations);
        return cosClientConfig.cosClient().putObject(putObjectRequest);
    }
}
